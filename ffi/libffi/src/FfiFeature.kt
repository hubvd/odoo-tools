package com.github.hubvd.odootools.ffi.libffi

import com.github.hubvd.odootools.ffi.libffi.test.git_error
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeProxyCreation

class FfiFeature : Feature {
    override fun duringSetup(access: Feature.DuringSetupAccess?) {
        RuntimeProxyCreation.register(git_error::class.java)
    }
}
