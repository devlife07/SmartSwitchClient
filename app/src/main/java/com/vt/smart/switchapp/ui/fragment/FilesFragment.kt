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
import androidx.recyclerview.widget.LinearLayoutManager
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.databinding.FragmentFilesBinding
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.ui.adapters.DataPrewAdapter
import com.vt.smart.switchapp.ui.viewmodel.FileSelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class FilesFragment : Fragment() {

    lateinit var binding: FragmentFilesBinding
    private var mOtherFilesList: ArrayList<FileModel> = ArrayList()
    private var selectedList: ArrayList<FileModel> = ArrayList()
    private var mFilesSize: Long = 0
    val viewModel: FileSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            loadFiles()
            withContext(Dispatchers.Main)
            {
                binding.noItemFound.visibility =
                    if (mOtherFilesList.isEmpty()) View.VISIBLE else View.GONE
                binding.layoutMain.visibility =
                    if (mOtherFilesList.isNotEmpty()) View.VISIBLE else View.GONE
                initAdapter()
                binding.progressBar.visibility = View.GONE
            }
        }

    }

    private fun initAdapter() {
        val adapter = DataPrewAdapter(
            false,
            mOtherFilesList,
            { fileModel: FileModel, isSelected: Boolean ->
                if (isSelected) {
                    selectedList.add(fileModel)
                } else
                    selectedList.remove(fileModel)
                binding.selection.isSelected = selectedList.size == mOtherFilesList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(fileModel, isSelected)
            },
            { changed: List<FileModel>, isSelected: Boolean ->
                if (isSelected) selectedList.addAll(changed) else selectedList.removeAll(changed)
                binding.selection.isSelected = selectedList.size == mOtherFilesList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(changed, isSelected)
            }
        )
        binding.selection.setOnClickListener {
            binding.selection.isSelected = !binding.selection.isSelected
            adapter.setSelected(binding.selection.isSelected)
            binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
        }
        binding.recycleView.layoutManager = LinearLayoutManager(context)
        binding.recycleView.adapter = adapter
    }

    private fun loadFiles() {
        val uri: Uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE
        )
        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE)
        val selectionArgs: Array<String>? = null

        val sortOrder: String? = null // unordered

        val cursor: Cursor? =
            context?.contentResolver?.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

        if (cursor != null) {
            while (cursor.moveToNext()) {

                val fileName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val filePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                var fileSize =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))

                if (fileName != null && filePath != null && fileSize != null) {
                    if (fileName.endsWith(".pdf") ||
                        fileName.endsWith(".txt") || fileName.endsWith(".docx") ||
                        fileName.endsWith(".xlsx")
                    ) {

                        mOtherFilesList.add(
                            FileModel(
                                fileName,
                                filePath,
                                "Apps"
                            )
                        )
                        try {
                            val fileLength = File(filePath).length()

                            if (fileLength > 0) {
                                mFilesSize += fileLength
                            }

                        } catch (ex: Exception) {

                        }
                    }
                }
            }
            cursor.close()
        }
    }

}