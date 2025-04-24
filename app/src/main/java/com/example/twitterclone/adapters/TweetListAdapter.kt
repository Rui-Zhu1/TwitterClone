package com.example.twitterclone.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.twitterclone.R
import com.example.twitterclone.activities.HomeActivity
import com.example.twitterclone.listeners.TweetListener
import com.example.twitterclone.utils.Tweet
import com.example.twitterclone.utils.getDate
import com.google.firebase.firestore.FirebaseFirestore

class TweetListAdapter(
    val userId: String,
    val tweets: ArrayList<Tweet>,
    private var showCommentIcon: Boolean = false
) : RecyclerView.Adapter<TweetListAdapter.TweetViewHolder>() {

    private var listener: TweetListener? = null

    // Set listener for interactions
    fun setListener(listener: TweetListener?) {
        this.listener = listener
    }

    fun getTweets(): List<Tweet> {
        return tweets
    }

    // Control visibility of the comment icon
    fun setCommentIconVisibility(show: Boolean) {
        showCommentIcon = show
        notifyDataSetChanged()
    }

    // Update the tweet list
    fun updateTweets(newTweets: List<Tweet>) {
        tweets.clear()
        tweets.addAll(newTweets)
        notifyDataSetChanged()
        Log.d("TweetListAdapter", "Updated tweets: ${newTweets.size}")
    }

    // Create a new view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TweetViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_tweet, parent, false)
    )

    // Return item count
    override fun getItemCount() = tweets.size

    // Bind the view holder
    override fun onBindViewHolder(holder: TweetViewHolder, position: Int) {
        holder.bind(userId, tweets[position], listener, showCommentIcon)
    }

    // View holder class for tweet items
    class TweetViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private val layout = v.findViewById<ViewGroup>(R.id.tweetLayout)
        private val username = v.findViewById<TextView>(R.id.tweetUsername)
        private val text = v.findViewById<TextView>(R.id.tweetText)
        private val image = v.findViewById<ImageView>(R.id.tweetImage)
        private val date = v.findViewById<TextView>(R.id.tweetDate)
        private val like = v.findViewById<ImageView>(R.id.tweetLike)
        private val likeCount = v.findViewById<TextView>(R.id.tweetLikeCount)
        private val retweet = v.findViewById<ImageView>(R.id.tweetRetweet)
        private val retweetCount = v.findViewById<TextView>(R.id.tweetRetweetCount)
        private val comment = v.findViewById<ImageView>(R.id.tweetComment)
        private val commentInput = v.findViewById<EditText>(R.id.commentInput) // Comment input

        private var currentCommentText: String? = null // Store the typed comment text

        // Bind tweet data and set up click listeners
        fun bind(userId: String, tweet: Tweet, listener: TweetListener?, showComment: Boolean) {
            username.text = tweet.username ?: "Unknown"
            text.text = tweet.text
            date.text = getDate(tweet.timestamp)
            likeCount.text = tweet.likes.size.toString()
            retweetCount.text = tweet.retweets.size.toString()

            // Show or hide the comment icon based on visibility flag
            comment.visibility = if (showComment) View.VISIBLE else View.GONE
            comment.setOnClickListener {
                // When comment icon is clicked, show the comment input field
                commentInput.visibility = View.VISIBLE

                // Pre-fill the comment input with the previously typed comment
                commentInput.setText(currentCommentText)
            }

            // Listen for the comment input field "done" action (Enter key)
            commentInput.setOnEditorActionListener { v, actionId, event ->
                val commentText = v.text.toString()
                if (commentText.isNotEmpty()) {
                    // Save the comment to Firebase
                    saveCommentToFirebase(tweet, commentText, userId)
                    v.setText("") // Clear input field after submission
                    v.visibility = View.GONE // Hide input field
                    Toast.makeText(v.context, "Comment added!", Toast.LENGTH_SHORT).show()
                }
                true
            }

            // Add a listener for clicking anywhere outside the comment input
            layout.setOnClickListener {
                if (commentInput.visibility == View.VISIBLE) {
                    // Save the comment text (even if not submitted)
                    currentCommentText = commentInput.text.toString()
                    commentInput.visibility = View.GONE
                } else {
                    listener?.onLayoutClick(tweet)
                }
            }

            // Load image if available
            if (tweet.imageUrl.isNullOrEmpty()) {
                image.visibility = View.GONE
            } else {
                image.visibility = View.VISIBLE
                Glide.with(itemView.context).load(tweet.imageUrl).into(image)
            }

            // Set the like button state
            val likeDrawable = if (tweet.likes.contains(userId)) R.drawable.like else R.drawable.like_inactive
            like.setImageDrawable(ContextCompat.getDrawable(itemView.context, likeDrawable))

            // Set the retweet button state
            val retweetDrawable = when {
                tweet.userIds.getOrNull(0) == userId -> R.drawable.original
                tweet.retweets.contains(userId) -> R.drawable.retweet
                else -> R.drawable.retweet_inactive
            }
            retweet.setImageDrawable(ContextCompat.getDrawable(itemView.context, retweetDrawable))
            retweet.isClickable = tweet.userIds.getOrNull(0) != userId

            // Set up interactions for layout and other buttons
            like.setOnClickListener { listener?.onLike(tweet) }
            retweet.setOnClickListener { listener?.onRetweet(tweet) }
        }

        // Save the comment to Firebase Firestore
        private fun saveCommentToFirebase(tweet: Tweet, commentText: String, userId: String) {
            val db = FirebaseFirestore.getInstance()
            val commentData = hashMapOf(
                "commentText" to commentText,
                "userId" to userId,
                "tweetId" to tweet.tweetId, // Use tweetId instead of id
                "timestamp" to System.currentTimeMillis()
            )

            // Save to Firestore in the "comments" collection
            db.collection("comments").add(commentData)
                .addOnSuccessListener {
                    Log.d("Comment", "Comment added successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("Comment", "Error adding comment: ${e.message}")
                }
        }
    }
}
