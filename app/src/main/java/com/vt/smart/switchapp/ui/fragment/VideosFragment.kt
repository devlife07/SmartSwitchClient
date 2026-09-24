package com.vt.smart.switchapp.ui.fragment

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.databinding.FragmentVideosBinding
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.ui.adapters.DataPrewAdapter
import com.vt.smart.switchapp.ui.viewmodel.FileSelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class VideosFragment : Fragment() {

    lateinit var binding: FragmentVideosBinding
    private var mVideosList: ArrayList<FileModel> = ArrayList()
    private var selectedList: ArrayList<FileModel> = ArrayList()
    private var mVideosSize: Long = 0
    val viewModel: FileSelectionViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            loadVideos()
            withContext(Dispatchers.Main)
            {
                binding.noItemFound.visibility =
                    if (mVideosList.isEmpty()) View.VISIBLE else View.GONE
                binding.layoutMain.visibility =
                    if (mVideosList.isNotEmpty()) View.VISIBLE else View.GONE
                initAdapter()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun initAdapter() {
        val adapter =
            DataPrewAdapter(true, mVideosList, { fileModel: FileModel, isSelected: Boolean ->
                if (isSelected) {
                    selectedList.add(fileModel)
                } else
                    selectedList.remove(fileModel)
                binding.selection.isSelected = selectedList.size == mVideosList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(fileModel, isSelected)
            }, { changed: List<FileModel>, isSelected: Boolean ->
                if (isSelected) selectedList.addAll(changed) else selectedList.removeAll(changed)
                binding.selection.isSelected = selectedList.size == mVideosList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(changed, isSelected)
            })
        binding.selection.setOnClickListener {
            binding.selection.isSelected = !binding.selection.isSelected
            adapter.setSelected(binding.selection.isSelected)
            binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
        }
        binding.recycleView.layoutManager = GridLayoutManager(context, 3)
        binding.recycleView.adapter = adapter
    }

    private fun loadVideos() {
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE
        )
        val cursor: Cursor? =
            context?.contentResolver?.query(uri, projection, null, null, null)

        if (cursor != null) {
            while (cursor.moveToNext()) {

                val fileName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                val filePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                val fileSize =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))

                if (fileName != null && filePath != null && fileSize != null) {

                    mVideosList.add(
                        FileModel(
                            fileName,
                            filePath,
                            "Videos"
                        )
                    )
                    try {
                        val fileLength = File(filePath).length()
                        if (fileLength > 0) {
                            mVideosSize += fileLength
                        }
                    } catch (ex: Exception) {
                    }
                }
            }
            cursor.close()
        }
    }
}