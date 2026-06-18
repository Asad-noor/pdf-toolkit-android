package com.offlinepdf.toolkit.core.domain.model

sealed class SplitMode {
    data class ByRange(val ranges: List<IntRange>) : SplitMode()
    data class EveryN(val n: Int) : SplitMode()
    data class AtPages(val pageNumbers: List<Int>) : SplitMode()
}
