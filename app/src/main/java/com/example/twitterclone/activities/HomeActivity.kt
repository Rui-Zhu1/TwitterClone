package com.example.twitterclone.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.twitterclone.R
import com.example.twitterclone.fragments.*
import com.example.twitterclone.listeners.HomeCallback
import com.example.twitterclone.utils.*
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), HomeCallback {

    private lateinit var toggleBulletMode: Switch
    private var sectionsPagerAdapter: SectionPageAdapter? = null
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val myActivityFragment = MyActivityFragment()
    private var userId = firebaseAuth.currentUser?.uid
    private var user: User? = null
    private var currentFragment: TwitterFragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        toggleBulletMode = findViewById(R.id.toggleBulletMode)
        toggleBulletMode.setOnCheckedChangeListener { _, isChecked ->
            if (currentFragment is SearchFragment) {
                (currentFragment as SearchFragment).setBulletMode(isChecked)
            }
        }

        sectionsPagerAdapter = SectionPageAdapter(supportFragmentManager)
        container.adapter = sectionsPagerAdapter
        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFragment = when (tab?.position) {
                    0 -> homeFragment
                    1 -> searchFragment
                    2 -> myActivityFragment
                    else -> homeFragment
                }

                titleBar.text = when (tab?.position) {
                    0 -> "Home"
                    2 -> "My Activity"
                    else -> ""
                }

                titleBar.visibility = if (tab?.position == 1) View.GONE else View.VISIBLE
                searchBar.visibility = if (tab?.position == 1) View.VISIBLE else View.GONE
                toggleCardView.visibility = if (tab?.position == 1) View.VISIBLE else View.GONE
            }
        })

        logo.setOnClickListener {
            startActivity(ProfileActivity.newIntent(this))
        }

        fab.setOnClickListener {
            startActivity(TweetActivity.newIntent(this, userId, user?.username))
        }

        homeProgressLayout.setOnTouchListener { _, _ -> true }

        search.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = v?.text.toString()
                searchFragment.newKeyword(keyword)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            startActivity(LoginActivity.newIntent(this))
            finish()
        } else {
            populate()
        }
    }

    override fun onUserUpdated() {
        populate()
    }

    override fun onRefresh() {
        currentFragment.updateList()
    }

    private fun populate() {
        homeProgressLayout.visibility = View.VISIBLE
        firebaseDB.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener {
                homeProgressLayout.visibility = View.GONE
                user = it.toObject(User::class.java)
                user?.imageUrl?.let { url -> logo.loadUrl(url, R.drawable.logo) }
                updateFragmentUser()
            }
            .addOnFailureListener {
                it.printStackTrace()
                finish()
            }
    }

    private fun updateFragmentUser() {
        homeFragment.setUser(user)
        searchFragment.setUser(user)
        myActivityFragment.setUser(user)
        currentFragment.setUser(user)
        currentFragment.updateList()
    }

    inner class SectionPageAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment = when (position) {
            0 -> homeFragment
            1 -> searchFragment
            else -> myActivityFragment
        }

        override fun getCount(): Int = 3
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }
}
