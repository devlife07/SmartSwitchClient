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
import com.vt.smart.switchapp.databinding.FragmentImagesBinding
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.ui.adapters.DataPrewAdapter
import com.vt.smart.switchapp.ui.viewmodel.FileSelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class ImagesFragment : Fragment() {

    lateinit var binding: FragmentImagesBinding
    private var mImagesSize: Long = 0
    private var mPicturesList: ArrayList<FileModel> = ArrayList()
    private var selectedList: ArrayList<FileModel> = ArrayList()
    val viewModel: FileSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            loadPictures()
            withContext(Dispatchers.Main)
            {
                binding.noItemFound.visibility =
                    if (mPicturesList.isEmpty()) View.VISIBLE else View.GONE
                binding.layoutMain.visibility =
                    if (mPicturesList.isNotEmpty()) View.VISIBLE else View.GONE
                initAdapter()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun initAdapter() {
        val adapter =
            DataPrewAdapter(true, mPicturesList, { fileModel: FileModel, isSelected: Boolean ->
                if (isSelected) {
                    selectedList.add(fileModel)
                } else
                    selectedList.remove(fileModel)
                binding.selection.isSelected = selectedList.size == mPicturesList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.tick_selected else R.drawable.tick_unselected)
                viewModel.updateSelectedList(fileModel, isSelected)
            }, { changed: List<FileModel>, isSelected: Boolean ->
                if (isSelected) {
                    selectedList.addAll(changed)
                } else {
                    selectedList.removeAll(changed)
                }
                binding.selection.isSelected = selectedList.size == mPicturesList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.tick_selected else R.drawable.tick_unselected)
                viewModel.updateSelectedList(changed, isSelected)
            })
        binding.selection.setOnClickListener {
            binding.selection.isSelected = !binding.selection.isSelected
            adapter.setSelected(binding.selection.isSelected)
            binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.tick_selected else R.drawable.tick_unselected)
        }
        binding.recycleView.layoutManager = GridLayoutManager(context, 3)
        binding.recycleView.adapter = adapter
    }

    private fun loadPictures() {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE
        )
        val cursor: Cursor? =
            context?.contentResolver?.query(uri, projection, null, null, null)

        if (cursor != null) {
            while (cursor.moveToNext()) {

                val fileName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val filePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val fileSize =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))

                if (fileName != null && filePath != null && fileSize != null) {
                    mPicturesList.add(
                        FileModel(
                            fileName,
                            filePath,
                            "Images"
                        )
                    )
                    try {
                        val fileLength = File(filePath).length()
                        if (fileLength > 0) {
                            mImagesSize += fileLength
                        }

                    } catch (ex: Exception) {

                    }


                }
            }
            cursor.close()
        }
    }

}