@file:Suppress("ktlint:standard:filename")

package com.github.hubvd.odootools.libgit

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

val structClass = ClassName("your.package.name", "Struct")
val structFactoryClass = ClassName("your.package.name", "StructFactory")
val memorySegmentClass = ClassName("your.package.name", "MemorySegment")
val memoryLayoutClass = ClassName("your.package.name", "MemoryLayout")
val addressClass = ClassName("java.lang.foreign.ValueLayout", "ADDRESS")
val javaLongClass = ClassName("java.lang.foreign.ValueLayout", "JAVA_LONG")
val javaIntClass = ClassName("java.lang.foreign.ValueLayout", "JAVA_INT")
val javaShortClass = ClassName("java.lang.foreign.ValueLayout", "JAVA_SHORT")
val javaCharClass = ClassName("java.lang.foreign.ValueLayout", "JAVA_CHAR")
val todoClass = ClassName("java.lang.foreign.ValueLayout", "TODO")

fun writeStruct(struct: Struct): String {
    val gitBuffClass = TypeSpec.classBuilder(struct.name)
        .addModifiers(KModifier.INTERNAL)
        .addSuperinterface(structClass)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("segment", memorySegmentClass)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("segment", memorySegmentClass, KModifier.OVERRIDE)
                .initializer("segment")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("ptr", addressClass)
                .delegate("layout.pointer()")
                .build(),
        )
        .addType(
            TypeSpec.companionObjectBuilder()
                .addSuperinterface(structFactoryClass.parameterizedBy(ClassName("your.package.name", "GitBuff")))
                .addProperty(
                    PropertySpec.builder("layout", memoryLayoutClass, KModifier.OVERRIDE)
                        .initializer(layout(struct))
                        .build(),
                )
                .addFunction(
                    FunSpec.builder("invoke")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("segment", memorySegmentClass)
                        .returns(ClassName("your.package.name", "GitBuff"))
                        .addStatement("return GitBuff(segment)")
                        .build(),
                )
                .build(),
        )
        .build()

    val fs = FileSpec.builder("your.package.name", "GitBuff")
        .addType(gitBuffClass)
        .build()
    return buildString {
        fs.writeTo(this)
    }
}

private fun layout(struct: Struct): CodeBlock {
    if (struct.opaque) return CodeBlock.of("ADDRESS")
    return CodeBlock.of(
        struct.members.joinToString(",\n", prefix = "%T.structLayout(\n", postfix = ")") { "%T.withName(%S)" },
        memoryLayoutClass,
        *buildList {
            struct.members.forEach {
                // FIXME: padding
                when (it.type) {
                    "char", "unsigned char" -> add(javaCharClass)
                    "uint16_t" -> add(javaShortClass)
                    "unsigned int", "uint32_t", "int" -> add(javaIntClass)
                    "size_t" -> add(javaLongClass)
                    "char *", "const char *", "void *" -> add(addressClass)
                    else -> {
                        println(it.name + ":" + it.type)
                        add(todoClass)
                    }
                }
                add(it.name)
            }
        }.toTypedArray(),
    )
}
