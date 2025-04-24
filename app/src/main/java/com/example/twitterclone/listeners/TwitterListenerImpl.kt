package com.example.twitterclone.listeners

import android.app.AlertDialog
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.twitterclone.R
import com.example.twitterclone.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TwitterListenerImpl(
    private val tweetList: RecyclerView,
    var user: User?,
    private val callback: HomeCallback?
) : TweetListener {

    private val firebaseDB = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid


    override fun onComment(tweet: Tweet) {
        // Handle the comment action
        // For example, we show a simple Toast message for now,
        // but in a real app, you'd likely navigate to a comment screen or show a comment input bar

        // Show a Toast (you can replace this with actual UI changes)
        Toast.makeText(tweetList?.context, "Comment clicked on tweet: ${tweet.text}", Toast.LENGTH_SHORT).show()

        // Optionally: Show an input bar for the user to type a comment
        // You could make a view visible for typing a comment here in the activity or fragment
        // This part depends on your app structure
    }


    override fun onLayoutClick(tweet: Tweet?) {
        tweet?.let {
            val owner = tweet.userIds?.get(0)
            if (owner != userId) {
                if (user?.followUsers?.contains(owner) == true) {
                    AlertDialog.Builder(tweetList.context)
                        .setTitle("Unfollow ${tweet.username}?")
                        .setPositiveButton("yes") { dialog, which ->
                            tweetList.isClickable = false
                            var followedUsers = user?.followUsers
                            if(followedUsers == null) {
                                followedUsers = arrayListOf()
                            }
                            followedUsers?.remove(owner)
                            firebaseDB.collection(DATA_USERS).document(userId!!).update(DATA_USER_FOLLOW, followedUsers)
                                .addOnSuccessListener {
                                    tweetList.isClickable = true
                                    callback?.onUserUpdated()
                                }
                                .addOnFailureListener {
                                    tweetList.isClickable = true
                                }
                        }
                        .setNegativeButton("cancel") { dialog, which -> }
                        .show()
                } else {
                    AlertDialog.Builder(tweetList.context)
                        .setTitle("Follow ${tweet.username}?")
                        .setPositiveButton("yes") { dialog, which ->
                            tweetList.isClickable = false
                            var followedUsers = user?.followUsers
                            if(followedUsers == null) {
                                followedUsers = arrayListOf()
                            }
                            owner?.let {
                                followedUsers?.add(owner)
                                firebaseDB.collection(DATA_USERS).document(userId!!)
                                    .update(DATA_USER_FOLLOW, followedUsers)
                                    .addOnSuccessListener {
                                        tweetList.isClickable = true
                                        callback?.onUserUpdated()
                                    }
                                    .addOnFailureListener {
                                        tweetList.isClickable = true
                                    }
                            }
                        }
                        .setNegativeButton("cancel") { dialog, which -> }
                        .show()
                }
            }
        }
    }


    override fun onLike(tweet: Tweet?) {
        tweet?.let {
            tweetList.isClickable = false
            val likes = tweet.likes
            if (likes.contains(userId)) {
                likes.remove(userId)
            } else {
                likes.add(userId!!)
            }
            firebaseDB.collection(DATA_TWEETS).document(tweet.tweetId)
                .update(DATA_TWEETS_LIKES, likes)
                .addOnSuccessListener {
                    tweetList.isClickable = true
                    callback?.onRefresh()
                }
                .addOnFailureListener {
                    tweetList.isClickable = true
                }
        }
    }

    override fun onRetweet(tweet: Tweet?) {
        tweet?.let {
            tweetList.isClickable = false
            val retweets = tweet.retweets
            if (retweets.contains(userId)) {
                retweets.remove(userId)
            } else {
                retweets.add(userId!!)
            }
            firebaseDB.collection(DATA_TWEETS).document(tweet.tweetId)
                .update(DATA_TWEETS_RETWEETS, retweets)
                .addOnSuccessListener {
                    tweetList.isClickable = true
                    callback?.onRefresh()
                }
                .addOnFailureListener {
                    tweetList.isClickable = true
                }
        }
    }
}