package com.vt.smart.switchapp.utils.utilities

import androidx.lifecycle.MutableLiveData

object GlobalValues {
    //InApp Purchases
    lateinit var countryName:String
    //adremovalCheck
    var isProVersion= MutableLiveData<Boolean>(true)
    var isFirstTime = MutableLiveData<Boolean>(true)

    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
}