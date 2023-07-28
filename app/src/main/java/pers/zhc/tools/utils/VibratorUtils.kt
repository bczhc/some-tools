package pers.zhc.tools.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import pers.zhc.tools.MyApplication

fun defaultVibrator(): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MyApplication.appContext.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        MyApplication.appContext.getSystemService(Context.VIBRATOR_SERVICE)!! as Vibrator
    }
}

fun Vibrator.oneShotVibrate(millis: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        this.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        this.vibrate(millis)
    }
}
