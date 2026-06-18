package com.offlinepdf.toolkit.worker

object WorkerKeys {
    const val KEY_OPERATION = "operation"
    const val KEY_INPUT_URIS = "input_uris"
    const val KEY_OUTPUT_URI = "output_uri"
    const val KEY_OUTPUT_DIR_URI = "output_dir_uri"
    const val KEY_PASSWORD = "password"
    const val KEY_NEW_PASSWORD = "new_password"
    const val KEY_PAGE_NUMBERS = "page_numbers"
    const val KEY_NEW_ORDER = "new_order"
    const val KEY_ROTATION = "rotation"
    const val KEY_COMPRESSION_LEVEL = "compression_level"
    const val KEY_SPLIT_MODE = "split_mode"
    const val KEY_WATERMARK_CONFIG = "watermark_config"
    const val KEY_PASSWORD_CONFIG = "password_config"
    const val KEY_PROGRESS_CURRENT = "progress_current"
    const val KEY_PROGRESS_TOTAL = "progress_total"
    const val KEY_PROGRESS_PHASE = "progress_phase"
    const val KEY_RESULT_OUTPUT_URI = "result_output_uri"
    const val KEY_RESULT_OUTPUT_NAME = "result_output_name"
    const val KEY_ERROR = "error_message"

    enum class Operation {
        MERGE, SPLIT, COMPRESS, ROTATE, EXTRACT_PAGES,
        ADD_WATERMARK, PASSWORD_PROTECT, UNLOCK,
        IMAGES_TO_PDF, EXTRACT_TEXT, REORDER, DELETE_PAGES
    }
}
