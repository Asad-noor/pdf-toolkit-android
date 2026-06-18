package com.example.pdf_utility_app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object MergePdfs : NavKey
@Serializable data object SplitPdf : NavKey
@Serializable data object ExtractPages : NavKey
@Serializable data object DeletePages : NavKey
@Serializable data object ReorderPages : NavKey
@Serializable data object RotatePages : NavKey
@Serializable data object CompressPdf : NavKey
@Serializable data object AddWatermark : NavKey
@Serializable data object PasswordProtect : NavKey
@Serializable data object UnlockPdf : NavKey
@Serializable data object ExtractText : NavKey
@Serializable data object ImagesToPdf : NavKey
