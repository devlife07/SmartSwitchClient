package com.vt.smart.switchapp.ui.inapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.databinding.InAppActivityBinding
import com.vt.smart.switchapp.ui.inapp.fragment.PremiumFragments

class PremiumActivity : AppCompatActivity() {
    private lateinit var binding: InAppActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InAppActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, PremiumFragments())
                .commit()
        }
    }

/*    override fun onBackPressed() {
        startMainActivity()
        super.onBackPressed()
    }
    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }*/
}
