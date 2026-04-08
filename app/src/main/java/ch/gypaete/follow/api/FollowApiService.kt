package ch.gypaete.follow.api

import ch.gypaete.follow.model.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface FollowApiService {

    // ── GET list : charge tous les vols de la journée ──────────────────────
    @GET("api.php")
    suspend fun list(
        @Query("action") action: String = "list",
        @Query("room") room: Int = 1
    ): Response<ListResponse>

    // ── GET poll : events depuis since_id ──────────────────────────────────
    @GET("api.php")
    suspend fun poll(
        @Query("action") action: String = "poll",
        @Query("room") room: Int = 1,
        @Query("since_id") sinceId: Int = 0
    ): Response<PollResponse>

    // ── GET exercices FSVL ─────────────────────────────────────────────────
    @GET("api.php")
    suspend fun listExercices(
        @Query("action") action: String = "list_exercices"
    ): Response<ExercicesResponse>

    // ── POST actions (décolle / atterri / annule / etc.) ───────────────────
    @POST("api.php")
    @FormUrlEncoded
    suspend fun action(
        @Field("action")       action: String,
        @Field("vol_id")       volId: Int,
        @Field("utilisateur_id") utilisateurId: Int,
        @Field("room")         room: Int = 1
    ): Response<ActionResponse>

    // ── GET rooms de la journée ────────────────────────────────────────────
    @GET("api.php")
    suspend fun listRooms(
        @Query("action") action: String = "list_rooms",
        @Query("room") room: Int = 1
    ): Response<RoomsResponse>

    // ── POST décolle avec exercices ────────────────────────────────────────
    // Retrofit ne supporte pas List<Int> directement en @Field → on passe
    // par un FormBody dans le Repository
    @POST("api.php")
    suspend fun actionRaw(@Body body: FormBody): Response<ActionResponse>
}

// ── Singleton Retrofit ────────────────────────────────────────────────────────
object ApiClient {

    private var _service: FollowApiService? = null
    private var _baseUrl: String = ""

    fun init(baseUrl: String, sessionCookie: String) {
        _baseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            // Injecte le cookie de session PHP
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Cookie", sessionCookie)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build()
                chain.proceed(req)
            }
            .build()

        _service = Retrofit.Builder()
            .baseUrl(_baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FollowApiService::class.java)
    }

    val service: FollowApiService
        get() = _service ?: error("ApiClient non initialisé — appelle ApiClient.init() d'abord")
}
