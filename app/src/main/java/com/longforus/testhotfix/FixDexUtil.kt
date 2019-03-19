package com.longforus.testhotfix

import android.content.Context
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import org.apache.commons.lang3.reflect.FieldUtils
import java.io.File
import java.lang.reflect.Array

object FixDexUtil {
    val loadedDex = hashSetOf<File>()
    fun loadFixDex(context: Context) {
        val dir = context.getDir("odex", Context.MODE_PRIVATE)
        loadedDex.addAll(dir.listFiles { file ->
            //添加根目录下所有不叫classes.dex的dex文件到set中
            file.name.endsWith(".dex") && file.name != "classes.dex"
        })
        createDexClassLoader(context, dir)
    }

    private fun createDexClassLoader(context: Context, dir: File?) {
        val optimizeDir = dir?.absolutePath + File.separator + "opt_dex"
        val optDir = File(optimizeDir)
        if (!optDir.exists()) {
            optDir.mkdirs()
        }
        loadedDex.forEach {
            //创建所有补丁dex的classloader
            val dexClassLoader = DexClassLoader(it.absolutePath, optimizeDir, null, context.classLoader)
            hotFix(dexClassLoader, context)
        }
    }

    private fun hotFix(dexClassLoader: DexClassLoader, context: Context) {
        val sysPathClassLoader = context.classLoader as PathClassLoader
        val myPathList = FieldUtils.getField(BaseDexClassLoader::class.java, "pathList", true).get(dexClassLoader)
        //获取补丁dex内的所有可以查找class的元素
        val myDexElements = FieldUtils.getField(myPathList.javaClass, "dexElements", true).get(myPathList)
        val sysPathList = FieldUtils.getField(BaseDexClassLoader::class.java, "pathList", true).get(sysPathClassLoader)
        val sysDexElements = FieldUtils.getField(sysPathList.javaClass, "dexElements", true).get(sysPathList)
        /*添加到系统的class查找列表的前面,都是找一个类,在前面照到了我们已经修复的没有bug的class,就不会再到后面去找有bug的class文件了,从而达到了热修复的目的.
        這里又几个疑问:
        1. 这个例子中每次都是新创建的bug类的对象,能够修复成功,说明在创建实例的时候,每次都会使用classLoader读取class,如果是生命周期比较长,创建时期比hotfix操作早的对象,应该就不能修复了吧.
        2.自己打包生成的dex体积较大,(这里没有什么内容dex就有4M,但是第一个dex只有几kb,是因为配置了multiDex-config.txt的原因么?如果是那么只有这个列表包含的class才会分配到主包当中,怎么才能反着做呢?反着做也不可能,谁知道哪些class是包含有bug的呢?)怎么把需要修复的class打成单独的dex,tinker的补丁包,只有几kB,是如何实现对比拆分的呢?
        3.在进程重启后,fix失效,需要重新进行fix操作,但是tinker可以持续有效,如果原理是完全相同的话,tinker肯定是保存了补丁dex文件,在每次启动的时候读取这些文件进行fix操作,在如2这么大的dex的情况下势必是非常耗时的,在dex小但是数量多的情况下会不会对启动性能造成明显的影响呢?
        */

        val combineArray = combineArray(myDexElements, sysDexElements)
//把含有已经修复的class的查找列表再设置回系统的classLoader
        FieldUtils.getField(sysPathList.javaClass, "dexElements", true).set(sysPathList,combineArray)

    }

    private fun combineArray(arrayLhs: Any, arrayRhs: Any): Any {
        val type = arrayLhs.javaClass.componentType
        val i = Array.getLength(arrayLhs)
        val j = i + Array.getLength(arrayRhs)
        val newInstance = Array.newInstance(type, j)
        for (a in 0 until j) {
            if (a < i) {
                Array.set(newInstance, a, Array.get(arrayLhs, a))
            } else {
                Array.set(newInstance, a, Array.get(arrayRhs, a - i))
            }
        }
        return newInstance
    }


}
