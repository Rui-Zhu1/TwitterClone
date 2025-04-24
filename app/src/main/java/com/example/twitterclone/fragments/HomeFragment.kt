package com.example.twitterclone.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twitterclone.BuildConfig
import com.example.twitterclone.R
import com.example.twitterclone.adapters.TweetListAdapter
import com.example.twitterclone.listeners.TwitterListenerImpl
import com.example.twitterclone.network.XApiClient
import com.example.twitterclone.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class HomeFragment : TwitterFragment() {

    private var isRefreshing = AtomicBoolean(false)
    private var lastApiCallTime = 0L
    private val minApiCallInterval = 15 * 60 * 1000L
    private var cachedTweets: List<Tweet> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("HomeFragment", "Bearer Token: ${BuildConfig.X_BEARER_TOKEN}")

        listener = TwitterListenerImpl(tweetList, currentUser, callback)
        tweetsAdapter = TweetListAdapter(userId!!, arrayListOf())
        tweetsAdapter?.setListener(listener)
        tweetList?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tweetsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setItemViewCacheSize(20)
            setRecycledViewPool(RecyclerView.RecycledViewPool())
        }

        swipeRefresh?.isEnabled = false

        cachedTweets = loadTweetsFromPrefs()
        if (cachedTweets.isNotEmpty()) {
            Log.d("HomeFragment", "Loaded ${cachedTweets.size} cached tweets")
            updateAdapter(cachedTweets)
            tweetList?.visibility = View.VISIBLE
            progressBar?.visibility = View.GONE
        } else {
            updateList()
        }
    }

    override fun updateList() {
        if (!isRefreshing.compareAndSet(false, true)) {
            Log.d("HomeFragment", "Update ignored due to ongoing request")
            return
        }

        tweetList?.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE

        if (System.currentTimeMillis() - lastApiCallTime < minApiCallInterval) {
            Log.d("HomeFragment", "API call blocked: within 15-minute window")
            Toast.makeText(context, "Using cached or Firestore tweets", Toast.LENGTH_LONG).show()
            if (cachedTweets.isNotEmpty()) {
                updateAdapter(cachedTweets)
                tweetList?.visibility = View.VISIBLE
                progressBar?.visibility = View.GONE
            } else {
                fetchFromFirestore()
            }
            isRefreshing.set(false)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("HomeFragment", "Attempting X API v2 call")
                val response = withContext(Dispatchers.IO) {
                    XApiClient.apiService.getRecentTweets(query = "from:elonmusk")
                }
                Log.d("HomeFragment", "X API Response: $response")
                val xTweets = response.data?.map { Tweet.fromXTweet(it, response.includes) } ?: emptyList()
                Log.d("HomeFragment", "Mapped X Tweets: $xTweets")
                isRefreshing.set(false)
                lastApiCallTime = System.currentTimeMillis()
                if (xTweets.isNotEmpty()) {
                    cachedTweets = xTweets
                    saveTweetsToPrefs(xTweets)
                    updateAdapter(xTweets)
                    tweetList?.visibility = View.VISIBLE
                    progressBar?.visibility = View.GONE
                } else {
                    Log.d("HomeFragment", "No X API tweets, falling back to Firestore")
                    Toast.makeText(context, "No X API tweets found", Toast.LENGTH_SHORT).show()
                    fetchFromFirestore()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "X API Error: ${e.message}", e)
                isRefreshing.set(false)
                if (e.message?.contains("429") == true) {
                    Log.e("HomeFragment", "Rate limit exceeded (429)")
                    Toast.makeText(context, "Rate limit exceeded, please wait", Toast.LENGTH_LONG).show()
                    lastApiCallTime = System.currentTimeMillis()
                    if (cachedTweets.isNotEmpty()) {
                        updateAdapter(cachedTweets)
                        tweetList?.visibility = View.VISIBLE
                        progressBar?.visibility = View.GONE
                    } else {
                        fetchFromFirestore()
                    }
                } else {
                    Log.e("HomeFragment", "Non-429 error, falling back to Firestore")
                    Toast.makeText(context, "Failed to load X tweets: ${e.message}", Toast.LENGTH_SHORT).show()
                    fetchFromFirestore()
                }
            }
        }
    }

    private fun fetchFromFirestore() {
        currentUser?.let {
            val tweets = arrayListOf<Tweet>()
            Log.d("HomeFragment", "Fetching Firestore tweets for user: ${currentUser?.username}")

            val hashtagJobs = it.followHashtags?.map { hashtag ->
                firebaseDB.collection(DATA_TWEETS).whereArrayContains(DATA_TWEET_HASHTAGS, hashtag).limit(10).get()
                    .addOnSuccessListener { list ->
                        for (document in list.documents) {
                            val tweet = document.toObject(Tweet::class.java)
                            tweet?.let { tweets.add(it) }
                        }
                        updateAdapter(tweets)
                        tweetList?.visibility = View.VISIBLE
                        progressBar?.visibility = View.GONE
                        if (tweets.isEmpty()) {
                            Log.d("HomeFragment", "No hashtag tweets found")
                            Toast.makeText(context, "No hashtag tweets found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Firestore hashtag error: ${e.message}", e)
                        tweetList?.visibility = View.VISIBLE
                        progressBar?.visibility = View.GONE
                        Toast.makeText(context, "Failed to load hashtag tweets: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            val userJobs = it.followUsers?.map { followedUser ->
                firebaseDB.collection(DATA_TWEETS).whereArrayContains(DATA_TWEET_USER_IDS, followedUser).limit(10).get()
                    .addOnSuccessListener { list ->
                        for (document in list.documents) {
                            val tweet = document.toObject(Tweet::class.java)
                            tweet?.let { tweets.add(it) }
                        }
                        updateAdapter(tweets)
                        tweetList?.visibility = View.VISIBLE
                        progressBar?.visibility = View.GONE
                        if (tweets.isEmpty()) {
                            Log.d("HomeFragment", "No user tweets found")
                            Toast.makeText(context, "No user tweets found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Firestore user error: ${e.message}", e)
                        tweetList?.visibility = View.VISIBLE
                        progressBar?.visibility = View.GONE
                        Toast.makeText(context, "Failed to load user tweets: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            if (hashtagJobs.isNullOrEmpty() && userJobs.isNullOrEmpty()) {
                Log.d("HomeFragment", "No hashtags or users to fetch")
                tweetList?.visibility = View.VISIBLE
                progressBar?.visibility = View.GONE
                Toast.makeText(context, "No followed hashtags or users", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.d("HomeFragment", "No current user, skipping Firestore fetch")
            tweetList?.visibility = View.VISIBLE
            progressBar?.visibility = View.GONE
            Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAdapter(tweets: List<Tweet>) {
        val sortedTweets = tweets.sortedWith(compareByDescending { it.timestamp })
        tweetsAdapter?.updateTweets(removeDuplicates(sortedTweets))
        Log.d("HomeFragment", "Updated adapter with ${sortedTweets.size} tweets")
    }

    private fun removeDuplicates(originalList: List<Tweet>) = originalList.distinctBy { it.tweetId }

    private fun saveTweetsToPrefs(tweets: List<Tweet>) {
        val prefs = context?.getSharedPreferences("TwitterClone", Context.MODE_PRIVATE)
        val json = Gson().toJson(tweets)
        prefs?.edit()?.putString("cached_tweets", json)?.apply()
        Log.d("HomeFragment", "Saved ${tweets.size} tweets to SharedPreferences")
    }

    private fun loadTweetsFromPrefs(): List<Tweet> {
        val prefs = context?.getSharedPreferences("TwitterClone", Context.MODE_PRIVATE)
        val json = prefs?.getString("cached_tweets", null) ?: return emptyList()
        return Gson().fromJson(json, object : TypeToken<List<Tweet>>() {}.type)
    }
}