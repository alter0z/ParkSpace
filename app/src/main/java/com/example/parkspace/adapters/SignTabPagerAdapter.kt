package com.example.parkspace.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.parkspace.view.fragments.SignIn
import com.example.parkspace.view.fragments.SignUp

class SignTabPagerAdapter(val context: Context, fragmentManager: FragmentManager, private val totalTab: Int): FragmentPagerAdapter(fragmentManager){
    override fun getCount() = totalTab

    override fun getItem(position: Int): Fragment {
        return when (position){
            0 -> SignIn()
            1 -> SignUp()
            else -> getItem(position)
        }
    }
}