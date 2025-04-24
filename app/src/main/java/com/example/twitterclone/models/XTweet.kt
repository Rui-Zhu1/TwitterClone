package com.example.twitterclone.models

import com.example.twitterclone.network.Attachments
import com.google.gson.annotations.SerializedName

data class XTweet(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("author_id") val authorId: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("attachments") val attachments: Attachments? = null
)
