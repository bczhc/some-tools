package pers.zhc.tools.test.viewtest

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import pers.zhc.tools.R
import java.io.IOException

fun f(x: Int): Int {
    return 2 + x
}

fun create(ac: AppCompatActivity) {
    val inputStream = ac.resources.openRawResource(R.raw.a)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    try {
        inputStream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val aView = AView(ac, bitmap)
    DoubleMoveView3(ac, bitmap)
    //        setContentView(doubleMoveView3);
    Toast.makeText(ac, f(3).toString(), Toast.LENGTH_SHORT).show()
    ac.setContentView(aView)
}

fun main() {
    println(1)
}