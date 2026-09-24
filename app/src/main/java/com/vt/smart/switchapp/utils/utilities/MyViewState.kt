package com.vt.smart.switchapp.utils.utilities

sealed class MyViewState {

    object Idle : MyViewState()

    object Success : MyViewState()

    class Progress(val progress: Int) : MyViewState()

    class Failed(val throwable: Throwable) : MyViewState()

}