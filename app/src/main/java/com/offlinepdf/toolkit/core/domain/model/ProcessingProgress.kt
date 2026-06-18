package com.offlinepdf.toolkit.core.domain.model

data class ProcessingProgress(
    val current: Int,
    val total: Int,
    val phase: Phase,
    val message: String? = null
) {
    val fraction: Float get() = if (total == 0) 0f else current.toFloat() / total

    enum class Phase { READING, PROCESSING, WRITING, DONE }
}
