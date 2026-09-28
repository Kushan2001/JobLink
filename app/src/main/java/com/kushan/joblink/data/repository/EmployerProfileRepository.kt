package com.kushan.joblink.data.repository

import com.kushan.joblink.data.model.CompanyProfile

interface EmployerProfileRepository {
    suspend fun getCompanyProfile(): ProfileResult<CompanyProfile>

    suspend fun saveCompanyProfile(
        profile: CompanyProfile,
    ): ProfileResult<CompanyProfile>
}
