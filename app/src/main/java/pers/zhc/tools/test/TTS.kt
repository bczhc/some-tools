package pers.zhc.tools.test

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.TtsTestActivityBinding
import pers.zhc.tools.kangxiconverter.KangxiConverter.kangxiRadicals2normal
import pers.zhc.tools.kangxiconverter.KangxiConverterActivity

class TTS : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = TtsTestActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val tts = TextToSpeech(this, null)
        bindings.btn.setOnClickListener { _: View? ->
            tts.speak(
                kangxiRadicals2normal(bindings.et.text.toString()),
                TextToSpeech.QUEUE_ADD,
                null,
                System.currentTimeMillis().toString()
            )
            KangxiConverterActivity.markKangxiRadicalsEditText(bindings.et.editText)
        }
    }
}
