package com.example.twitterclone.models

import com.google.gson.annotations.SerializedName

data class XTweetResponse(
    @SerializedName("data") val data: List<XTweet>?
)