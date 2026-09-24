package com.vt.smart.switchapp.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vt.smart.switchapp.databinding.ItemviewWifiBinding
import com.vt.smart.switchapp.ui.interfaces.ClickInterface

class WifiAdapter(
    private val context: Context,
    private val mArrayList: ArrayList<String>,
    private val clickInterface: ClickInterface
) : RecyclerView.Adapter<WifiAdapter.MViewHolder>() {

    inner class MViewHolder(val binding: ItemviewWifiBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
        return MViewHolder(ItemviewWifiBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MViewHolder, position: Int) {
        holder.binding.textView7.text = mArrayList[position]
        holder.binding.button.setOnClickListener {
            clickInterface.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return mArrayList.size
    }
}