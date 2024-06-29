package com.github.hubvd.odootools.actions.git

import java.lang.foreign.*
import java.lang.foreign.MemoryLayout.PathElement
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class LibGitInvocationHandler(
    private val lookup: SymbolLookup,
    private val linker: Linker,
) : InvocationHandler {

    private val methodHandleByMethodName = ConcurrentHashMap<String, MethodHandle>()

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val checkReturnValue = method.canThrow()

        val handle = methodHandleByMethodName.computeIfAbsent(method.name) {
            val segment = lookup.find("git_$it").get()
            val descriptor = method.descriptor()
            linker.downcallHandle(segment, descriptor)
        }
        val result = handle.invokeWithArguments(args?.toList() ?: emptyList<Any>())

        if (checkReturnValue && (result as Int) != 0) {
            val lastError = lookup.find("git_error_last").get()
            val descriptor = FunctionDescriptor.of(ADDRESS)
            val error = (
                linker.downcallHandle(lastError, descriptor)
                    .invoke() as MemorySegment
                ).reinterpret(git_error.layout.byteSize())

            val message = (
                git_error.layout.varHandle(PathElement.groupElement("message"))
                    .get(error, 0) as MemorySegment
                ).reinterpret(Long.MAX_VALUE)
            val klazz = git_error.layout.varHandle(PathElement.groupElement("klass")).get(error, 0) as Int
            throw LibGitError(
                git_error_code.entries.first { it.value == result }, // TODO: should index ?
                git_error_t.entries[klazz],
                message.getString(0),
            )
        }

        if (method.returnType == Void.TYPE) {
            return null
        }

        return result
    }

    companion object {
        internal fun Method.descriptor(): FunctionDescriptor? {
            val parameters = parameters.map { typeToLayout(it.type) }.toTypedArray()
            return if (returnType == Void.TYPE) {
                if (canThrow()) {
                    FunctionDescriptor.of(JAVA_INT, *parameters)
                } else {
                    FunctionDescriptor.ofVoid(*parameters)
                }
            } else {
                FunctionDescriptor.of(typeToLayout(returnType), *parameters)
            }
        }

        private fun Method.canThrow(): Boolean = returnType == Void.TYPE && !name.endsWith("_free")

        private fun typeToLayout(type: Class<*>): MemoryLayout = when (type) {
            Int::class.java -> JAVA_INT
            Long::class.java -> JAVA_LONG
            Boolean::class.java -> JAVA_BOOLEAN
            MemorySegment::class.java, String::class.java -> ADDRESS
            else -> TODO(type.toString())
        }
    }
}
