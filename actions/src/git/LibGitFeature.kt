package com.github.hubvd.odootools.actions.git

import com.github.hubvd.odootools.actions.git.LibGitInvocationHandler.Companion.descriptor
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
