package pers.zhc.tools.fdb

import android.content.Context
import android.graphics.PixelFormat.RGBA_8888
import android.os.Build
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import pers.zhc.tools.R
import pers.zhc.tools.floatingdrawing.PaintView

/**
 * @author bczhc
 */
class FdbWindow(private val context: Context) {
    private var wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var panelRL = PanelRL(context)
    private var panelLP = WindowManager.LayoutParams()

    private var paintView = PaintView(context)
    private var paintViewLP = WindowManager.LayoutParams()

    private var operationMode = OperationMode.Operating
    private var brushMode = BrushMode.Drawing

    init {
        paintViewLP.apply {
            flags = FLAG_NOT_TOUCHABLE
                .xor(FLAG_NOT_FOCUSABLE)
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("deprecation")
                TYPE_SYSTEM_ERROR
            }
            format = RGBA_8888
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        panelLP.apply {
            flags = FLAG_NOT_FOCUSABLE
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("deprecation")
                TYPE_SYSTEM_ERROR
            }
            format = RGBA_8888
            width = WRAP_CONTENT
            height = WRAP_CONTENT
        }

        panelRL.setOnButtonTouchedListener { mode, buttonIndex ->
            when (mode) {
                PanelRL.MODE_IMAGE_ICON -> {
                    panelRL.changeMode(PanelRL.MODE_PANEL)
                }
                PanelRL.MODE_PANEL -> {
                    when (buttonIndex) {
                        0 -> {
                            // back
                            panelRL.changeMode(PanelRL.MODE_IMAGE_ICON)
                        }
                        1 -> {
                            // controlling / drawing
                            val tv = panelRL.getPanelTextView(1)
                            when (operationMode) {
                                OperationMode.Operating -> {
                                    paintViewLP.flags = FLAG_NOT_FOCUSABLE
                                    wm.updateViewLayout(paintView, paintViewLP)

                                    tv.text = context.getString(R.string.fdb_panel_drawing_mode)
                                    operationMode = OperationMode.Drawing
                                }
                                OperationMode.Drawing -> {
                                    paintViewLP.flags = FLAG_NOT_TOUCHABLE
                                        .xor(FLAG_NOT_FOCUSABLE)
                                    wm.updateViewLayout(paintView, paintViewLP)

                                    tv.text = context.getString(R.string.fdb_panel_operating_mode)
                                    operationMode = OperationMode.Operating
                                }
                            }
                        }
                        2 -> {

                        }
                        else -> {
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    fun startFAB() {
        wm.addView(paintView, paintViewLP)
        wm.addView(panelRL, panelLP)
    }

    fun stopFAB() {
        wm.removeView(panelRL)
        wm.removeView(paintView)
    }

    enum class OperationMode {
        Drawing,
        Operating
    }

    enum class BrushMode {
        Drawing,
        Erasing
    }
}