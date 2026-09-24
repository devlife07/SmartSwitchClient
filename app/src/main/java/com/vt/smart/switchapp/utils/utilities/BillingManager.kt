package com.vt.smart.switchapp.utils.utilities

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow



object BillingManager {

    private const val TAG = "BillingManager"

    // ─── Product IDs ────────────────────────────────────────────────
    const val PRODUCT_MONTHLY = "quickshare_monthly"
    const val PRODUCT_YEARLY  = "quickshare_yearly"

    // ─── State ──────────────────────────────────────────────────────
    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> get() = _isSubscribed

    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache fetched products so SubscriptionActivity can show prices
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> get() = _products

    // ─── Init ────────────────────────────────────────────────────────
    /**
     * Call once from Application.onCreate() or MainActivity.onCreate()
     */
    fun init(context: Context) {
        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        connectAndSync()
    }

    // ─── Connection ───────────────────────────────────────────────────
    private fun connectAndSync() {
        billingClient?.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Billing connected")
                    scope.launch {
                        querySubscriptionStatus()
                        queryProductDetails()
                    }
                } else {
                    Log.e(TAG, "❌ Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing disconnected — retrying…")
                scope.launch {
                    delay(3_000)
                    connectAndSync()
                }
            }
        })
    }

    // ─── Query active subscriptions ───────────────────────────────────
    suspend fun querySubscriptionStatus() {
        val client = billingClient ?: return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        try {
            // FIX: Billing v7 ktx syntax returns PurchasesResult directly
            val result = client.queryPurchasesAsync(params)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchasesList = result.purchasesList
                val activeSub = purchasesList.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            (purchase.products.contains(PRODUCT_MONTHLY) ||
                                    purchase.products.contains(PRODUCT_YEARLY))
                }
                _isSubscribed.value = activeSub
                Log.d(TAG, "Subscription status → $activeSub")

                // Acknowledge any unacknowledged purchases
                purchasesList
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                    .forEach { acknowledgePurchase(it) }
            } else {
                Log.e(TAG, "❌ Query purchases failed: ${result.billingResult.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception querying purchases: ${e.message}")
        }
    }

    // ─── Query product details (prices) ──────────────────────────────
    suspend fun queryProductDetails() {
        val client = billingClient ?: return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        try {
            // FIX: Billing v7 ktx handles threading and returns ProductDetailsResult
            val result = client.queryProductDetails(params)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = result.productDetailsList ?: emptyList()
                Log.d(TAG, "Products fetched: ${result.productDetailsList?.map { it.productId }}")
            } else {
                Log.e(TAG, "❌ Product query failed: ${result.billingResult.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception querying product details: ${e.message}")
        }
    }

    // ─── Launch purchase flow ─────────────────────────────────────────
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails
            .subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken ?: run {
            Log.e(TAG, "No offer token found")
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient?.launchBillingFlow(activity, flowParams)
        Log.d(TAG, "Launch billing flow → ${result?.responseCode}")
    }

    // ─── Purchases listener ───────────────────────────────────────────
    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        val valid = purchase.products.any { id ->
                            id == PRODUCT_MONTHLY || id == PRODUCT_YEARLY
                        }
                        if (valid) {
                            _isSubscribed.value = true
                            scope.launch { acknowledgePurchase(purchase) }
                            Log.d(TAG, "✅ Purchase successful")
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.d(TAG, "User cancelled purchase")
            else ->
                Log.e(TAG, "Purchase error: ${result.debugMessage}")
        }
    }

    // ─── Acknowledge ──────────────────────────────────────────────────
    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        try {
            // FIX: Billing v7 ktx suspend execution structure
            val result = client.acknowledgePurchase(params)
            Log.d(TAG, "Acknowledge result → ${result.responseCode}")
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Purchase acknowledged successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception acknowledging purchase: ${e.message}")
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────
    fun destroy() {
        billingClient?.endConnection()
        scope.cancel()
    }
}