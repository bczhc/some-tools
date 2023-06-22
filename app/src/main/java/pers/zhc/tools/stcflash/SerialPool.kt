package pers.zhc.tools.stcflash

import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class SerialPool(private var port: UsbSerialPort?) {
    private val dataQueue: Queue<Byte>
    private val thread: Thread

    @Volatile
    private var run: Boolean = false


    init {
        val buf = ByteArray(300)
        dataQueue = ConcurrentLinkedQueue()
        thread = Thread {
            while (true) {
                try {
                    if (port == null) break
                    val readLen = port!!.read(buf, 0)
                    if (!run) break
                    val sb = StringBuilder("pool received: ")
                    for (i in 0 until readLen) {
                        dataQueue.add(buf[i])
                        sb.append(buf[i].toString()).append(' ')
                    }
                    println(sb.toString())
                } catch (e: IOException) {
                    if (run) {
                        throw e
                    }
                }
            }
        }
    }

    fun run() {
        run = true
        dataQueue.clear()
        if (!thread.isAlive) {
            thread.start()
        }
    }

    fun stop() {
        run = false
        if (port != null && port!!.isOpen) {
            port!!.close()
            port = null
        }
    }

    fun read(size: Int, timeout: Int): ByteArray {
        val b = ByteArray(size)
        val start = System.currentTimeMillis()
        val haveRead: Int
        while (true) {
            if (dataQueue.size >= size) {
                b.indices.forEach {
                    b[it] = dataQueue.poll()!!
                }
                haveRead = size
                break
            }
            if (System.currentTimeMillis() - start >= timeout) {
                haveRead = dataQueue.size
                for (i in 0 until haveRead) {
                    b[i] = dataQueue.poll()!!
                }
                break
            }
        }
        if (haveRead != size) return b.copyOf(haveRead)
        return b
    }

    fun flush() {
        dataQueue.clear()
    }
}
