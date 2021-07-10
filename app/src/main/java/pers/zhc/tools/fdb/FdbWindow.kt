package pers.zhc.tools.fdb

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PixelFormat.RGBA_8888
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.fdb_panel_settings_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.floatingdrawing.PaintView
import pers.zhc.tools.utils.ColorUtils
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

    private val colorPickers = ColorPickers()
    private val dialogs = Dialogs()

    private var followBrushColor = false
    private var invertTextColor = false

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
                            dialogs.brushColorPicker.show()
                        }
                        3 -> {
                            // stroke width

                        }
                        4 -> {
                            // undo
                            paintView.undo()
                        }
                        5 -> {
                            // redo
                            paintView.redo()
                        }
                        6 -> {
                            // drawing / erasing
                            val tv = panelRL.getPanelTextView(6)
                            when (brushMode) {
                                BrushMode.Drawing -> {
                                    brushMode = BrushMode.Erasing
                                    tv.text = context.getString(R.string.fdb_panel_erasing_mode)
                                    paintView.isEraserMode = true
                                }
                                BrushMode.Erasing -> {
                                    brushMode = BrushMode.Drawing
                                    tv.text = context.getString(R.string.fdb_panel_drawing_mode)
                                    paintView.isEraserMode = false
                                }
                            }
                        }
                        7 -> {
                            // clear
                            createConfirmationDialog({ _, _ ->
                                paintView.clearAll()
                            }, R.string.fdb_clear_confirmation_dialog).show()
                        }
                        8 -> {
                            // pick color
                            TODO()
                        }
                        9 -> {
                            // panel
                            dialogs.panelSettings.show()
                        }
                        10 -> {
                            // more
                            dialogs.moreMenu.show()
                        }
                        11 -> {
                            // exit
                            createConfirmationDialog({ _, _ ->
                                if (context is FdbMainActivity) {
                                    // also triggers `stopFAB()`
                                    context.fdbSwitch.isChecked = false
                                } else {
                                    stopFAB()
                                }
                            }, R.string.fdb_exit_confirmation_dialog).show()
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

            brush.setOnColorPickedInterface { _, _, color ->
                updateBrushColor(color)
            }

            panel.setOnColorPickedInterface { _, _, color ->
                updatePanelColor(color)
            }

            panelText.setOnColorPickedInterface { _, _, color ->
                updatePanelTextColor(color)
            }
        }

        dialogs.apply {
            brushColorPicker = createDialog(colorPickers.brush, true)
            panelColorPicker = createDialog(colorPickers.panel, true)
            panelTextColorPicker = createDialog(colorPickers.panelText, true)
            panelSettings = createPanelSettingsDialog()
            moreMenu = createMoreOptionDialog()
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

    private enum class OperationMode {
        Drawing,
        Operating
    }

    private enum class BrushMode {
        Drawing,
        Erasing
    }

    private class ColorPickers {
        lateinit var brush: HSVAColorPickerRL
        lateinit var panelText: HSVAColorPickerRL
        lateinit var panel: HSVAColorPickerRL
    }

    private class Dialogs {
        lateinit var brushColorPicker: Dialog
        lateinit var panelTextColorPicker: Dialog
        lateinit var panelColorPicker: Dialog
        lateinit var panelSettings: Dialog
        lateinit var moreMenu: Dialog
    }

    private fun createConfirmationDialog(
        positiveAction: DialogInterface.OnClickListener,
        @StringRes titleRes: Int
    ): AlertDialog {
        return DialogUtil.createConfirmationAlertDialog(context, positiveAction, titleRes, true)
    }

    private fun createDialog(view: View, transparent: Boolean = false): Dialog {
        val dialog = Dialog(context)
        DialogUtil.setDialogAttr(dialog, transparent, true)
        dialog.setContentView(view)
        return dialog
    }

    private fun updateBrushColor(color: Int) {
        paintView.drawingColor = color
        if (followBrushColor) {
            updatePanelColor(color)
        }
    }

    private fun updatePanelColor(color: Int) {
        panelRL.setPanelColor(color)
        if (invertTextColor) {
            updatePanelTextColor(ColorUtils.invertColor(color))
        }
    }

    private fun updatePanelTextColor(color: Int) {
        panelRL.setPanelTextColor(color)
    }

    private fun createPanelSettingsDialog(): Dialog {
        val inflate = View.inflate(context, R.layout.fdb_panel_settings_view, null)
        val dialog = createDialog(inflate)

        val panelColorBtn = inflate.panel_color!!
        val textColorBtn = inflate.text_color!!
        val followBrushColorSwitch = inflate.follow_painting_color!!
        val invertTextColorSwitch = inflate.invert_text_color!!

        panelColorBtn.setOnClickListener {
            dialogs.panelColorPicker.show()
        }
        textColorBtn.setOnClickListener {
            dialogs.panelTextColorPicker.show()
        }
        followBrushColorSwitch.setOnCheckedChangeListener { _, isChecked ->
            followBrushColor = isChecked
            panelColorBtn.isEnabled = !isChecked

            if (isChecked) {
                updatePanelColor(paintView.drawingColor)
            } else {
                updatePanelColor(colorPickers.panel.color)
            }
        }
        invertTextColorSwitch.setOnCheckedChangeListener { _, isChecked ->
            invertTextColor = isChecked
            textColorBtn.isEnabled = !isChecked

            if (isChecked) {
                updatePanelTextColor(ColorUtils.invertColor(panelRL.getPanelColor()))
            } else {
                updatePanelTextColor(colorPickers.panelText.color)
            }
        }

        return dialog
    }

    private fun createMoreOptionDialog(): Dialog {
        val onClickActions: ((index: Int) -> View.OnClickListener) = { index ->
            View.OnClickListener {
                when (index) {
                    0 -> {
                        // import image
                    }
                    1 -> {
                        // export image
                    }
                    2 -> {
                        // import path
                    }
                    3 -> {
                        // export path
                    }
                    4 -> {
                        // reset transformation
                    }
                    5 -> {
                        // manage layers
                    }
                    6 -> {
                        // hide drawing board
                    }
                    7 -> {
                        // drawing statistics
                    }
                    else -> {
                    }
                }
                TODO()
            }
        }

        val inflate = View.inflate(context, R.layout.fdb_panel_more_view, null)
        val ll = inflate.ll!!

        val btnStrings = context.resources.getStringArray(R.array.fdb_more_menu)
        btnStrings.forEachIndexed { i, btnString ->
            val button = MaterialButton(context)
            button.text = btnString
            button.setOnClickListener(onClickActions(i))

            ll.addView(button)
        }
        return createDialog(inflate)
    }
}