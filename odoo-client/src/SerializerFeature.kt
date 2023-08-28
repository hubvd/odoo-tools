package com.github.hubvd.odootools.odoo.client

import com.oracle.svm.hosted.FeatureImpl
import kotlinx.serialization.Serializable
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection

class SerializerFeature : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
        access as FeatureImpl.FeatureAccessImpl
        val serializableClasses = access.findAnnotatedClasses(Serializable::class.java)
        serializableClasses.forEach {
            RuntimeReflection.registerFieldLookup(it, "Companion")
            val companionClass = it.declaredFields.find { it.name == "Companion" }?.type
            if (companionClass != null) {
                RuntimeReflection.register(companionClass.declaredMethods.find { it.name == "serializer" })
            }
        }
    }
}
