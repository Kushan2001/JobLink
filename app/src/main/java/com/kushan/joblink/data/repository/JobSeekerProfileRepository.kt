package com.kushan.joblink.data.repository

import com.kushan.joblink.data.model.CvMetadata
import com.kushan.joblink.data.model.CvUploadFile
import com.kushan.joblink.data.model.JobSeekerProfile

enum class ProfileError {
    NOT_AUTHENTICATED,
    PROFILE_NOT_FOUND,
    WRONG_ROLE,
    PERMISSION_DENIED,
    NETWORK,
    UNKNOWN,
    INVALID_CV_FILE,
    CV_FILE_UNAVAILABLE,
    CV_UPLOAD_FAILED,
}

sealed interface ProfileResult<out T> {
    data class Success<T>(val value: T) : ProfileResult<T>
    data class Failure(val error: ProfileError) : ProfileResult<Nothing>
}

interface JobSeekerProfileRepository {
    suspend fun getProfile(): ProfileResult<JobSeekerProfile>

    suspend fun saveProfile(profile: JobSeekerProfile): ProfileResult<JobSeekerProfile>

    suspend fun uploadCv(
        file: CvUploadFile,
        onProgress: (Float) -> Unit,
    ): ProfileResult<CvMetadata>
}
