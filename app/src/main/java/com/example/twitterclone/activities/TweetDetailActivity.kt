//package com.example.twitterclone.activities
//
//import android.os.Bundle
//import android.text.format.DateFormat
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import com.bumptech.glide.Glide
//import com.example.twitterclone.R
//import com.example.twitterclone.utils.Tweet
//
//class TweetDetailActivity : AppCompatActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.tweet_detail)
//
//        val tweet = intent.getParcelableExtra<Tweet>("tweet") ?: return
//
//        val usernameText = findViewById<TextView>(R.id.tvUsername)
//        val tweetText = findViewById<TextView>(R.id.tvTweetText)
//        val tweetImage = findViewById<ImageView>(R.id.imgTweet)
//        val timestamp = findViewById<TextView>(R.id.tvTimestamp)
//
//        usernameText.text = tweet.username
//        tweetText.text = tweet.text
//        timestamp.text = DateFormat.format("dd MMM yyyy hh:mm a", tweet.timestamp)
//
//        if (!tweet.imageUrl.isNullOrEmpty()) {
//            tweetImage.visibility = ImageView.VISIBLE
//            Glide.with(this).load(tweet.imageUrl).into(tweetImage)
//        }
//    }
//}
