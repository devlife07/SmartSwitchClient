package com.vt.smart.switchapp.ui.adapters

import android.content.Context
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

class ContentStateAdapter(
    val context: Context, fragmentManager: FragmentManager, lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments: MutableList<PageItem> = ArrayList()
    private val fragmentFactory: FragmentFactory = fragmentManager.fragmentFactory

    fun add(fragment: PageItem) {
        fragments.add(fragment)
    }

    override fun createFragment(pos: Int): Fragment {
        val item = getItem(pos)
        val fragment =
            item.fragment ?: fragmentFactory.instantiate(context.classLoader, item.pageclass)
        item.fragment = fragment
        return fragment
    }

    override fun getItemCount(): Int = fragments.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun getItem(position: Int): PageItem = synchronized(fragments) { fragments[position] }


    @Parcelize
    data class PageItem(var pagetitle: String, var pageclass: String) : Parcelable {
        @IgnoredOnParcel
        var fragment: Fragment? = null
    }
}