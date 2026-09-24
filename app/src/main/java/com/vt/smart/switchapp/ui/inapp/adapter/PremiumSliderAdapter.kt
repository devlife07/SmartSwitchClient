package com.vt.smart.switchapp.ui.inapp.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter


class PremiumSliderAdapter (
    context: Context?
) : PagerAdapter() {
/*

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var binding: SliderItemBinding?=null
    private val imgs = intArrayOf(
        R.drawable.property_one,
        R.drawable.property_two,
        R.drawable.property_three)

    override fun instantiateItem(container: ViewGroup, position: Int): View {
        binding = SliderItemBinding.inflate(layoutInflater, container, false)
        container.addView(binding?.root)
        binding?.let { bindviews(it,position % imgs.size) }
        return binding?.root!!
    }

    private fun bindviews(binding: SliderItemBinding, position: Int) {
        val imglist= imgs[position]
        imglist.let { binding.imageView.loadWithGlide(it) }
    }
*/

    override fun getCount(): Int {
       // return imgs.size
        return Int.MAX_VALUE
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view === obj
    }

    override fun destroyItem(container: ViewGroup, position: Int, objects: Any) {
        val view = objects as View
        container.removeView(view)
    }
}