package com.vt.smart.switchapp.ui.inapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import com.android.billingclient.api.PurchaseHistoryRecord
import com.vt.smart.switchapp.ui.inapp.model.InAppPrice
//import games.moisoni.google_iab.BillingConnector
//import games.moisoni.google_iab.BillingEventListener
//import games.moisoni.google_iab.enums.ErrorType
//import games.moisoni.google_iab.enums.ProductType
//import games.moisoni.google_iab.enums.PurchasedResult
//import games.moisoni.google_iab.enums.SupportState
//import games.moisoni.google_iab.models.BillingResponse
//import games.moisoni.google_iab.models.ProductInfo
//import games.moisoni.google_iab.models.PurchaseInfo


class PremiumViewModel : ViewModel() {
//    var billingConnector: BillingConnector? = null
    val plans = arrayListOf<InAppPrice>()
//    private val purchasedInfoList = mutableListOf<PurchaseInfo>()
//    private val fetchedProductInfoList = mutableListOf<ProductInfo>()
    private val _purchasedDone = MutableLiveData<Boolean>()
    val purchasedDone : LiveData<Boolean> get() = _purchasedDone
    val _isSubscribedd = MutableLiveData<Boolean>(false)


    var isPurchasedFirst = false


    var SKU_ITEM_MONTH_VRSN = "phone_clone_monthly" //subVersion
    var SKU_ITEM_WEEKLY_VRSN = "phone_clone_weekly" //subVersion
//    var SKU_ITEM_YEARLY_VRSN = "y_plan" //subVersion
    var SKU_ITEM_YEARLY_VRSN = "phone_clone_yearly" //subVersion
    var SKU_ITEM_ONETIME_VRSN = "lifetime_plan"//subVersion

    var LICENSE_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2XCXZ4ffS2yMhzS+nOLm3aABmh6QN8Nu1WIgwmkyFfQvFYz5n6nafdHUysjqzvp/nc1vekrt2oZIeX88H1ouPHBh4ZKYme1YXxSwDsvA3vparFSVPwy4CcigI9oOmkiY7mI9WFoB7rDn+5KX6U3oPJlZhS5FIOFHhfDtCjAMI3PhSkVwMyVQQiETMiT7hIbMmFF2Oaqn3SD+qoCHB4IF6rLIhFijKXj8RgIDv3vpo4lWPLRp3KTPommbdbxy6NeetNjNq9UbDVmBOfivyYzku/HfkAjvYqOIOyYlWXAkBQ4k9h6L+t8htzSXFTEtDtXbNrfzQPYuc2KvPrEMFSnGvQIDAQAB"


    var skuList = MutableLiveData<List<InAppPrice>>()

    var isSubscribed = MutableLiveData<Boolean>()

    var isRequested = false

    init {
        skuList.postValue(emptyList())
    }

