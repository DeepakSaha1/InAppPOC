package com.example.inapppoc

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    // interface for communication between the library and user application code
    private lateinit var billingClient: BillingClient
    private val skuList = listOf("test_product_one", "test_product_two")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpBillingClient()
    }

    private fun setUpBillingClient() {
        billingClient = BillingClient.newBuilder(applicationContext)
                .enablePendingPurchases()
                .setListener(purchaseUpdateListener)
                .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.v("TAG_IN_APP", "Setup Billing Done")

                    // The BillingClient is ready. You can query purchases here.
                    queryAvailableProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.v("TAG_IN_APP", "Billing client Disconnected")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }

        })
    }

    private fun queryAvailableProducts() {
        if (billingClient.isReady) {
            val params = SkuDetailsParams
                    .newBuilder()
                    .setSkusList(skuList)
                    .setType(BillingClient.SkuType.INAPP)
                    .build()

            billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList?.isNullOrEmpty() == true) {
                    for (skuDetails in skuDetailsList) {
                        Log.v("TAG_IN_APP", "skuDetailsList : $skuDetailsList")
                        //this will return both the SKUs from Google Play Console
//                        updateUI(skuDetails)
                    }
                    updateUI(skuDetailsList)
                }

            }
        }
    }

    private fun updateUI(skuDetails: List<SkuDetails?>) {
        skuDetails?.let {
//            txt_product_name?.text = skuDetails.title
//            txt_product_description?.text = skuDetails.description
            showUIElements(skuDetails)
        }
    }

    private fun showUIElements(skuDetails: List<SkuDetails?>) {
        txt_product_name?.visibility = View.VISIBLE
        txt_product_description?.visibility = View.VISIBLE
        txt_product_buy?.visibility = View.VISIBLE

        txt_product_buy?.setOnClickListener {
            skuDetails[0]?.let { skuDetail ->
                val billingFlowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetail)
                        .build()
                billingClient.launchBillingFlow(this, billingFlowParams).responseCode
            }
        }
    }

    private val purchaseUpdateListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                Log.v("TAG_INAPP","billingResult responseCode : ${billingResult.responseCode}")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
//                        handleNonConcumablePurchase(purchase)
                        handleConsumedPurchases(purchase)
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    // Handle an error caused by a user cancelling the purchase flow.
                } else {
                    // Handle any other error codes.
                }
            }

    private fun handleConsumedPurchases(purchase: Purchase) {
        Log.d("TAG_INAPP", "handleConsumablePurchasesAsync foreach it is $purchase")
        val params =
                ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient?.consumeAsync(params) { billingResult, purchaseToken ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    // Update the appropriate tables/databases to grant user the items
                    Log.d(
                            "TAG_INAPP",
                            " Update the appropriate tables/databases to grant user the items"
                    )
                }
                else -> {
                    Log.w("TAG_INAPP", billingResult.debugMessage)
                }
            }
        }
    }

    private fun handleNonConcumablePurchase(purchase: Purchase) {
        Log.v("TAG_INAPP","handlePurchase : ${purchase}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    val billingResponseCode = billingResult.responseCode
                    val billingDebugMessage = billingResult.debugMessage

                    Log.v("TAG_INAPP","response code: $billingResponseCode")
                    Log.v("TAG_INAPP","debugMessage : $billingDebugMessage")

                }
            }
        }
    }

}