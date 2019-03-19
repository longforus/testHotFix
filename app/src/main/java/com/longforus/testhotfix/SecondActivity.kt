package com.longforus.testhotfix

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import com.longforus.baselib.TestBug
import kotlinx.android.synthetic.main.activity_second.*
import org.apache.commons.io.FileUtils
import java.io.File

private val dexName = "classes2.dex"

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        btn_invoke.setOnClickListener {
            //调用含有bug的代码
            val testBug = TestBug()
            testBug.showToast(this@SecondActivity)
        }
        btn_fix.setOnClickListener {
            //进行修复操作
            val sourceFile = File(Environment.getExternalStorageDirectory(), dexName)
            println(sourceFile.absolutePath)
            val targetDir = File(getDir("odex", Context.MODE_PRIVATE).absolutePath)
            println(targetDir.absolutePath)
            //這里有点奇怪,odex目录的文件夹不是叫odex目录是 /data/user/0/com.longforus.testhotfix/app_odex
            val targetFile = File(targetDir, dexName)
            if (targetFile.exists()) {
                targetFile.delete()
                println("delete")
            }
            FileUtils.copyFileToDirectory(sourceFile, targetDir)
            println("copy finish")
            FixDexUtil.loadFixDex(this@SecondActivity)
        }
    }
}
