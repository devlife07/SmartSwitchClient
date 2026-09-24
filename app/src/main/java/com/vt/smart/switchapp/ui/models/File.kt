package com.vt.smart.switchapp.ui.models

import android.content.pm.ApplicationInfo
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

class FileModel(
    val name: String,
    val path: String,
    val type: String,
    val appInfo: ApplicationInfo? = null,
    var isSelected: Boolean = false
) {
    // identity is the file path only — isSelected is mutated after the object
    // is stored in a HashSet, so it must never affect equals()/hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileModel) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()
}

@Parcelize
data class ContactModel(
    val contactName: String,
    val contactNumber: String,
    val absolutePath: String
) : Parcelable, ListItem {
    @IgnoredOnParcel
    var isSelected = false

    override val listId: Long
        get() = contactNumber.hashCode().toLong() + javaClass.hashCode()
}

interface ListItem {
    val listId: Long
}

data class HistoryModel(
    val name: String,
    val path: String,
    val dateModified: Long
)