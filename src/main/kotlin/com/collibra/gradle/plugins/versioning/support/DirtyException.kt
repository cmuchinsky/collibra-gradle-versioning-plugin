package com.collibra.gradle.plugins.versioning.support

import org.gradle.api.GradleException

class DirtyException : GradleException("Dirty working copy - cannot compute version.")
