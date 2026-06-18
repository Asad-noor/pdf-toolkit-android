package com.offlinepdf.toolkit.core.domain.model

data class PasswordConfig(
    val userPassword: String,
    val ownerPassword: String = userPassword,
    val allowPrinting: Boolean = true,
    val allowCopying: Boolean = false
)
