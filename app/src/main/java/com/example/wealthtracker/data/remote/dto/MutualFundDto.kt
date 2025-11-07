package com.example.wealthtracker.data.remote.dto

data class MutualFundDto(
    val schemeCode: String,
    val schemeName: String,
    val nav: Double,
    val date: String,
    val category: String? = null,
    val fundHouse: String? = null,
    val oneYearReturn: Double? = null,
    val threeYearReturn: Double? = null,
    val fiveYearReturn: Double? = null
)
