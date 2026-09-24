package com.vt.smart.switchapp.ui.adapters

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ui.models.FileModel

class DataPrewAdapter(
    val isMedia: Boolean = false,
    val list: ArrayList<FileModel>,
    val callback: (fileModel: FileModel, isSelected: Boolean) -> Unit,
    val onSelectAll: ((changed: List<FileModel>, isSelected: Boolean) -> Unit)? = null
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (isMedia) {
            MediaViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
            )
        } else {
            NonMediaViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_non_media, parent, false)
            )
        }
    }
    fun setSelected(checked: Boolean) {
        // Batch this instead of invoking the per-item callback in a loop:
        // that used to rebuild the whole selection set once per item, which
        // ANR'd the main thread on large lists ("select all" on galleries).
        val changed = ArrayList<FileModel>()
        for (index in list.indices) {
            if (list[index].isSelected == checked) continue
            list[index].isSelected = checked
            changed.add(list[index])
        }
        if (onSelectAll != null) {
            onSelectAll.invoke(changed, checked)
        } else {
            changed.forEach { callback.invoke(it, checked) }
        }
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NonMediaViewHolder)
            holder.setItem(list[position])
        else if (holder is MediaViewHolder)
            holder.setItem(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class NonMediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val imgPlaceholder: ImageView = itemView.findViewById(R.id.imgItem)
        val imgCheck: ImageView = itemView.findViewById(R.id.imgCheck)
        fun setItem(fileModel: FileModel) {
            imgCheck.setOnClickListener {
                Log.e("TESTTAG","CLicked")
                fileModel.isSelected = !fileModel.isSelected
                callback(fileModel, fileModel.isSelected)
                imgCheck.setImageResource(if (fileModel.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)

            }
            title.text = fileModel.name
            if (fileModel.appInfo != null) {
                try {
                    val icon: Drawable =
                        itemView.context.packageManager.getApplicationIcon(fileModel.appInfo.packageName)
                    Glide.with(imgPlaceholder).load(icon).into(imgPlaceholder)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                if (fileModel.name.endsWith(".mp3")
                    || fileModel.name.endsWith(".flac") ||
                    fileModel.name.endsWith(".m4a") ||
                    fileModel.name.endsWith(".mp3") ||
                    fileModel.name.endsWith(".wav") ||
                    fileModel.name.endsWith(".ogg") ||
                    fileModel.name.endsWith(".amr") ||
                    fileModel.name.endsWith(".opus")
                )
                    Glide.with(imgPlaceholder).load(R.drawable.audio_new).into(imgPlaceholder)
                else if (fileModel.name.endsWith(".ppt"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_ppt).into(imgPlaceholder)
                else if (fileModel.name.endsWith(".txt"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_text).into(imgPlaceholder)
                else if (fileModel.name.endsWith(".xls") || fileModel.name.endsWith(".xlns"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_excel).into(imgPlaceholder)
                else if (fileModel.name.endsWith(".doc") || fileModel.name.endsWith(".docx"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_document).into(imgPlaceholder)
                else if (fileModel.name.endsWith(".pdf"))
                    Glide.with(imgPlaceholder).load(R.drawable.ic_pdf).into(imgPlaceholder)
                else
                    Glide.with(imgPlaceholder).load(fileModel.path).into(imgPlaceholder)
            }
            imgCheck.setImageResource(if (fileModel.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
        }
    }

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imgPlaceholder: ImageView = itemView.findViewById(R.id.image_view)
        val imgCheck: ImageView = itemView.findViewById(R.id.imgCheck)
        fun setItem(fileModel: FileModel) {
            imgCheck.setOnClickListener {
                fileModel.isSelected = !fileModel.isSelected
                callback(fileModel, fileModel.isSelected)
                imgCheck.setImageResource(if (fileModel.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)

            }
            Glide.with(imgPlaceholder).load(fileModel.path).into(imgPlaceholder)
            imgCheck.setImageResource(if (fileModel.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
        }
    }
}