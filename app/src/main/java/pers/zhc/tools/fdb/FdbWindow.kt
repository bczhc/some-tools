package pers.zhc.tools.fdb

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat.RGBA_8888
import android.os.Build
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import android.widget.Button
import android.widget.TextView
import pers.zhc.tools.R
import pers.zhc.tools.floatingdrawing.PaintView
import pers.zhc.tools.utils.DialogUtil
import pers.zhc.tools.views.HSVAColorPickerRL

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

    private var colorPickers = ColorPickers()

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
                            // color
                        }
                        10 -> {
                            // more
                        }
                        11 -> {
                            // exit
                            DialogUtil.createConfirmationAlertDialog(context, { _, _ ->
                                if (context is FdbMainActivity) {
                                    // also triggers `stopFAB()`
                                    context.fdbSwitch.isChecked = false
                                } else {
                                    stopFAB()
                                }
                            }, R.string.fdb_exit_confirmation_dialog, true).show()
                        }
                        else -> {
                        }
                    }
                }
                else -> {
                }
            }
        }

        colorPickers.apply {
            brush = HSVAColorPickerRL(context, Color.RED)
            panel = HSVAColorPickerRL(context, Color.WHITE)
            panelText = HSVAColorPickerRL(context, Color.parseColor("#808080"))
        }

        paintView.apply {
            drawingStrokeWidth = 10F
            eraserStrokeWidth = 10F
            drawingColor = colorPickers.brush.color
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

    class ColorPickers {
        lateinit var brush: HSVAColorPickerRL
        lateinit var panelText: HSVAColorPickerRL
        lateinit var panel: HSVAColorPickerRL
    }
}