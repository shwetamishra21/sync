package com.jsac.sync.data.repository

import com.jsac.sync.data.remote.api.HealthApi
import javax.inject.Inject

class HealthRepository @Inject constructor(
    private val api: HealthApi
) {

    suspend fun checkHealth() = api.checkHealth()
}