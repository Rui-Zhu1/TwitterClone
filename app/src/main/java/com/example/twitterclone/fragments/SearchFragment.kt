package com.example.twitterclone.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twitterclone.R
import com.example.twitterclone.adapters.TweetListAdapter
import com.example.twitterclone.listeners.TwitterListenerImpl

import com.example.twitterclone.utils.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.*
import kotlin.random.Random

class SearchFragment : TwitterFragment() {

    private var currentKeyword = ""
    private var keywordFollowed = false
    private var isBulletModeEnabled = false
    private val bulletScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listener = TwitterListenerImpl(tweetList, currentUser, callback)

        tweetsAdapter = TweetListAdapter(userId!!, arrayListOf()).apply {
            setListener(listener)
            setCommentIconVisibility(true)
        }

        tweetList?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tweetsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
            updateList()
        }

        followHashtag.setOnClickListener {
            followHashtag.isClickable = false
            val followed = currentUser?.followHashtags
            if (keywordFollowed) followed?.remove(currentKeyword)
            else followed?.add(currentKeyword)

            firebaseDB.collection(DATA_USERS).document(userId)
                .update(DATA_USER_HASTAGS, followed)
                .addOnSuccessListener {
                    callback?.onUserUpdated()
                    followHashtag.isClickable = true
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    followHashtag.isClickable = true
                }
        }
    }

    fun newKeyword(term: String) {
        currentKeyword = term
        followHashtag.visibility = View.VISIBLE
        updateList()
    }

    override fun updateList() {
        tweetList?.visibility = View.GONE
        commentOverlay?.removeAllViews()

        firebaseDB.collection(DATA_TWEETS).get()
            .addOnSuccessListener { list ->
                tweetList?.visibility = View.VISIBLE
                val tweets = list.documents.mapNotNull { it.toObject(Tweet::class.java) }
                    .filter { it.text.contains(currentKeyword, ignoreCase = true) }
                    .sortedByDescending { it.timestamp }

                tweetsAdapter?.updateTweets(tweets)

                if (isBulletModeEnabled) loadFlyingComments()
            }
            .addOnFailureListener { it.printStackTrace() }

        updateFollowDrawable()
    }

    private fun updateFollowDrawable() {
        keywordFollowed = currentUser?.followHashtags?.contains(currentKeyword) == true
        context?.let {
            val drawable = if (keywordFollowed) R.drawable.follow else R.drawable.follow_inactive
            followHashtag.setImageDrawable(ContextCompat.getDrawable(it, drawable))
        }
    }

    fun setBulletMode(isEnabled: Boolean) {
        isBulletModeEnabled = isEnabled
        updateList()
    }

    private fun loadFlyingComments() {
        bulletScope.coroutineContext.cancelChildren() // Clear previous animations

        FirebaseFirestore.getInstance().collection(DATA_COMMENTS)
            .whereGreaterThan("timestamp", System.currentTimeMillis() - 86400000) // last 24h
            .get()
            .addOnSuccessListener { result ->
                result.documents.mapNotNull { it.toObject(Comment::class.java) }
                    .filter { it.commentText.contains(currentKeyword, ignoreCase = true) }
                    .forEachIndexed { index, comment ->
                        val delayTime = Random.nextLong(0L, 5000L)
                        bulletScope.launch {
                            delay(delayTime)
                            spawnFlyingComment(comment.commentText)
                        }
                    }
            }
            .addOnFailureListener { it.printStackTrace() }
    }
    private fun applyBulletModeAnimation() {
        commentOverlay?.removeAllViews()

        val tweets = tweetsAdapter?.getTweets() ?: return
        for (tweet in tweets) {
            FirebaseFirestore.getInstance()
                .collection("comments")
                .whereEqualTo("tweetId", tweet.tweetId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val comments = snapshot.documents.mapNotNull { it.getString("commentText") }

                    comments.forEachIndexed { index, commentText ->
                        val commentView = TextView(requireContext()).apply {
                            text = commentText
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            setBackgroundResource(R.drawable.bullet_bg)
                            textSize = 14f
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = 100 + (index * 120) // spread vertically
                            }
                        }

                        commentOverlay?.addView(commentView)

                        commentView.translationX = commentOverlay!!.width.toFloat()
                        commentView.animate()
                            .translationX(-commentView.width.toFloat())
                            .setDuration(6000)
                            .start()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BulletComment", "Error loading comments: ${e.message}")
                }
        }
    }



    private fun spawnFlyingComment(text: String) {
        context?.let { ctx ->
            val textView = TextView(ctx).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                setBackgroundResource(R.drawable.bullet_bg)
                setPadding(20, 10, 20, 10)
                textSize = 16f
                elevation = 6f
            }

            commentOverlay?.addView(textView)

            commentOverlay?.doOnLayout {
                val startX = commentOverlay.width
                val endX = -textView.paint.measureText(text).toInt()
                val maxY = commentOverlay.height - 100
                val posY = Random.nextInt(0, maxY.coerceAtLeast(1))

                val speed = Random.nextLong(3000L, 8000L)

                textView.translationX = startX.toFloat()
                textView.translationY = posY.toFloat()

                ObjectAnimator.ofFloat(textView, "translationX", startX.toFloat(), endX.toFloat()).apply {
                    duration = speed
                    start()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bulletScope.cancel()
    }
}
