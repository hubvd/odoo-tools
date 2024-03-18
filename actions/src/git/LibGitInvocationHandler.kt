package com.github.hubvd.odootools.actions.git

import java.lang.foreign.*
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
        val checkReturnValue =
            method.returnType == Int::class.java && method.annotations.filterIsInstance<LibGitNoError>().isEmpty()

        val handle = methodHandleByMethodName.computeIfAbsent(method.name) {
            val segment = lookup.find("git_$it").get()
            val descriptor = method.descriptor()
            linker.downcallHandle(segment, descriptor)
        }
        val result = handle.invokeWithArguments(args?.toList() ?: emptyList<Any>())

        if (checkReturnValue && (result as Int) != 0) {
            val lastError = lookup.find("git_error_last").get()
            val descriptor = FunctionDescriptor.of(ValueLayout.ADDRESS)
            val error = (
                linker.downcallHandle(lastError, descriptor)
                    .invoke() as MemorySegment
                ).reinterpret(GitError.layout.byteSize())

            val message = (
                GitError.layout.varHandle(MemoryLayout.PathElement.groupElement("message"))
                    .get(error) as MemorySegment
                ).reinterpret(Long.MAX_VALUE)
            val klazz = GitError.layout.varHandle(MemoryLayout.PathElement.groupElement("klass")).get(error) as Int
            throw LibGitError(result, klazz, message.getUtf8String(0))
        }

        return result
    }

    companion object {
        internal fun Method.descriptor(): FunctionDescriptor? {
            val parameters = parameters.map { typeToLayout(it.type) }.toTypedArray()
            return if (returnType == Void.TYPE) {
                FunctionDescriptor.ofVoid(*parameters)
            } else {
                FunctionDescriptor.of(typeToLayout(returnType), *parameters)
            }
        }

        private fun typeToLayout(type: Class<*>): MemoryLayout = when (type) {
            Int::class.java -> ValueLayout.JAVA_INT
            Long::class.java -> ValueLayout.JAVA_LONG
            Boolean::class.java -> ValueLayout.JAVA_BOOLEAN
            MemorySegment::class.java, String::class.java -> ValueLayout.ADDRESS
            else -> TODO(type.toString())
        }
    }
}
