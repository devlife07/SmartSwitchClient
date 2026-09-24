package com.vt.smart.switchapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vt.smart.switchapp.R


import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.vt.smart.switchapp.utils.utilities.BillingManager
import kotlinx.coroutines.launch

class SubscriptionActivity : AppCompatActivity() {

    // ── Views (bind with ViewBinding or findViewById) ─────────────────
    private lateinit var btnClose: ImageView
    private lateinit var btnMonthly: LinearLayout
    private lateinit var btnYearly: LinearLayout
    private lateinit var btnSubscribe: Button
    private lateinit var tvMonthlyPrice: TextView
    private lateinit var tvYearlyPrice: TextView
    private lateinit var tvYearlySaving: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var radioMonthly: RadioButton
    private lateinit var radioYearly: RadioButton

    private var selectedProduct: ProductDetails? = null

    private var isFromSplash=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        isFromSplash=intent.getBooleanExtra("isFromSplash",false)
        bindViews()
        setupListeners()
        observeProducts()
    }

    private fun bindViews() {
        btnClose      = findViewById(R.id.btnClose)
        btnMonthly    = findViewById(R.id.layoutMonthly)
        btnYearly     = findViewById(R.id.layoutYearly)
        btnSubscribe  = findViewById(R.id.btnSubscribe)
        tvMonthlyPrice = findViewById(R.id.tvMonthlyPrice)
        tvYearlyPrice  = findViewById(R.id.tvYearlyPrice)
        tvYearlySaving = findViewById(R.id.tvYearlySaving)
        progressBar    = findViewById(R.id.progressBar)
        radioMonthly   = findViewById(R.id.radioMonthly)
        radioYearly    = findViewById(R.id.radioYearly)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener {

            if (isFromSplash){
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()


        }

        btnMonthly.setOnClickListener { selectPlan(isYearly = false) }
        btnYearly.setOnClickListener  { selectPlan(isYearly = true) }

        btnSubscribe.setOnClickListener {
            selectedProduct?.let { product ->
                BillingManager.launchPurchaseFlow(this, product)
            } ?: Toast.makeText(this, "Plan select a plan", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Observe products from BillingManager ─────────────────────────
    private fun observeProducts() {
        progressBar.visibility = View.VISIBLE
        btnSubscribe.isEnabled = false

        lifecycleScope.launch {
            BillingManager.products.collect { list ->
                if (list.isEmpty()) return@collect

                progressBar.visibility = View.GONE
                btnSubscribe.isEnabled = true

                list.forEach { product ->
                    val priceStr = product.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.firstOrNull()
                        ?.formattedPrice ?: "—"

                    when (product.productId) {
                        BillingManager.PRODUCT_MONTHLY -> {
                            tvMonthlyPrice.text = "$priceStr / month"
                        }
                        BillingManager.PRODUCT_YEARLY -> {
                            tvYearlyPrice.text = "$priceStr / year"
                            // Show saving badge if you have monthly price too
                            tvYearlySaving.visibility = View.VISIBLE
                            tvYearlySaving.text = "Save 40%"
                        }
                    }
                }

                // Default select yearly
                val yearly = list.find { it.productId == BillingManager.PRODUCT_YEARLY }
                yearly?.let { selectProduct(it, isYearly = true) }
            }
        }
    }

    private fun selectPlan(isYearly: Boolean) {
        val product = BillingManager.products.value.find {
            it.productId == if (isYearly) BillingManager.PRODUCT_YEARLY else BillingManager.PRODUCT_MONTHLY
        }
        product?.let { selectProduct(it, isYearly) }
    }

    private fun selectProduct(product: ProductDetails, isYearly: Boolean) {
        selectedProduct = product

        // Highlight selected card
        btnYearly.isSelected  = isYearly
        btnMonthly.isSelected = !isYearly
        radioYearly.isChecked  = isYearly
        radioMonthly.isChecked = !isYearly
    }
}