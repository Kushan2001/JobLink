package com.kushan.joblink.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class CvMetadata(
    val fileName: String = "",
    val storagePath: String = "",
    val contentType: String = PDF_CONTENT_TYPE,
    val sizeBytes: Long = 0L,
    val uploadedAt: Timestamp? = null,
)

data class CvUploadFile(
    val uri: String,
    val fileName: String,
    val contentType: String?,
) {
    val isPdf: Boolean
        get() = contentType.equals(PDF_CONTENT_TYPE, ignoreCase = true) &&
            fileName.endsWith(PDF_EXTENSION, ignoreCase = true)
}

const val PDF_CONTENT_TYPE = "application/pdf"
private const val PDF_EXTENSION = ".pdf"
