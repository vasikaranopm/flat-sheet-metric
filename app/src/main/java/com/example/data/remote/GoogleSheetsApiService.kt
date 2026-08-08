package com.example.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ValueRangeResponse(
    val range: String?,
    val majorDimension: String?,
    val values: List<List<String>>?
)

interface GoogleSheetsApiService {

    @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getSheetValues(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("key") apiKey: String
    ): Response<ValueRangeResponse>
}
