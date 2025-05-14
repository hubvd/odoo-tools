package com.github.hubvd.odootools.libgit.legacy

import com.github.hubvd.odootools.libgit.legacy.LibGitInvocationHandler.Companion.descriptor
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout.ADDRESS

class LibGitFeature : Feature {
    override fun duringSetup(access: Feature.DuringSetupAccess?) {
        LibGit::class.java.methods.forEach {
            RuntimeForeignAccess.registerForDowncall(it.descriptor())
        }
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ADDRESS))
    }
}
