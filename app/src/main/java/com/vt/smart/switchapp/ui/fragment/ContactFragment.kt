package com.vt.smart.switchapp.ui.fragment

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.databinding.FragmentContactBinding
import com.vt.smart.switchapp.ui.models.ContactModel
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.ui.viewmodel.FileSelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException

class ContactFragment : Fragment() {

    lateinit var binding: FragmentContactBinding

    var list: ArrayList<ContactModel> = ArrayList()
    var selectedList: ArrayList<ContactModel> = ArrayList()
    val viewModel: FileSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val projection: Array<String> = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
                    &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
            -> {
                CoroutineScope(Dispatchers.IO).launch {
                    list = getContactList()
                    withContext(Dispatchers.Main)
                    {
                        binding.noItemFound.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                        binding.layoutMain.visibility =
                            if (list.isNotEmpty()) View.VISIBLE else View.GONE
                        initAdapter()
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_CONTACTS)
                    || shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)
            -> {
                requestPermissionLauncher.launch(
                    android.Manifest.permission.WRITE_CONTACTS
                )
                requestPermissionLauncher.launch(
                    android.Manifest.permission.READ_CONTACTS
                )
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    android.Manifest.permission.READ_CONTACTS
                )
                requestPermissionLauncher.launch(
                    android.Manifest.permission.WRITE_CONTACTS
                )
            }
        }

    }

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                CoroutineScope(Dispatchers.IO).launch {
                    list = getContactList()
                    withContext(Dispatchers.Main)
                    {
                        binding.noItemFound.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                        binding.layoutMain.visibility =
                            if (list.isNotEmpty()) View.VISIBLE else View.GONE
                        initAdapter()
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    private fun initAdapter() {
        val fileModelList: ArrayList<FileModel> = ArrayList()
        val adapter = ContactAdapter(ArrayList()) { contactModel: ContactModel, i: Int ->

            try {
                fileModelList[i].isSelected = contactModel.isSelected
                if (contactModel.isSelected) {
                    selectedList.add(contactModel)
                } else
                    selectedList.remove(contactModel)
                binding.selection.isSelected = selectedList.size == list.size
                binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
                viewModel.updateSelectedList(fileModelList[i], contactModel.isSelected)
            } catch (e: Exception) {
            }
        }
        list.forEach {
            fileModelList.add(FileModel(it.contactName, it.absolutePath, "contacts", null))
        }
        adapter.submitList(list)
        binding.selection.setOnClickListener {
            binding.selection.isSelected = !binding.selection.isSelected
            adapter.setSelected(binding.selection.isSelected)
            binding.selection.setImageResource(if (binding.selection.isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
        }
        context?.let {
            binding.recycleView.layoutManager = LinearLayoutManager(it)
        }
        binding.recycleView.adapter = adapter
    }

    private fun getContactList(): ArrayList<ContactModel> {
        val list = ArrayList<ContactModel>()
        val cr: ContentResolver? = context?.contentResolver
        try {
            val cursor = cr?.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            if (cursor != null) {
                try {
                    val mobileNoSet = HashSet<String>()
                    cursor.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val numberIndex =
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        var name: String
                        var number: String
                        while (cursor.moveToNext()) {
                            name = cursor.getString(nameIndex)
                            number = cursor.getString(numberIndex)
                            number = number.replace(" ", "")
                            if (!mobileNoSet.contains(number)) {
                                number = number.replace(" ", "").replace("*", "").replace("-", "")
                                var csvFile = getCsvFile(number)
                                if (csvFile == null)
                                    csvFile = createCsvFile(number)
                                list.add(ContactModel(name, number, "${csvFile?.absolutePath}"))

                                mobileNoSet.add(number)
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
        return list
    }

    private fun getCsvFile(filenumber: String): File? {
        val vdfDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + File.separator + ".${context?.getString(R.string.app_name)}"
        )
        if (!vdfDirectory.exists()) {
            vdfDirectory.mkdirs()
        }
        val vcfFile = File(
            vdfDirectory,
            "Num_$filenumber.vcf"
        )
        return if (vcfFile.exists())
            vcfFile
        else null

    }

    private fun createCsvFile(filenumber: String): File? {
        try {
            val vdfDirectory = File(
                context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + File.separator + ".${context?.getString(R.string.app_name)}"
            )
            if (!vdfDirectory.exists()) {
                vdfDirectory.mkdirs()
            }
            val vcfFile = File(
                vdfDirectory,
                "Num_$filenumber.vcf"
            )
            var fw: FileWriter? = null
            fw = FileWriter(vcfFile)
            fw.write("BEGIN:VCARD\r\n")
            fw.write("VERSION:3.0\r\n")
            fw.write(
                """
            FN:${filenumber}
            
            """.trimIndent()
            )

            fw.write(
                """
            TEL;TYPE=WORK,VOICE:${filenumber}
            
            """.trimIndent()
            )
            fw.write("END:VCARD\r\n")
            fw.close()
            return vcfFile
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}

class ContactAdapter(
    var list: List<ContactModel>,
    val clickListener: (contactModel: ContactModel, pos: Int) -> Unit
) :
    RecyclerView.Adapter<ContactAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_contacts,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textName.text = list[position].contactName
        holder.textNameChar.text = list[position].contactName[0].toString()
        holder.textNumber.text = list[position].contactNumber
        holder.imgSelection.setImageResource(if (list[position].isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)

    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun submitList(contactList: List<ContactModel>) {
        this.list = contactList
        notifyDataSetChanged()
    }

    fun setSelected(checked: Boolean) {
        for (index in list.indices) {
            if (list[index].isSelected == checked) continue
            list[index].isSelected = checked
            clickListener.invoke(list[index], index)
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var textName: TextView = itemView.findViewById(R.id.title)
        var textNumber: TextView = itemView.findViewById(R.id.number)
        var textNameChar: TextView = itemView.findViewById(R.id.textNameChar)
        var imgSelection: ImageView = itemView.findViewById(R.id.selection)
        var cardBg: ConstraintLayout = itemView.findViewById(R.id.cardBg)

        init {
            itemView.setOnClickListener {
                list[adapterPosition].isSelected = !list[adapterPosition].isSelected
                imgSelection.isSelected = list[adapterPosition].isSelected
                clickListener.invoke(list[adapterPosition], adapterPosition)
                imgSelection.setImageResource(if (list[adapterPosition].isSelected) R.drawable.ic_checked else R.drawable.ic_uncheck)
            }
        }
    }

}
