package com.vt.smart.switchapp.ui.fragment

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.databinding.FragmentMusicBinding
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.ui.adapters.DataPrewAdapter
import com.vt.smart.switchapp.ui.viewmodel.FileSelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicFragment : Fragment() {

    lateinit var binding: FragmentMusicBinding
    private var mMusicsSize: Long = 0
    private var mMusicList: ArrayList<FileModel> = ArrayList()
    private var selectedList: ArrayList<FileModel> = ArrayList()
    val viewModel: FileSelectionViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            loadMusic()
            withContext(Dispatchers.Main)
            {
                binding.noItemFound.visibility =
                    if (mMusicList.isEmpty()) View.VISIBLE else View.GONE
                binding.layoutMain.visibility =
                    if (mMusicList.isNotEmpty()) View.VISIBLE else View.GONE
                initAdapter()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun initAdapter() {
        val adapter =
            DataPrewAdapter(false, mMusicList, { fileModel: FileModel, isSelected: Boolean ->
                if (isSelected) {
                    selectedList.add(fileModel)
                } else
                    selectedList.remove(fileModel)
                binding.selection.isSelected = selectedList.size == mMusicList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(fileModel, isSelected)

            }, { changed: List<FileModel>, isSelected: Boolean ->
                if (isSelected) selectedList.addAll(changed) else selectedList.removeAll(changed)
                binding.selection.isSelected = selectedList.size == mMusicList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(changed, isSelected)
            })
        binding.selection.setOnClickListener {
            binding.selection.isSelected = !binding.selection.isSelected
            adapter.setSelected(binding.selection.isSelected)
            binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
        }
        binding.recycleView.layoutManager = LinearLayoutManager(context)
        binding.recycleView.adapter = adapter
    }

    private fun loadMusic() {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE
        )
        val cursor: Cursor? =
            context?.contentResolver?.query(uri, projection, null, null, null)

        if (cursor != null) {
            while (cursor.moveToNext()) {

                val fileName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val filePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val fileSize =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))

                if (fileName != null && filePath != null && fileSize != null) {
                    mMusicList.add(
                        FileModel(
                            fileName,
                            filePath,
                            "Music"
                        )
                    )
                    try {
                        val fileLength = File(filePath).length()
                        if (fileLength > 0) {
                            mMusicsSize += fileLength
                        }
                    } catch (ex: Exception) {
                    }
                }
            }
            cursor.close()
        }
    }
}