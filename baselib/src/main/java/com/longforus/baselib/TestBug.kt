package com.longforus.baselib

import android.content.Context
import android.widget.Toast

class TestBug {
    fun showToast(context: Context) {
        var i = 10
        var j = 1
        Toast.makeText(context,"result=${i/j}",Toast.LENGTH_SHORT).show()
    }
}