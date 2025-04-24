package com.example.twitterclone.utils

import android.util.Log



data class User(
    val email: String? = "",
    val username: String? = "",
    val imageUrl: String? = "",
    val followHashtags: ArrayList<String>? = arrayListOf(),
    val followUsers: ArrayList<String>? = arrayListOf()
)
data class Comment(
    val commentText: String = "",
    val timestamp: Long = 0L,
    val tweetId: String = "",
    val userId: String = ""
)


data class Tweet(
    val tweetId: String = "",
    val userIds: ArrayList<String> = arrayListOf(),
    val username: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0,
    val hashtags: ArrayList<String> = arrayListOf(),
    val likes: ArrayList<String> = arrayListOf(),
    val retweets: ArrayList<String> = arrayListOf(),
    val comments: ArrayList<Comment> = ArrayList()
) {
    companion object {
        fun fromXTweet(xTweet: com.example.twitterclone.models.XTweet, includes: com.example.twitterclone.network.Includes?): Tweet {
            val username = includes?.users?.find { it.id == xTweet.authorId }?.username ?: "Unknown"
            Log.d("TweetConversion", "Author ID: ${xTweet.authorId}")
            val hashtags = extractHashtags(xTweet.text)
            return Tweet(
                tweetId = xTweet.id,
                userIds = arrayListOf(xTweet.authorId ?: ""),
                username = username,
                text = xTweet.text,
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                hashtags = hashtags,
                likes = arrayListOf(),
                retweets = arrayListOf()
            )
        }

        fun extractHashtags(text: String): ArrayList<String> {
            val hashtags = arrayListOf<String>()
            val regex = Regex("#\\w+")
            regex.findAll(text).forEach { hashtags.add(it.value.removePrefix("#").lowercase()) }
            return hashtags
        }
    }
}