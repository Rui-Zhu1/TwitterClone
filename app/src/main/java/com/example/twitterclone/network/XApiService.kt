package com.example.twitterclone.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import com.example.twitterclone.models.XTweet

interface XApiService {
    @GET("2/tweets/search/recent")
    suspend fun getRecentTweets(
        @Query("query") query: String,
        @Query("expansions") expansions: String = "author_id",
        @Query("user.fields") userFields: String = "username",
        @Header("Authorization") authHeader: String = "Bearer AAAAAAAAAAAAAAAAAAAAANDl0gEAAAAAoMg%2FpHnMIPNz9zwyFDvkcoY5kT0%3DE4IEHIH7FzTYzXSRk0VFdOaWW280Adnwy2nJZgIPWCmFP40eU9"
    ): XTweetResponse
}

data class XTweetResponse(
    val data: List<XTweet>?,
    val includes: Includes?
)

data class Includes(
    val users: List<XUser>?
)

data class XUser(
    val id: String,
    val username: String
)



data class Attachments(
    val mediaKeys: List<String>?
)