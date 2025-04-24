package com.example.twitterclone.models

import com.google.gson.annotations.SerializedName

data class XAttachments(
    @SerializedName("media_keys") val mediaKeys: List<String>?
)