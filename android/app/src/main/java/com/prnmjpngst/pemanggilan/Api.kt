package com.prnmjpngst.pemanggilan

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class VehicleDto(
    val id: String,
    val raw: Map<String, Any?>? = null,
    val status: String = "pending",
    val calledCount: Int = 0,
    val lastCalledAt: String? = null,
)

data class MetaResponse(
    val headers: List<String> = emptyList(),
    val count: Int = 0,
    val lastSync: String? = null,
)

data class ListResponse(
    val headers: List<String> = emptyList(),
    val lastSync: String? = null,
    val items: List<VehicleDto> = emptyList(),
)

data class IngestResponse(
    val ok: Boolean = false,
    val received: Int = 0,
    val created: Int = 0,
    val updated: Int = 0,
    val total: Int = 0,
)

interface PemanggilanApi {
    @GET("api/meta")
    suspend fun meta(@Query("apiKey") apiKey: String? = null): Response<MetaResponse>

    @GET("api/vehicles")
    suspend fun vehicles(
        @Query("status") status: String? = null,
        @Query("apiKey") apiKey: String? = null,
    ): Response<ListResponse>

    @POST("api/vehicles/{id}/call")
    suspend fun call(@Path("id") id: String, @retrofit2.http.Query("apiKey") apiKey: String? = null): Response<Map<String, Any>>

    @POST("api/vehicles/{id}/done")
    suspend fun done(@Path("id") id: String, @retrofit2.http.Query("apiKey") apiKey: String? = null): Response<Map<String, Any>>
}

fun createApi(baseUrl: String, apiKey: String): PemanggilanApi {
    val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    val client = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("X-API-Key", apiKey)
                .build()
            chain.proceed(req)
        }
        .build()
    return Retrofit.Builder()
        .baseUrl(normalized)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PemanggilanApi::class.java)
}