  /*  fun initializeBillingClient(app:Context) {
        isRequested = true
        if (!isNetworkAvailable(app.applicationContext)) {
            isRequested = false
            return
        }
        //create a list with non-consumable ids
        val nonConsumableIds = mutableListOf<String>()
        nonConsumableIds.add(SKU_ITEM_ONETIME_VRSN)

        val subscriptionIds = mutableListOf<String>()
        subscriptionIds.add(SKU_ITEM_MONTH_VRSN)
        subscriptionIds.add(SKU_ITEM_YEARLY_VRSN)

        billingConnector = BillingConnector(
            app.applicationContext, LICENSE_KEY
        ).setNonConsumableIds(nonConsumableIds).setSubscriptionIds(subscriptionIds)
            .autoAcknowledge().autoConsume().enableLogging().connect()

        billingConnector?.setBillingEventListener(object : BillingEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onProductsFetched(productDetails: MutableList<ProductInfo>) {
                var product: String
                var price: String

                for (productInfo in productDetails) {
                    product = productInfo.product
                    when (product) {
                        SKU_ITEM_ONETIME_VRSN -> {
                            price = productInfo.oneTimePurchaseOfferFormattedPrice
                            Log.d("BillingConnector", "Product price: $price")
                            plans.add(InAppPrice(SKU_ITEM_ONETIME_VRSN, price, "life-time"))
                        }
                        SKU_ITEM_MONTH_VRSN -> {
                            price =
                                productInfo.productDetails.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(
                                    0
                                )?.formattedPrice.toString()
                            Log.d("BillingConnectorr", "Product price: $price")
                            plans.add(InAppPrice(SKU_ITEM_MONTH_VRSN, price, "monthly"))
                        }
                        SKU_ITEM_YEARLY_VRSN -> {
                            price =
                                productInfo.productDetails.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(
                                    0
                                )?.formattedPrice.toString()
                            Log.d("BillingConnector", "Product price: $price")
                            if (productInfo.subscriptionOfferDetails.size > 1) //Has a trial version
                            {
                                var trialPeriod = ""
                                for (pricesPhases in productInfo.subscriptionOfferDetails) {
                                    for (p in pricesPhases.pricingPhases) {
                                        if (p.formattedPrice != "Free") {
                                            price = p.formattedPrice
                                        } else {
                                            // trialPeriod = p.billingPeriod

                                            val days = Period.parse(p.billingPeriod)
                                            Log.d("period","${days}")
                                            val totalDays: Long = days.get(ChronoUnit.DAYS)
                                            trialPeriod = "$totalDays days free trial"
                                            Log.d("dd", totalDays.toString())
                                        }
                                    }

                                }
                                plans.add(
                                    InAppPrice(
                                        SKU_ITEM_YEARLY_VRSN,
                                        price,
                                        "yearly",
                                        trialPeriod = trialPeriod
                                    )
                                )
                            } else {
                                plans.add(InAppPrice(SKU_ITEM_YEARLY_VRSN, price, "yearly"))
                            }

                        }
                    }
                    skuList.postValue(plans)
                    isRequested = false
                }
            }

            override fun onPurchasedProductsFetched(
                productType: ProductType, purchases: MutableList<PurchaseInfo>
            ) {
                *//*
                * This will be called even when no purchased products are returned by the API
                * *//*

                when (productType) {
                    ProductType.INAPP -> {
                        //lifetime purchased item
                    }

                    ProductType.SUBS -> {
                        //subscription purchased items
                    }

                    ProductType.COMBINED -> {
                        //this will be triggered on activity start
                        //the other two (INAPP and SUBS) will be triggered when the user actually buys a product
                        //restore purchases
                    }

                    else -> {
                        Log.d("BillingConnector", "None of the above ProductType match")
                    }
                }

                purchases.forEach {
                    when (it.product) {
                        SKU_ITEM_ONETIME_VRSN -> {
                            Log.d("BillingConnector", "Purchased product fetched: $it")
                            // Toast.makeText(app.applicationContext,"purchased item is lifetimesss",Toast.LENGTH_SHORT).show()
                            isSubscribed.postValue(true)
                            _isSubscribedd.postValue(true)
                            //ProVersionCommented
//                            isProVersion.value = true
                           isPurchasedFirst = true

                        }
                        SKU_ITEM_MONTH_VRSN -> {
                            Log.d("BillingConnector", "Purchased product fetched: $it")
                            //  Toast.makeText(app.applicationContext,"purchased item is month version",Toast.LENGTH_SHORT).show()
                            isSubscribed.postValue(true)
                            _isSubscribedd.postValue(true)
                            //ProVersionCommented
//                            isProVersion.value = true
                        //    isPurchasedFirst = true


                        }
                        SKU_ITEM_YEARLY_VRSN -> {
                            Log.d("BillingConnector", "Purchased product fetched: $it")
                            //  Toast.makeText(app.applicationContext,"purchased item is year version",Toast.LENGTH_SHORT).show()
                            isSubscribed.postValue(true)
                            _isSubscribedd.postValue(true)
                            //ProVersionCommented
//                            isProVersion.value = true
                            isPurchasedFirst = true
                        }

                    }
                }
            }

            override fun onProductsPurchased(purchases: MutableList<PurchaseInfo>) {
                var product: String
                var purchaseToken: String

                for (purchaseInfo in purchases) {
                    product = purchaseInfo.product
                    purchaseToken = purchaseInfo.purchaseToken
                    if (product == SKU_ITEM_ONETIME_VRSN) {
                        Log.d("BillingConnector", "Product purchased: $product")
                        Toast.makeText(
                            app.applicationContext, "Subscribed", Toast.LENGTH_SHORT
                        ).show()
                        _purchasedDone.postValue(true)
                        //ProVersionCommented
//                        isProVersion.value = true
                        Log.d("BillingConnector", "Purchase token: $purchaseToken")
                        isSubscribed.postValue(true)
                        if (!purchaseInfo.isAcknowledged) {
                            billingConnector?.acknowledgePurchase(purchaseInfo)
                        }
                    }
                    if (product == SKU_ITEM_MONTH_VRSN) {
                        Log.d("BillingConnector", "Product purchased: $product")
                        Toast.makeText(
                            app.applicationContext, "Subscribed", Toast.LENGTH_SHORT
                        ).show()
                        //ProVersionCommented
//                        isProVersion.value = true
                        _purchasedDone.postValue(true)
                        Log.d("BillingConnector", "Purchase token: $purchaseToken")
                        isSubscribed.postValue(true)
                        if (!purchaseInfo.isAcknowledged) {
                            billingConnector?.acknowledgePurchase(purchaseInfo)
                        }
                    }
                    if (product == SKU_ITEM_YEARLY_VRSN) {
                        Log.d("BillingConnector", "Product purchased: $product")
                        Toast.makeText(
                            app.applicationContext, "Subscribed", Toast.LENGTH_SHORT
                        ).show()
                        _purchasedDone.postValue(true)
                        //ProVersionCommented
//                        isProVersion.value = true
                        Log.d("BillingConnector", "Purchase token: $purchaseToken")
                        isSubscribed.postValue(true)
                        if (!purchaseInfo.isAcknowledged) {
                            billingConnector?.acknowledgePurchase(purchaseInfo)
                        }
                    }
                    purchasedInfoList.add(purchaseInfo) //check "usefulPublicMethods" to see how to acknowledge or consume a purchase manually
                }
            }

            override fun onPurchaseAcknowledged(purchase: PurchaseInfo) {
                *//*
                 * Grant user entitlement for NON-CONSUMABLE products and SUBSCRIPTIONS here
                 *
                 * Even though onProductsPurchased is triggered when a purchase is successfully made
                 * there might be a problem along the way with the payment and the purchase won't be acknowledged
                 *
                 * Google will refund users purchases that aren't acknowledged in 3 days
                 *
                 * To ensure that all valid purchases are acknowledged the library will automatically
                 * check and acknowledge all unacknowledged products at the startup
                 * *//*

                when (purchase.product) {
                    SKU_ITEM_ONETIME_VRSN -> {
                        Log.d("BillingConnector", "Acknowledged: ${purchase.product}")
                     *//*   Toast.makeText(
                            app.applicationContext,
                            "Acknowledged: ${purchase.product}",
                            Toast.LENGTH_SHORT
                        ).show()*//*
                    }
                    SKU_ITEM_MONTH_VRSN -> {
                        Log.d("BillingConnector", "Acknowledged: ${purchase.product}")
                      *//*  Toast.makeText(
                            app.applicationContext,
                            "Acknowledged: ${purchase.product}",
                            Toast.LENGTH_SHORT
                        ).show()*//*
                    }
                    SKU_ITEM_YEARLY_VRSN -> {
                        Log.d("BillingConnector", "Acknowledged: ${purchase.product}")
                      *//*  Toast.makeText(
                            app.applicationContext,
                            "Acknowledged: ${purchase.product}",
                            Toast.LENGTH_SHORT
                        ).show()*//*
                    }

                }
            }

            override fun onPurchaseConsumed(purchase: PurchaseInfo) {
                *//*
                 * Grant user entitlement for CONSUMABLE products here
                 *
                 * Even though onProductsPurchased is triggered when a purchase is successfully made
                 * there might be a problem along the way with the payment and the user will be able consume the product
                 * without actually paying
                 * *//*

                when (purchase.product) {
                    SKU_ITEM_ONETIME_VRSN -> {
                        Log.d("BillingConnector", "Consumed: ${purchase.product}")

                    }
                    SKU_ITEM_MONTH_VRSN -> {
                        Log.d("BillingConnector", "Consumed: ${purchase.product}")

                    }
                    SKU_ITEM_YEARLY_VRSN -> {
                        Log.d("BillingConnector", "Consumed: ${purchase.product}")

                    }

                }
            }

            override fun onBillingError(
                billingConnector: BillingConnector, response: BillingResponse
            ) {
                when (response.errorType) {
                    ErrorType.CLIENT_NOT_READY -> {
                        //client is not ready yet
                        Toast.makeText(
                            app.applicationContext,
                            "client not ready. Try again later",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    ErrorType.CLIENT_DISCONNECTED -> {
                        // client has disconnected
//                        Toast.makeText(
//                            app.applicationContext,
//                            "client disconnected. Try again later",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }

                    ErrorType.PRODUCT_NOT_EXIST -> {
                        // product does not exist
                        Toast.makeText(
                            app.applicationContext, "Product not exist", Toast.LENGTH_SHORT
                        ).show()
                    }

                    ErrorType.CONSUME_ERROR -> {
                        //error during consumption
//                        Toast.makeText(
//                            app.applicationContext,
//                            "Error occurred. Try again later",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }

                    ErrorType.CONSUME_WARNING -> {
                        *//*
                        * This will be triggered when a consumable purchase has a PENDING state
                        * User entitlement must be granted when the state is PURCHASED
                        *
                        * PENDING transactions usually occur when users choose cash as their form of payment
                        *
                        * Here users can be informed that it may take a while until the purchase complete
                        * and to come back later to receive their purchase
                        * *//*
                        //warning during consumption
                        Toast.makeText(
                            app.applicationContext, "You have a pending request", Toast.LENGTH_SHORT
                        ).show()
                    }

                    ErrorType.ACKNOWLEDGE_ERROR -> {
                        //error during acknowledgment
                        *//*Toast.makeText(
                            app.applicationContext,
                            "error occurred, Try again later",
                            Toast.LENGTH_SHORT
                        ).show()*//*
                    }

                    ErrorType.ACKNOWLEDGE_WARNING -> {
                        *//*
                          * This will be triggered when a purchase can not be acknowledged because the state is PENDING
                          * A purchase can be acknowledged only when the state is PURCHASED
                          *
                          * PENDING transactions usually occur when users choose cash as their form of payment
                          *
                          * Here users can be informed that it may take a while until the purchase complete
                          * and to come back later to receive their purchase
                          * *//*
                        // warning during acknowledgment
                       *//* Toast.makeText(
                            app.applicationContext, "You have a pending request", Toast.LENGTH_SHORT
                        ).show()*//*
                    }

                    ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR -> {
                        //error occurred while querying purchased products
//                        Toast.makeText(
//                            app.applicationContext,
//                            "error occurred. Try again later",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }

                    ErrorType.BILLING_ERROR -> {
                        //error occurred during initialization / querying product details
                        //  Toast.makeText(app.applicationContext,"error occurred. Try again later", Toast.LENGTH_SHORT).show()
                        Log.d("BillingConnector", "Billing error")
//                           initializeBillingClient()
                    }

                    ErrorType.USER_CANCELED -> {
                        // user pressed back or canceled a dialog
                      *//*  Toast.makeText(app.applicationContext, "Cancelled", Toast.LENGTH_SHORT)
                            .show()*//*
                    }

                    ErrorType.SERVICE_UNAVAILABLE -> {
                        //network connection is down
                        Toast.makeText(
                            app.applicationContext, "no internet available", Toast.LENGTH_SHORT
                        ).show()
                    }

                    ErrorType.BILLING_UNAVAILABLE -> {
                        //billing API version is not supported for the type requested
                        Toast.makeText(app.applicationContext, "not supported", Toast.LENGTH_SHORT)
                            .show()
                    }

                    ErrorType.ITEM_UNAVAILABLE -> {
                        // requested product is not available for purchase
                        Toast.makeText(
                            app.applicationContext,
                            "requested item is not available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    ErrorType.DEVELOPER_ERROR -> {
                        //invalid arguments provided to the API
//                        Toast.makeText(
//                            app.applicationContext,
//                            "error occurred. Try again later",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }

                    ErrorType.ERROR -> {
                        //fatal error during the API action
                        Toast.makeText(app.applicationContext, "fatal error", Toast.LENGTH_SHORT)
                            .show()
                    }

                    ErrorType.ITEM_ALREADY_OWNED -> {
                        //item is already owned
                        *//*Toast.makeText(
                            app.applicationContext, "Already purchased", Toast.LENGTH_SHORT
                        ).show()*//*
                    }

                    ErrorType.ITEM_NOT_OWNED -> {
                        //failure to consume since item is not owned
                        Toast.makeText(
                            app.applicationContext, "item is not owned", Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        Log.d("BillingConnector", "None of the above ErrorType match")
//                        Toast.makeText(
//                            app.applicationContext,
//                            "error occurred. Try again later",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                }

                Log.d(
                    "BillingConnector",
                    "Error type: ${response.errorType}" + " Response code: ${response.responseCode}" + " Message: ${response.debugMessage}"
                )

                *//*  Toast.makeText(
                      app.applicationContext,
                      "Error type: ${response.errorType}" + " Response code: ${response.responseCode}"
                              + " Message: ${response.debugMessage}",
                      Toast.LENGTH_SHORT
                  ).show()*//*
            }

            override fun onHistoryChecked(message: String?, list: List<PurchaseHistoryRecord>?) {
                if (!list.isNullOrEmpty()) {
                    try {
                        for (i in list) {
                         if (i.products.toString() == "[y_plan]") {
                             Log.d("purchaseStatus","yearly found")
                            isPurchasedFirst = true
                             break
                         }else {
                          //   isPurchasedFirst = false
                             Log.d("purchaseStatus","not found")
                         }
                        }
                        Log.d("purchaseStatus","${isPurchasedFirst}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })

    }*/


    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            if (network != null) {
                val nc = connectivityManager.getNetworkCapabilities(network)
                return nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI
                )
            }
        } else {
            val networkInfos = connectivityManager.allNetworkInfo
            for (tempNetworkInfo in networkInfos) {
                if (tempNetworkInfo.isConnected) {
                    return true
                }
            }
        }
        return false
    }


