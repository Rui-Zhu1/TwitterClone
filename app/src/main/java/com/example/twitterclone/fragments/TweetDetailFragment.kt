package com.example.twitterclone.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.twitterclone.R
import com.example.twitterclone.utils.DATA_TWEETS
import com.example.twitterclone.utils.Tweet
import com.example.twitterclone.utils.getDate
import com.google.firebase.firestore.FirebaseFirestore

class TweetDetailFragment : Fragment() {

    private val firebaseDB = FirebaseFirestore.getInstance()
    private var tweetId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tweetId = arguments?.getString("tweetId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tweet_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val tweetText: TextView = view.findViewById(R.id.tweetText)
        val tweetImage: ImageView = view.findViewById(R.id.tweetImage)
        val tweetDate: TextView = view.findViewById(R.id.tweetDate)

        tweetId?.let { id ->
            firebaseDB.collection(DATA_TWEETS).document(id).get()
                .addOnSuccessListener { document ->
                    val tweet = document.toObject(Tweet::class.java)
                    tweet?.let {
                        usernameText.text = it.username ?: "Unknown"
                        tweetText.text = it.text
                        tweetDate.text = getDate(it.timestamp)
                        if (!it.imageUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(it.imageUrl)
                                .into(tweetImage)
                        } else {
                            tweetImage.visibility = View.GONE
                        }
                    } ?: run {
                        usernameText.text = "Error"
                        tweetText.text = "Tweet not found"
                    }
                }
                .addOnFailureListener { e ->
                    usernameText.text = "Error"
                    tweetText.text = "Failed to load tweet: ${e.message}"
                }
        } ?: run {
            usernameText.text = "Error"
            tweetText.text = "Invalid tweet ID"
        }
    }
}