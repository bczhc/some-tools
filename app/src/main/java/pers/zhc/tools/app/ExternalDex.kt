package pers.zhc.tools.app

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dalvik.system.DexClassLoader
import pers.zhc.tools.utils.DigestUtil
import pers.zhc.tools.utils.MkdirException
import pers.zhc.tools.utils.writeTo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author bczhc
 */
class ExternalDex {
    companion object {
        private lateinit var dexFile: File
        private lateinit var dexOptimizedDir: File
        private val TAG = ExternalDex::class.java.name
        private val lock = Any()
        private var loaded: Class<*>? = null


        fun asyncFetch(context: Context, doneAction: (runner: Runner) -> Unit) {
            val packageManager = context.packageManager
            val r =
                packageManager.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, context.packageName)
            if (r != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val dexDir = File(context.filesDir, "dex")
            if (!dexDir.exists()) {
                if (!dexDir.mkdir()) {
                    throw MkdirException()
                }
            }
            dexFile = File(dexDir, "ext1.dex")
            dexOptimizedDir = File(dexDir, "optimized")
            if (!dexOptimizedDir.exists()) {
                if (!dexOptimizedDir.mkdir()) {
                    throw MkdirException()
                }
            }

            val loadAndRun = {
                synchronized(lock) {
                    if (loaded == null) {
                        loaded = load()
                    }
                }
                doneAction(Runner(loaded!!))
            }

            Thread {
                val fetchedDigest: String?
                try {
                    fetchedDigest = fetchDigest()
                } catch (e: IOException) {
                    e.printStackTrace()
                    return@Thread
                }

                fetchedDigest ?: return@Thread
                if (dexFile.exists() && checkLocalDigest(fetchedDigest)) {
                    loadAndRun()
                } else {
                    var check = false
                    var trails = 0
                    while (true) {
                        try {
                            check = downloadDexAndCheck(fetchedDigest)
                            break
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.d(TAG, "main: retry... $trails")
                            if (trails == 3) {
                                Log.d(TAG, "main: stop retrying")
                                break
                            }
                            ++trails
                        }
                    }
                    if (check && checkLocalDigest(fetchedDigest)) {
                        loadAndRun()
                    }
                }
            }.start()
        }

        private fun downloadDex() {
            val out = FileOutputStream(dexFile)

            val dexURL = URL("https://gitlab.com/bczhc/store/-/raw/master/extDex/ext1.dex")
            val inputStream = dexURL.openStream()
            inputStream.writeTo(out)
            out.close()
            inputStream.close()
        }

        private fun downloadDexAndCheck(sha1: String): Boolean {
            downloadDex()
            return checkLocalDigest(sha1)
        }

        private fun fetchDigest(): String? {
            val sha1URL = URL("https://gitlab.com/bczhc/store/-/raw/master/extDex/ext1.sha1")
            val connection = sha1URL.openConnection() as HttpURLConnection
            if (connection.responseCode != 200) {
                return null
            }
            val inputStream = connection.inputStream
            val sha1 = inputStream.reader().readLines()[0]
            inputStream.close()

            if (sha1.length != 40) {
                return null
            }
            return sha1
        }

        private fun checkLocalDigest(sha1: String): Boolean {
            val compute = DigestUtil.getFileDigestString(dexFile, "SHA1")
            return compute == sha1
        }

        private fun load(): Class<*> {
            val dexClassLoader =
                DexClassLoader(dexFile.path, dexOptimizedDir.path, null, {}::class.java.classLoader)
            return dexClassLoader.loadClass("pers.zhc.tools.External1")!!
        }

        class Runner(private val clazz: Class<*>) {
            fun run(context: Context) {
                val method = clazz.getMethod("run", Context::class.java)
                method.invoke(null, context)
            }
        }
    }
}