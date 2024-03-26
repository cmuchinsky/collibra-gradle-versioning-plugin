package com.collibra.gradle.plugins.versioning

import org.gradle.api.GradleException

class GitDirtyException : GradleException("Dirty git repository - cannot compute version.")
