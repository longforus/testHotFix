package com.longforus.testhotfix

import android.support.multidex.MultiDexApplication

class BaseApp:MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
    }
}