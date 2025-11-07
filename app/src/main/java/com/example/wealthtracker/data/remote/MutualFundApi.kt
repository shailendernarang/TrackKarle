package com.example.wealthtracker.data.remote

import com.example.wealthtracker.data.remote.dto.MutualFundDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MutualFundApi {
    @GET("api/mutual-funds")
    suspend fun searchMutualFunds(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<MutualFundDto>

    @GET("api/mutual-funds/{schemeCode}")
    suspend fun getMutualFundDetails(
        @Path("schemeCode") schemeCode: String
    ): MutualFundDto

    @GET("api/mutual-funds/trending")
    suspend fun getTrendingFunds(
        @Query("limit") limit: Int = 10
    ): List<MutualFundDto>
}