//    @Suppress("unused")
/*    private fun usefulPublicMethods() {
        *//*
        * public final boolean isReady()
        *
        * Returns the state of the billing client
        * *//*
        if (billingConnector?.isReady == true) {
            Log.d("BillingConnector", "Billing client is ready")
        }

        *//*
        * public SupportState isSubscriptionSupported()
        *
        * To check device-support for subscriptions (not all devices support subscriptions)
        * *//*
        when (billingConnector?.isSubscriptionSupported) {
            SupportState.SUPPORTED -> {
                Log.d("BillingConnector", "Device subscription support: SUPPORTED")
            }

            SupportState.NOT_SUPPORTED -> {
                Log.d("BillingConnector", "Device subscription support: NOT_SUPPORTED")
            }

            SupportState.DISCONNECTED -> {
                Log.d("BillingConnector", "Device subscription support: client DISCONNECTED")
            }

            else -> {
                Log.d("BillingConnector", "None of the above SupportState match")
            }
        }

        *//*
         * public final PurchasedResult isPurchased(ProductInfo productInfo)
         *
         * To synchronously check a purchase state
         * *//*
        for (productInfo in fetchedProductInfoList) {
            when (billingConnector?.isPurchased(productInfo)) {
                PurchasedResult.YES -> {
                    Log.d("BillingConnector", "The product: ${productInfo.product} is purchased")
                }

                PurchasedResult.NO -> {
                    Log.d(
                        "BillingConnector", "The product: ${productInfo.product} is not purchased"
                    )
                }

                PurchasedResult.CLIENT_NOT_READY -> {
                    Log.d(
                        "BillingConnector",
                        "Cannot check: ${productInfo.product} because client is not ready"
                    )
                }

                PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET -> {
                    Log.d(
                        "BillingConnector",
                        "Cannot check: ${productInfo.product} because purchased products are not fetched yet"
                    )
                }

                else -> {
                    Log.d("BillingConnector", "None of the above PurchasedResult match")
                }
            }
        }

        *//*
        * public void consumePurchase(PurchaseInfo purchaseInfo)
        *
        * To consume consumable products
        * *//*
        for (purchaseInfo in purchasedInfoList) {
            billingConnector?.consumePurchase(purchaseInfo)
        }

        *//*
        * public void acknowledgePurchase(PurchaseInfo purchaseInfo)
        *
        * To acknowledge non-consumable products & subscriptions
        * *//*
        for (purchaseInfo in purchasedInfoList) {
            billingConnector?.acknowledgePurchase(purchaseInfo)
        }

        *//*
         * public final void purchase(Activity activity, String productId)
         *
         * To purchase a non-consumable/consumable product
         * *//*
        // billingConnector?.purchase(app.applicationContext, "product_id")

        *//*
         * public final void subscribe(Activity activity, String productId)
         *
         * To purchase a subscription with a base plan
         * *//*
        //     billingConnector.subscribe(this, "product_id")

        *//*
         * public final void subscribe(Activity activity, String productId, int selectedOfferIndex)
         *
         * To purchase a subscription with multiple offers
         * *//*
        //  billingConnector.subscribe(this, "product_id", 1)

        *//*
        * public final void unsubscribe(Activity activity, String productId)
        *
        * To cancel a subscription
        * *//*
        //  billingConnector.unsubscribe(this, "product_id")
    }*/
}