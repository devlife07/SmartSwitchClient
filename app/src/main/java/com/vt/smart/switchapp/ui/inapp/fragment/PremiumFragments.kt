package com.vt.smart.switchapp.ui.inapp.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
//import android.view.animation.AlphaAnimation
//import android.widget.Toast
//import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
//import com.easy.clone.R
import com.vt.smart.switchapp.databinding.ProPanelBinding
//import com.easy.clone.ui.ui.activity.MainActivity
import com.vt.smart.switchapp.ui.inapp.PremiumViewModel
//import com.easy.clone.utils.utilities.AppUtils
//import com.easy.clone.utils.utilities.GlobalValues

class PremiumFragments : Fragment() {
    var binding: ProPanelBinding? = null
    private val mViewModel: PremiumViewModel by viewModels()
    private var nullvalue: Boolean? = false
    var mActivity: FragmentActivity? = null
    private var selectedProduct: String? = "phone_clone_monthly"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ProPanelBinding.inflate(inflater, container, false)
        binding?.lifecycleOwner = viewLifecycleOwner
//        activity?.setTheme(R.style.FullScreenTheme)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let {
            binding?.monthly?.isActivated = true
//            AppUtils.firebaseUserAction("ProFragments_onViewCreated_ProFragments", "ProFragments")
//            AppUtils.proCounter -= 1
//            AppUtils.getMain(it).hideBottomBar()
//            checkPurchaseStatus()
            if (!mViewModel.isRequested) {
//                mViewModel.initializeBillingClient(requireContext())
            }
//            onBackPressedCallback.handleOnBackPressed()
//            Handler(Looper.getMainLooper()).postDelayed({
//                binding?.cross?.visibility = View.VISIBLE
//                // Apply fade-in animation
//                val fadeIn = AlphaAnimation(0f, 1f)
//                fadeIn.duration = 1000 // Animation duration (1 second)
//                binding?.cross?.startAnimation(fadeIn)
//            }, 2000)
//            transparentStausBar(it)
//            observeData()
//            handleClicks()
        }
    }

   /* private fun strikeThroughText(text : String) {
        // Remove any non-digit characters and currency symbols
        val cleanedPrice = text.replace(Regex("[^\\d.]"), "")
        // Extract the currency unit
        val currencyUnit = text.replace(Regex("[\\d.]"), "")
        // Convert the cleaned string to a double
        val annualPrice = cleanedPrice.toDoubleOrNull() ?: 0.0

        // Multiply the annual price by 12
        val olderPrice = annualPrice * 12
        val formattedOlderPrice = String.format("%.2f", olderPrice)
        val olderPriceWitUnit = "${currencyUnit} ${formattedOlderPrice}"

        try {
            val spannable = SpannableString(olderPriceWitUnit)
            spannable.setSpan(StrikethroughSpan(), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//            binding?.monthly?.text = spannable
            binding?.monthly?.text = "${text}/Month"
        }catch (e  :Exception){
            e.printStackTrace()
        }
    }

    private fun checkPurchaseStatus() {
        mViewModel.purchasedDone.observe(viewLifecycleOwner) {
            if (it) {
                try {
                    GlobalValues.isProVersion.postValue(true)
                    mActivity?.finish()
                } catch (e: Exception) {
                    //
                }
            }
        }
    }

    private fun observeData() {
        mViewModel.skuList.observe(viewLifecycleOwner) {
            Log.e("checkinApp","$it")
            if (it.isNotEmpty()) {
//                binding?.clLoader?.visibility = View.GONE
                for (item in it) {
                    if (item.key == "monthly") {
                        strikeThroughText(item.price)
                    } else if (item.key == "yearly") {
                        val amount =
                            Html.fromHtml("<b>" + item.price + "/Year" + "</b>")
                        binding?.yearly?.text = amount
                     *//*   if (!item.trialPeriod.isNullOrEmpty() && !mViewModel.isPurchasedFirst) {
                            binding?.yearly?.text = "${item.trialPeriod}, then ${amount}"
                        } else {
                        }*//*
                    } else {
//                        binding?.yearly?.text = "${item.price}/Year"
                    }
                }
            } else {
                mActivity?.let {
                    if (!mViewModel.isNetworkAvailable(it)) {
//                        binding?.clLoader?.visibility = View.GONE
                    } else {
//                        binding?.clLoader?.visibility = View.GONE
                    }

                }


            }
        }

    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Handle the back button event
            try {
                startMainActivity()
//                findNavController().popBackStack()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun startMainActivity() {
        try {
            startActivity(Intent(requireActivity(), MainActivity::class.java))
            requireActivity().finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun handleClicks() {
        binding?.cross?.setOnClickListener {
            startMainActivity()
        }
        binding?.privacy?.setOnClickListener {
            try {
                mActivity?.let {
                    AppUtils.privacy(it)
                }
            } catch (e: Exception) {
               e.printStackTrace()
            }
        }
        binding?.terms?.setOnClickListener {
            try {
                mActivity?.let {
                    AppUtils.privacy(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
           *//* binding?.weekly?.setOnClickListener {
                selectedProduct = mViewModel.SKU_ITEM_WEEKLY_VRSN
                binding?.monthly?.isActivated = false
                binding?.yearly?.isActivated = false
                mActivity?.let {
                    binding?.weekly?.setBackgroundResource(R.drawable.stroke_orange)
                    binding?.monthly?.setBackgroundResource(R.drawable.stroke_gray)
                    binding?.yearly?.setBackgroundResource(R.drawable.stroke_gray)
                    binding?.once?.setBackgroundResource(R.drawable.ic_round_black)
                    binding?.standard?.setBackgroundResource(R.drawable.ic_round_black)
                    binding?.basic?.setBackgroundResource(R.drawable.ic_round_orange)
                }
                Toast.makeText(requireActivity(), "Weekly", Toast.LENGTH_SHORT).show()
            }*//*

            binding?.monthly?.setOnClickListener {
                selectedProduct = mViewModel.SKU_ITEM_MONTH_VRSN
                binding?.weekly?.isActivated = false
                binding?.yearly?.isActivated = false
                mActivity?.let {
                    binding?.monthly?.setBackgroundResource(R.drawable.stroke_orange)
                    binding?.basic?.setBackgroundResource(R.drawable.ic_round_black)
                    binding?.once?.setBackgroundResource(R.drawable.ic_round_black)
                    binding?.yearly?.setBackgroundResource(R.drawable.stroke_gray)
                    binding?.weekly?.setBackgroundResource(R.drawable.stroke_gray)
                    binding?.standard?.setBackgroundResource(R.drawable.ic_round_orange)
//                    Toast.makeText(requireActivity(), "Monthly", Toast.LENGTH_SHORT).show()
                }
            }
            binding?.yearly?.setOnClickListener {
                selectedProduct = mViewModel.SKU_ITEM_YEARLY_VRSN
                binding?.weekly?.isActivated = false
                binding?.monthly?.isActivated = false
                mActivity?.let {
                    binding?.yearly?.setBackgroundResource(R.drawable.stroke_orange)
                    binding?.monthly?.setBackgroundResource(R.drawable.stroke_gray)
                    binding?.weekly?.setBackgroundResource(R.drawable.stroke_gray)
                    binding?.basic?.setBackgroundResource(R.drawable.ic_round_black)
                    binding?.standard?.setBackgroundResource(R.drawable.ic_round_black)
                    binding?.once?.setBackgroundResource(R.drawable.ic_round_orange)
                }
//                Toast.makeText(requireActivity(), "Yearly", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireActivity(), "Something went wrong", Toast.LENGTH_SHORT).show()
        }
        binding?.purchase?.setOnClickListener {
            Log.e("checkinApp","Click : $selectedProduct")
            if (mViewModel.isNetworkAvailable(requireContext())) {
                try {
                    mActivity?.let { activity ->
                        Log.e("checkinApp","Internet Available Click : $selectedProduct")
                        when (selectedProduct) {
                            mViewModel.SKU_ITEM_MONTH_VRSN -> {
                                mViewModel.billingConnector?.subscribe(
                                    activity,
                                    mViewModel.SKU_ITEM_MONTH_VRSN
                                )
                                Log.e("checkinApp","MonthClick : $selectedProduct")
                            }
                            mViewModel.SKU_ITEM_YEARLY_VRSN -> {
                                mViewModel.billingConnector?.subscribe(
                                    activity,
                                    mViewModel.SKU_ITEM_YEARLY_VRSN
                                )
                                Log.e("checkinApp","YearClick : $selectedProduct")
                            }
                            else -> {
                                Log.e("LOG", "invalid position in item purchasing")
                            }
                        }

                    }

                } catch (e: Exception) {
                    Toast.makeText(requireActivity(), "Please come back later", Toast.LENGTH_SHORT).show()
//                    ToastUtils.showToast(requireContext(), "Please come back later")
                }

            }
//            Log.e("checkinApp","$it")
//            Toast.makeText(requireActivity(), "$selectedProduct", Toast.LENGTH_SHORT).show()
        }

    }*/

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
        nullvalue = false
    }

}