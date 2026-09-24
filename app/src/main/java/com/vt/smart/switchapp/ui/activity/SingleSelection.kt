package com.vt.smart.switchapp.ui.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.databinding.ActivitySingleSelectionBinding
import com.vt.smart.switchapp.ui.models.TransferData
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.ui.adapters.ContentStateAdapter
import com.vt.smart.switchapp.ui.fragment.AppsFragment
import com.vt.smart.switchapp.ui.fragment.ContactFragment
import com.vt.smart.switchapp.ui.fragment.FilesFragment
import com.vt.smart.switchapp.ui.fragment.ImagesFragment
import com.vt.smart.switchapp.ui.fragment.MusicFragment
import com.vt.smart.switchapp.ui.fragment.VideosFragment
import com.vt.smart.switchapp.utils.utilities.MyConstant
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.ui.viewmodel.FileSelectionViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope

class SingleSelection : AppCompatActivity() {

    // ─────────────────────────────────────────────
    // View Binding
    // ─────────────────────────────────────────────

    private val binding: ActivitySingleSelectionBinding by lazy {
        ActivitySingleSelectionBinding.inflate(layoutInflater)
    }

    // ─────────────────────────────────────────────
    // Variables
    // ─────────────────────────────────────────────

    private lateinit var sharedPreferences: SharedPreferences

    // LiveData se selected files
    private var list: List<FileModel> = emptyList()

    // ViewModel
    private val viewModel: FileSelectionViewModel by viewModels()

    // ─────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        sharedPreferences =
            getSharedPreferences(MyConstant.prefName, MODE_PRIVATE)

        initViews()
        setupViewPager()
        observeData()
        clickListeners()
    }

    // ─────────────────────────────────────────────
    // Init Views
    // ─────────────────────────────────────────────

    private fun initViews() {

        binding.textCount.text = "Selected file (0)"

        binding.btnShare.isEnabled = false
    }

    // ─────────────────────────────────────────────
    // Click Listeners
    // ─────────────────────────────────────────────

    private fun clickListeners() {

        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnShare.setOnClickListener {
            startShareData()
        }
    }

    // ─────────────────────────────────────────────
    // Observe LiveData
    // ─────────────────────────────────────────────

    private fun observeData() {

        viewModel.currentContent.observe(this) { selectedFiles ->

            list = ArrayList(selectedFiles)

            if (selectedFiles.isEmpty()) {

                binding.textCount.text = "Selected file (0)"

                binding.btnShare.isEnabled = false

            } else {

                binding.btnShare.isEnabled = true

                binding.textCount.text =
                    resources.getQuantityString(
                        R.plurals.item_count,
                        selectedFiles.size,
                        selectedFiles.size
                    )
            }
        }
    }

    // ─────────────────────────────────────────────
    // ViewPager Setup
    // ─────────────────────────────────────────────

    private fun setupViewPager() {

        val targetFragment =
            intent.getIntExtra("targetFragment", 0)

        val pagerAdapter =
            ContentStateAdapter(
                this,
                supportFragmentManager,
                lifecycle
            )

        pagerAdapter.add(
            ContentStateAdapter.PageItem(
                getString(R.string.image),
                ImagesFragment::class.java.name
            )
        )

        pagerAdapter.add(
            ContentStateAdapter.PageItem(
                getString(R.string.videos),
                VideosFragment::class.java.name
            )
        )

        pagerAdapter.add(
            ContentStateAdapter.PageItem(
                getString(R.string.app),
                AppsFragment::class.java.name
            )
        )

        pagerAdapter.add(
            ContentStateAdapter.PageItem(
                getString(R.string.music),
                MusicFragment::class.java.name
            )
        )

        pagerAdapter.add(
            ContentStateAdapter.PageItem(
                getString(R.string.files),
                FilesFragment::class.java.name
            )
        )

        pagerAdapter.add(
            ContentStateAdapter.PageItem(
                getString(R.string.contacts),
                ContactFragment::class.java.name
            )
        )

        binding.viewPager.apply {

            isUserInputEnabled = true

            adapter = pagerAdapter

            offscreenPageLimit =
                pagerAdapter.itemCount - 1
        }

        // First Mediator
        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->

            tab.text =
                pagerAdapter.getItem(position).pagetitle

        }.attach()

        // Custom Tabs
        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->

            val tabView = LayoutInflater.from(this)
                .inflate(R.layout.item_tab, null)

            val tabIcon =
                tabView.findViewById<ImageView>(R.id.tab_icon)

            val tabTitle =
                tabView.findViewById<TextView>(R.id.tab_title)

            tabTitle.text =
                pagerAdapter.getItem(position).pagetitle

            when (position) {

                0 -> tabIcon.setImageResource(R.drawable.image_selection)

                1 -> tabIcon.setImageResource(R.drawable.video_selection)

                2 -> tabIcon.setImageResource(R.drawable.apps_selection)

                3 -> tabIcon.setImageResource(R.drawable.music_selection)

                4 -> tabIcon.setImageResource(R.drawable.file_selection)

                5 -> tabIcon.setImageResource(R.drawable.contact_selection)
            }

            tab.customView = tabView

        }.attach()

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab?) {

                    tab?.customView
                        ?.findViewById<ImageView>(R.id.tab_icon)
                        ?.isSelected = true
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                    tab?.customView
                        ?.findViewById<ImageView>(R.id.tab_icon)
                        ?.isSelected = false
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )

        // Tab Margins
        for (i in 0 until binding.tabLayout.tabCount) {

            val tabView =
                (binding.tabLayout.getChildAt(0) as ViewGroup)
                    .getChildAt(i)

            val params =
                tabView.layoutParams as ViewGroup.MarginLayoutParams

            params.setMargins(24, 0, 24, 0)

            tabView.requestLayout()
        }

        binding.viewPager.setCurrentItem(targetFragment, false)
    }

    // ─────────────────────────────────────────────
    // Start Share Data
    // ─────────────────────────────────────────────

    private fun startShareData() {

        // Snapshot copy to avoid ConcurrentModificationException
        val selectedList = ArrayList(list)

        lifecycleScope.launch(Dispatchers.IO) {

            val transferList =
                ArrayList<TransferData>()

            selectedList.forEach { file ->

                transferList.add(
                    TransferData(
                        file.name,
                        file.path,
                        file.type
                    )
                )
            }

            withContext(Dispatchers.Main) {

                if (transferList.isEmpty()) {

                    AppUtils.presentToast(
                        this@SingleSelection,
                        "Select item"
                    )

                } else {

                    saveToPrefs(transferList)
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Save To Prefs
    // ─────────────────────────────────────────────

    private fun saveToPrefs(
        data: List<TransferData>
    ) {

        try {

            val gson = Gson()

            // Another safe snapshot
            val json =
                gson.toJson(ArrayList(data))

            sharedPreferences.edit {
                putString("dataList", json)
            }

            startActivity(
                Intent(
                    this,
                    ChooseConnection::class.java
                ).putExtra("user", "sender")
            )

            finish()

        } catch (e: Exception) {

            e.printStackTrace()

            AppUtils.presentToast(
                this,
                "Something went wrong"
            )
        }
    }
}