package com.vt.smart.switchapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vt.smart.switchapp.ui.models.FileModel

class FileSelectionViewModel : ViewModel() {

    // 1. Change type to List<FileModel>
    private val _currentContent = MutableLiveData<List<FileModel>>()

    val currentContent: LiveData<List<FileModel>>
        get() = _currentContent

    // 2. Use a HashSet for "selectedList" to fix the remove() crash/ANR
    // This is much faster than ArrayList for adding/removing items
    private val selectedList = HashSet<FileModel>()

    fun updateSelectedList(fileModel: FileModel, shouldAdd: Boolean) {
        if (shouldAdd) {
            selectedList.add(fileModel)
        } else {
            selectedList.remove(fileModel)
        }

        // 3. Convert to List. LiveData will now accept this.
        _currentContent.postValue(selectedList.toList())
    }

    // Batch variant for "select all" / "deselect all" so we don't rebuild
    // the list once per item, which is what caused the main-thread ANR.
    fun updateSelectedList(fileModels: List<FileModel>, shouldAdd: Boolean) {
        if (shouldAdd) {
            selectedList.addAll(fileModels)
        } else {
            selectedList.removeAll(fileModels)
        }

        _currentContent.postValue(selectedList.toList())
    }
}