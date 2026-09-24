package com.vt.smart.switchapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ui.models.HistoryModel

class HistoryAdapter(
    var list: List<Any>,
    val clickListener: (recentCall: Any) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var VIEW_TYPE_DATA = 0
    private var VIEW_TYPE_DATE = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == VIEW_TYPE_DATA) DataViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_history,
                parent,
                false
            )
        )
        else {
            DateViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.list_date,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val recentCall = list[position]
        if (recentCall is HistoryModel && holder is DataViewHolder)
            holder.setItem(recentCall)
        else if (recentCall is String && holder is DateViewHolder)
            holder.bind(recentCall)
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position] is HistoryModel)
            VIEW_TYPE_DATA
        else VIEW_TYPE_DATE
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun submitList(contactList: List<Any>) {
        this.list = contactList
        notifyDataSetChanged()
    }

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDateHeader: TextView = itemView.findViewById(R.id.text_date)
        fun bind(date: String) {
            textDateHeader.text = date
        }
    }

    inner class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val imgPlaceholder: ImageView = itemView.findViewById(R.id.imgItem)
        val btnOpen: Button = itemView.findViewById(R.id.btnOpen)
        fun setItem(historyModel: HistoryModel) {
            btnOpen.setOnClickListener {
                clickListener(historyModel)
            }
            title.text = historyModel.name
            if (historyModel.name.endsWith(".apk")) {

                Glide.with(itemView.context)
                    .load(R.drawable.ic_apk)
                    .into(imgPlaceholder)
            } else {
                if (historyModel.name.endsWith(".mp3")
                    || historyModel.name.endsWith(".flac") ||
                    historyModel.name.endsWith(".m4a") ||
                    historyModel.name.endsWith(".mp3") ||
                    historyModel.name.endsWith(".wav") ||
                    historyModel.name.endsWith(".ogg") ||
                    historyModel.name.endsWith(".amr") ||
                    historyModel.name.endsWith(".opus")
                )
                    Glide.with(imgPlaceholder).load(R.drawable.ic_music).into(imgPlaceholder)
                else if (historyModel.name.endsWith(".ppt"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_ppt).into(imgPlaceholder)
                else if (historyModel.name.endsWith(".txt"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_text).into(imgPlaceholder)
                else if (historyModel.name.endsWith(".xls") ||
                    historyModel.name.endsWith(".xlns") ||
                    historyModel.name.endsWith(".xlsx")
                )
                    Glide.with(imgPlaceholder).load(R.drawable.ic_excel).into(imgPlaceholder)
                else if (historyModel.name.endsWith(".doc") || historyModel.name.endsWith(".docx"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_document).into(imgPlaceholder)
                else if (historyModel.name.endsWith(".pdf"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_pdf).into(imgPlaceholder)
                else if (historyModel.name.endsWith(".vcf"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_contact).into(imgPlaceholder)
                else
                    Glide.with(imgPlaceholder).load(historyModel.path).into(imgPlaceholder)
            }
        }
    }
}