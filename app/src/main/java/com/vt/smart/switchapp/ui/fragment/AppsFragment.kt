package com.vt.smart.switchapp.ui.fragment

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.databinding.FragmentAppBinding
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.ui.adapters.DataPrewAdapter
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.ui.viewmodel.FileSelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsFragment : Fragment() {

    lateinit var binding: FragmentAppBinding
    private var mAppsList: ArrayList<FileModel> = ArrayList()
    private var selectedList: ArrayList<FileModel> = ArrayList()
    private var mAppsSize: Long = 0
    val viewModel: FileSelectionViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            loadApps()
            withContext(Dispatchers.Main)
            {
                binding.noItemFound.visibility =
                    if (mAppsList.isEmpty()) View.VISIBLE else View.GONE
                binding.layoutMain.visibility =
                    if (mAppsList.isNotEmpty()) View.VISIBLE else View.GONE
                initAdapter()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun initAdapter() {
        val adapter =
            DataPrewAdapter(false, mAppsList, { fileModel: FileModel, isSelected: Boolean ->
                if (isSelected) {
                    selectedList.add(fileModel)
                } else
                    selectedList.remove(fileModel)
                binding.selection.isSelected = selectedList.size == mAppsList.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(fileModel, isSelected)
            }, { changed: List<FileModel>, isSelected: Boolean ->
                if (isSelected) selectedList.addAll(changed) else selectedList.removeAll(changed)
                binding.selection.isSelected = selectedList.size == mAppsList.size
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

    private fun loadApps() {
        val flags = PackageManager.GET_META_DATA or
                PackageManager.GET_SHARED_LIBRARY_FILES or
                PackageManager.GET_UNINSTALLED_PACKAGES
        val pm = context?.packageManager
        val applications = pm?.getInstalledApplications(flags)
        if (applications != null) {
            for (appInfo in applications) {
                try {
                    if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1) {
                        continue
                    } else {
                        val apkFile = context?.let { AppUtils.appFile(it, appInfo.packageName) }
                        val apkSize = apkFile?.length()

                        apkFile?.path?.let {
                            FileModel(
                                appInfo.loadLabel(pm).toString(),
                                it,
                                "Apps",
                                appInfo
                            )
                        }?.let {
                            mAppsList.add(
                                it
                            )
                        }

                        if (apkSize != null) {
                            if (apkSize > 0) {
                                mAppsSize += apkSize
                            }
                        }
                    }
                } catch (ex: PackageManager.NameNotFoundException) {
                }
            }
        }
    }
}