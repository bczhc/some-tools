package pers.zhc.tools.fdb

import android.app.Dialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat.RGBA_8888
import android.os.Build
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.fdb_eraser_opacity_adjusting_view.view.*
import kotlinx.android.synthetic.main.fdb_panel_settings_view.view.*
import kotlinx.android.synthetic.main.fdb_panel_settings_view.view.ll
import kotlinx.android.synthetic.main.fdb_stoke_settings_view.view.*
import kotlinx.android.synthetic.main.fdb_transformation_settings_view.view.*
import kotlinx.android.synthetic.main.progress_bar.view.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePickerRL
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener.ViewDimension
import pers.zhc.tools.floatingdrawing.PaintView
import pers.zhc.tools.floatingdrawing.PaintView.PathVersion
import pers.zhc.tools.utils.*
import pers.zhc.tools.views.HSVAColorPickerRL
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

/**
 * @author bczhc
 */
class FdbWindow(private val context: BaseActivity) {
    private val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val panelRL = PanelRL(context)
    private val panelSV = ScrollView(context)
    private val panelLP = WindowManager.LayoutParams()

    private val paintView = PaintView(context)
    private val paintViewLP = WindowManager.LayoutParams()

    private var operationMode = OperationMode.OPERATING
    private var brushMode = BrushMode.DRAWING

    private val colorPickers = ColorPickers()
    private val dialogs = Dialogs()

    private var followBrushColor = false
    private var invertTextColor = false
    private val timestamp = System.currentTimeMillis()
    private val pathFiles = object {
        val tmpPathDir = File(context.filesDir, "path")
        val tmpPathFile = File(tmpPathDir, "$timestamp.path")
    }

    // TODO: 7/29/21 rewrite position updater
    private val panelDimension = ViewDimension()
    private val positionUpdater = FloatingViewOnTouchListener(panelLP, wm, panelSV, 0, 0, panelDimension)

    private val pathSaver = PathSaver(pathFiles.tmpPathFile.path)

    private val receivers = object {
        lateinit var main: FdbBroadcastReceiver
        var startScreenColorPicker: StartScreenColorPickerReceiver? = null
        var screenColorPickerResult: ScreenColorPickerResultReceiver? = null
    }

    private var layerManagerView: LayerManagerView

    private var hasStartedScreenColorPicker = false

    init {
        val ll = LinearLayout(context)
        ll.addView(panelRL)
        panelSV.addView(ll)

        if (!pathFiles.tmpPathDir.exists() && !pathFiles.tmpPathDir.mkdirs()) {
            ToastUtils.show(context, R.string.mkdir_failed)
        }

        val floatingWindowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("deprecation")
            TYPE_SYSTEM_ERROR
        }

        paintViewLP.apply {
            flags = FLAG_NOT_TOUCHABLE
                .xor(FLAG_NOT_FOCUSABLE)
            type = floatingWindowType
            format = RGBA_8888
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        panelLP.apply {
            flags = FLAG_NOT_FOCUSABLE
            type = floatingWindowType
            format = RGBA_8888
            width = WRAP_CONTENT
            height = WRAP_CONTENT
        }

        panelRL.apply {
            setOnButtonClickedListener { mode, buttonIndex ->
                when (mode) {
                    PanelRL.MODE_IMAGE_ICON -> {
                        panelRL.changeMode(PanelRL.MODE_PANEL)
                        updatePanelDimension()
                    }
                    PanelRL.MODE_PANEL -> {
                        when (buttonIndex) {
                            0 -> {
                                // back
                                panelRL.changeMode(PanelRL.MODE_IMAGE_ICON)
                                updatePanelDimension()
                            }
                            1 -> {
                                // controlling / drawing
                                val tv = panelRL.getPanelTextView(1)
                                when (operationMode) {
                                    OperationMode.OPERATING -> {
                                        paintViewLP.flags = FLAG_NOT_FOCUSABLE
                                        wm.updateViewLayout(paintView, paintViewLP)

                                        tv.text = context.getString(R.string.fdb_panel_drawing_mode)
                                        operationMode = OperationMode.DRAWING
                                    }
                                    OperationMode.DRAWING -> {
                                        paintViewLP.flags = FLAG_NOT_TOUCHABLE
                                            .xor(FLAG_NOT_FOCUSABLE)
                                        wm.updateViewLayout(paintView, paintViewLP)

                                        tv.text = context.getString(R.string.fdb_panel_operating_mode)
                                        operationMode = OperationMode.OPERATING
                                    }
                                }
                            }
                            2 -> {
                                // color
                                if (paintView.isEraserMode) {
                                    dialogs.eraserOpacity.show()
                                } else {
                                    dialogs.brushColorPicker.show()
                                }
                            }
                            3 -> {
                                // stroke width
                                showBrushWidthAdjustingDialog()
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
                                    BrushMode.DRAWING -> {
                                        brushMode = BrushMode.ERASING
                                        tv.text = context.getString(R.string.fdb_panel_erasing_mode)
                                        paintView.isEraserMode = true
                                    }
                                    BrushMode.ERASING -> {
                                        brushMode = BrushMode.DRAWING
                                        tv.text = context.getString(R.string.fdb_panel_drawing_mode)
                                        paintView.isEraserMode = false
                                    }
                                    else -> {
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
                                // pick screen color
                                pickScreenColorAction()
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
                                    exit()
                                }, R.string.fdb_exit_confirmation_dialog).show()
                            }
                            else -> {
                            }
                        }
                        paintView.flushPathSaver()
                    }
                    else -> {
                    }
                }
            }

            @Suppress("ClickableViewAccessibility")
            setOnTouchListener { _, event ->
                return@setOnTouchListener positionUpdater.onTouch(panelSV, event, false)
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

        paintView.apply {
            drawingStrokeWidth = 10F
            eraserStrokeWidth = 10F
            drawingColor = colorPickers.brush.color

            setPathSaver(pathSaver)

            layerManagerView = LayerManagerView(context) { id ->
                paintView.add1Layer(id)
            }

            setOnScreenDimensionChangedListener { width, height ->
                positionUpdater.updateParentDimension(width, height)
            }
            post {
                // add the default layer
                val id = System.currentTimeMillis()
                paintView.add1Layer(id)
                paintView.switchLayer(id)
                layerManagerView.add1Layer(LayerInfo(id, context.getString(R.string.fdb_layer_default_name), true))
                layerManagerView.setChecked(id)
            }

            setOnImportLayerAddedListener {
                layerManagerView.add1Layer(it)
            }
        }

        dialogs.apply {
            brushColorPicker = createDialog(colorPickers.brush, true, dim = false)
            panelColorPicker = createDialog(colorPickers.panel, true, dim = false)
            panelTextColorPicker = createDialog(colorPickers.panelText, true, dim = false)
            panelSettings = createPanelSettingsDialog()
            moreMenu = createMoreOptionDialog()
            eraserOpacity = createEraserOpacityDialog()
            transformationSettings = createTransformationSettingsDialog()
            layerManager = createLayerManagerDialog()
        }

        val externalStorage = Common.getExternalStoragePath(context)
        val parent = File(externalStorage, "DrawingBoard")
        externalPath.path = File(parent, "path")
        externalPath.image = File(parent, "image")
        if ((!externalPath.path.exists() && !externalPath.path.mkdirs()) ||
            (!externalPath.image.exists() && !externalPath.image.mkdirs())
        ) {
            ToastUtils.show(context, R.string.mkdir_failed)
        }
        // TODO: 7/11/21 handle storage permission request

        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        positionUpdater.updateParentDimension(screenWidth, screenHeight)

        receivers.main = FdbBroadcastReceiver(this)
        val filter = IntentFilter()
        filter.apply {
            addAction(FdbBroadcastReceiver.ACTION_FDB_SHOW)
            addAction(FdbBroadcastReceiver.ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED)
            addAction(FdbBroadcastReceiver.ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED)
            addAction(FdbBroadcastReceiver.ACTION_ON_SCREEN_ORIENTATION_CHANGED)
        }
        context.applicationContext.registerReceiver(receivers.main, filter)
    }

    private fun showBrushWidthAdjustingDialog() {
        val dialog = createDialog(createStrokeSettingsView(), width = MATCH_PARENT)
        dialog.show()
    }

    private fun createStrokeSettingsView(): View {
        val inflate = View.inflate(context, R.layout.fdb_stoke_settings_view, null)!!.rootView as LinearLayout

        val rg = inflate.rg!!
        val widthSlider = inflate.slider!!
        val infoTV = inflate.tv!!
        val lockBrushCB = inflate.cb!!
        val hardnessSlider = inflate.hardness_slider!!
        val strokeShowView = inflate.stroke_show!!

        lockBrushCB.isChecked = paintView.isLockStrokeEnabled
        strokeShowView.setColor(paintView.drawingColor)
        strokeShowView.setDiameter(paintView.strokeWidthInUse)
        rg.check(
            if (paintView.isEraserMode) {
                R.id.eraser_radio
            } else {
                R.id.brush_radio
            }
        )

        // for non-linear width adjusting
        val base = 1.07
        val updateDisplay = { mode: BrushMode ->
            val width = when (mode) {
                BrushMode.DRAWING -> paintView.drawingStrokeWidth
                BrushMode.ERASING -> paintView.eraserStrokeWidth
                BrushMode.IN_USE -> paintView.strokeWidthInUse
            }
            strokeShowView.setDiameter(width * paintView.scale)
            widthSlider.value = (ln(width.toDouble()) / ln(base)).toFloat()
            strokeShowView.setDiameter(width * paintView.scale)
            infoTV.text = context.getString(
                R.string.fdb_stroke_width_info,
                width,
                paintView.scale * width,
                paintView.scale * 100F
            )
        }

        updateDisplay(BrushMode.IN_USE)

        val updateWidthAndDisplay = { width: Float ->
            when (rg.checkedRadioButtonId) {
                R.id.brush_radio -> {
                    paintView.drawingStrokeWidth = width
                    updateDisplay(BrushMode.DRAWING)
                }
                R.id.eraser_radio -> {
                    paintView.eraserStrokeWidth = width
                    updateDisplay(BrushMode.ERASING)
                }
                else -> {
                }
            }
            if (paintView.isLockStrokeEnabled) {
                paintView.updateLockedStrokeWidth()
            }
        }

        infoTV.setOnClickListener {
            createPromptDialog(R.string.fdb_stroke_width_prompt_dialog) { _, et ->
                val px = et.text.toString().toFloatOrNull()
                if (px == null) {
                    ToastUtils.show(context, R.string.please_enter_correct_value_toast)
                    return@createPromptDialog
                }
                updateWidthAndDisplay(px)
            }.show()
        }

        widthSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val width = base.pow(value.toDouble()).toFloat()
                updateWidthAndDisplay(width)
            }
        }

        rg.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.brush_radio -> updateDisplay(BrushMode.DRAWING)
                R.id.eraser_radio -> updateDisplay(BrushMode.ERASING)
                else -> {
                }
            }
        }

        lockBrushCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isLockStrokeEnabled = isChecked
        }

        hardnessSlider.value = (1F - paintView.blurRadius * 2F / paintView.drawingStrokeWidth) * 100F
        hardnessSlider.addOnChangeListener { slider, value, fromUser ->
            if (!fromUser) {
                return@addOnChangeListener
            }
            val blurRadius = (1F - value / 100F) * paintView.drawingStrokeWidth / 2F
            paintView.blurRadius = blurRadius
        }

        return inflate
    }

    fun startFDB() {
        wm.addView(paintView, paintViewLP)
        wm.addView(panelSV, panelLP)
    }

    private fun stopFDB() {
        wm.removeView(panelSV)
        wm.removeView(paintView)
    }

    private enum class OperationMode {
        DRAWING,
        OPERATING
    }

    private enum class BrushMode {
        DRAWING,
        ERASING,
        IN_USE
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
        lateinit var eraserOpacity: Dialog
        lateinit var transformationSettings: Dialog
        lateinit var layerManager: Dialog
    }

    private fun createConfirmationDialog(
        positiveAction: DialogInterface.OnClickListener,
        @StringRes titleRes: Int
    ): AlertDialog {
        return DialogUtil.createConfirmationAlertDialog(context, positiveAction, titleRes, true)
    }

    private fun createDialog(
        view: View,
        transparent: Boolean = false,
        dim: Boolean = true,
        width: Int = WRAP_CONTENT,
        height: Int = WRAP_CONTENT
    ): Dialog {
        return Dialog(context).apply {
            setContentView(view)
        }.also {
            DialogUtils.setDialogAttr(it, transparent, width, height, true)
            if (!dim) {
                it.window?.clearFlags(FLAG_DIM_BEHIND)
            }
        }
    }

    private fun createPromptDialog(@StringRes titleRes: Int, callback: PromptDialogCallback): AlertDialog {
        val dialog = DialogUtils.createPromptDialog(context, titleRes, callback)
        DialogUtils.setDialogAttr(dialog, isTransparent = false, overlayWindow = true)
        return dialog
    }

    private fun createFilePickerDialog(
        type: Int = FilePickerRL.TYPE_PICK_FILE,
        initialPath: File,
        enableFilenameET: Boolean = false,
        onPickResultCallback: (dialog: Dialog, picker: FilePickerRL, path: String) -> Unit
    ): Dialog {
        val dialog = Dialog(context)

        val filePickerRL = FilePickerRL(context, type, initialPath, {
            dialog.dismiss()
        }, { picker, path ->
            dialog.dismiss()
            onPickResultCallback(dialog, picker, path)
        }, null, enableFilenameET)

        dialog.apply {
            setContentView(filePickerRL)
            setCanceledOnTouchOutside(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    filePickerRL.previous()
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        }
        DialogUtil.setDialogAttr(dialog, false, true)
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

    @Suppress("DuplicatedCode")
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
                        TODO()
                    }
                    1 -> {
                        // export image
                        createFilePickerDialog(
                            FilePickerRL.TYPE_PICK_FOLDER,
                            externalPath.image,
                            true
                        ) { _, picker, path ->
                            var filename = picker.filenameText!!
                            val extension = File(filename).extension
                            if (extension.toLowerCase(Locale.US) != ".png") {
                                filename += ".png"
                            }
                            val imgFile = File(path, filename)

                            paintView.exportImg(imgFile, paintView.measuredWidth, paintView.measuredHeight)
                        }
                    }
                    2 -> {
                        // import path
                        createFilePickerDialog(FilePickerRL.TYPE_PICK_FILE, externalPath.path) { _, _, path ->
                            dialogs.moreMenu.dismiss()

                            val progressView = View.inflate(context, R.layout.progress_bar, null)

                            progressView.progress_bar_title!!.text =
                                context.getString(R.string.fdb_importing_path_progress_title)
                            val progressBar = progressView.progress_bar!!
                            val progressTV = progressView.progress_tv!!
                            val progressDialog = createDialog(progressView, width = MATCH_PARENT)
                            progressDialog.setCanceledOnTouchOutside(false)
                            progressDialog.show()

                            val file = File(path)
                            val pathVersion = PaintView.getPathVersion(file)

                            val tryDo = AsyncTryDo()

                            paintView.asyncImportPathFile(file, {
                                Common.runOnUiThread(context) {
                                    progressDialog.dismiss()
                                    ToastUtils.show(
                                        context,
                                        context.getString(R.string.fdb_importing_path_succeeded_toast)
                                    )

                                    colorPickers.brush.color = paintView.drawingColor
                                    panelRL.getPanelTextView(6).text = context.getString(
                                        if (paintView.isEraserMode) {
                                            R.string.fdb_panel_erasing_mode
                                        } else {
                                            R.string.fdb_panel_drawing_mode
                                        }
                                    )

                                    when (pathVersion) {
                                        PathVersion.VERSION_3_0 -> {
                                            try {
                                                val db = SQLite3.open(path)
                                                val extraInfos = PathSaver.getExtraInfos(db)
                                                db.close()
                                                extraInfos ?: return@runOnUiThread
                                                val savedColors =
                                                    extraInfos.getJSONArray("savedColors") ?: return@runOnUiThread
                                                val list = ArrayList<HSVAColorPickerRL.SavedColor>()
                                                for (i in 0 until savedColors.length()) {
                                                    val savedColor = savedColors.getJSONObject(i)

                                                    val hsvaJSONArray = savedColor.getJSONArray("colorHSVA")
                                                    list.add(
                                                        HSVAColorPickerRL.SavedColor(
                                                            floatArrayOf(
                                                                hsvaJSONArray.getDouble(0).toFloat(),
                                                                hsvaJSONArray.getDouble(1).toFloat(),
                                                                hsvaJSONArray.getDouble(2).toFloat()
                                                            ),
                                                            hsvaJSONArray.getInt(3),
                                                            savedColor.getString("colorName")
                                                        )
                                                    )
                                                }
                                                colorPickers.brush.setSavedColor(list)

                                                val defaultTransformationJSONObject =
                                                    extraInfos.getJSONObject("defaultTransformation")

                                                val matrix =
                                                    ExtraInfosUtils.getDefaultTransformation(
                                                        defaultTransformationJSONObject
                                                    )
                                                paintView.defaultTransformation = matrix
                                            } catch (_: Exception) {
                                            }
                                        }
                                        else -> {
                                        }
                                    }
                                }
                            }, { progress ->

                                tryDo.tryDo { _, notifier ->
                                    progress!!
                                    context.runOnUiThread {
                                        progressBar.setProgressCompat((progress * 100F).toInt(), true)
                                        progressTV.text = context.getString(R.string.percentage, progress * 100F)
                                        notifier.finish()
                                    }
                                }

                            }, 0/* TODO */, pathVersion)
                        }.show()
                    }
                    3 -> {
                        // export path

                        val extraInfos = ExtraInfos(
                            paintView.isLockStrokeEnabled,
                            paintView.lockedDrawingStrokeWidth,
                            paintView.lockedEraserStrokeWidth,
                            colorPickers.brush.savedColors,
                            paintView.defaultTransformation,
                            layerManagerView.getLayersInfo()
                        )
                        pathSaver.setExtraInfos(extraInfos)
                        pathSaver.flush()

                        createFilePickerDialog(
                            FilePickerRL.TYPE_PICK_FOLDER,
                            externalPath.path,
                            true
                        ) { _, picker, path ->
                            dialogs.moreMenu.dismiss()

                            exportPath(path, picker.filenameET!!.text.toString())
                        }.show()
                    }
                    4 -> {
                        // reset transformation
                        paintView.resetTransformation()
                    }
                    5 -> {
                        // manage layers
                        dialogs.layerManager.show()
                    }
                    6 -> {
                        // hide drawing board
                        createConfirmationDialog({ _, _ ->
                            hideFDB()
                            dialogs.moreMenu.dismiss()
                        }, R.string.fdb_hide_fdb_confirmation_dialog).show()
                    }
                    7 -> {
                        // drawing statistics
                        TODO()
                    }
                    8 -> {
                        // transformation settings
                        dialogs.transformationSettings.show()
                    }
                    else -> {
                    }
                }
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

    private fun createEraserOpacityDialog(): Dialog {
        val inflate = View.inflate(context, R.layout.fdb_eraser_opacity_adjusting_view, null)
        val opacitySlider = inflate.opacity!!
        opacitySlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {

            }

            override fun onStopTrackingTouch(slider: Slider) {
                paintView.eraserAlpha = slider.value.toInt()
            }
        })

        val dialog = createDialog(inflate, dim = false)
        DialogUtils.setDialogAttr(dialog, width = MATCH_PARENT, overlayWindow = true)
        return dialog
    }

    private fun exportPath(internalPath: String, filename: String) {

        val progressView = View.inflate(context, R.layout.progress_bar, null)
        val progressTitle = progressView.progress_bar_title!!
        val progressBar = progressView.progress_bar!!
        val progressTV = progressView.progress_tv!!

        val dialog = createDialog(progressView).apply {
            setCanceledOnTouchOutside(false)
        }
        dialog.show()

        val tryDo = AsyncTryDo()
        Thread {
            try {
                val pathFile = File(internalPath, filename)
                FileUtil.copy(pathFiles.tmpPathFile, pathFile)

                val first = arrayOf(true, true)
                /*TODO
                PathProcessor.optimizePath(pathFile.path) { phase, progress ->
                    phase!!
                    tryDo.tryDo { _, notifier ->
                        context.runOnUiThread {
                            when (phase) {
                                PathProcessor.ProgressCallback.Phase.PHASE_1 -> {
                                    if (first[0]) {
                                        progressTitle.text =
                                            context.getString(R.string.fdb_process_path_progress_title, 1)
                                        first[0] = false
                                    }
                                    progressBar.progress = (progress * 100F).toInt()
                                    progressTV.text = context.getString(R.string.progress_tv, progress * 100F)
                                }
                                PathProcessor.ProgressCallback.Phase.PHASE_2 -> {
                                    if (first[1]) {
                                        progressTitle.text =
                                            context.getString(R.string.fdb_process_path_progress_title, 2)
                                        first[1] = false
                                    }
                                    progressBar.progress = (progress * 100F).toInt()
                                    progressTV.text = context.getString(R.string.progress_tv, progress * 100F)
                                }
                                PathProcessor.ProgressCallback.Phase.DONE -> {
                                    progressBar.progress = 100
                                    progressTitle.text = context.getString(R.string.process_done)
                                    progressTV.text = context.getString(R.string.progress_tv, 100F)
                                }
                            }
                            notifier.finish()
                        }
                    }
                    if (phase == PathProcessor.ProgressCallback.Phase.DONE) {
                        context.runOnUiThread {
                            dialog.dismiss()
                        }
                    }
                }*/

                ToastUtils.show(context, R.string.fdb_exporting_path_succeeded_toast)
            } catch (e: IOException) {
                Common.showException(e, context)
                dialog.dismiss()
            }
        }.start()
    }

    private fun hideFDB() {
        showHideNotification()

        wm.removeView(panelSV)
    }

    fun restoreFDB() {
        wm.addView(panelSV, panelLP)
    }

    private fun showHideNotification() {
        val intent = Intent(FdbBroadcastReceiver.ACTION_FDB_SHOW)
        intent.putExtra(FdbBroadcastReceiver.EXTRA_FDB_ID, timestamp)

        val notificationId = timestamp.hashCode()
        val pi = PendingIntent.getBroadcast(context, notificationId, intent, 0)!!

        val nm =
            context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(
            context,
            MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL
        )
        builder.apply {
            setSmallIcon(R.drawable.ic_db)
            setContentTitle(context.getString(R.string.fdb_hide_fdb_notification_title))
            setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(
                        R.string.fdb_hide_fdb_notification_text,
                        Date(timestamp).toString()
                    )
                )
            )
            setChannelId(MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setOngoing(false)
            setContentIntent(pi)
        }
        nm.notify(notificationId, builder.build())
    }

    /**
     * For preventing dragging the panel outside the screen
     *
     * update it on the change of the panel dimension
     */
    private fun updatePanelDimension() {
        panelDimension.apply {
            panelRL.measure(0, 0)
            width = panelRL.measuredWidth
            height = panelRL.measuredHeight
        }
    }

    private fun pickScreenColorAction() {

        if (receivers.startScreenColorPicker == null) {
            receivers.startScreenColorPicker = StartScreenColorPickerReceiver { fdbId ->
                if (fdbId == this.getFdbId()) {
                    hasStartedScreenColorPicker = true
                }
            }
            context.applicationContext.registerReceiver(
                receivers.startScreenColorPicker,
                IntentFilter(StartScreenColorPickerReceiver.ACTION_SCREEN_COLOR_PICKER_ON_STARTED)
            )
        }

        if (receivers.screenColorPickerResult == null) {
            receivers.screenColorPickerResult = ScreenColorPickerResultReceiver(timestamp) { color ->
                ToastUtils.show(context, ColorUtils.getHexString(color, true))
                colorPickers.brush.color = color
            }
            val filter = IntentFilter(ScreenColorPickerResultReceiver.ACTION_ON_SCREEN_COLOR_PICKED)
            context.applicationContext.registerReceiver(receivers.screenColorPickerResult, filter)
        }

        // close the old one if exists
        if (hasStartedScreenColorPicker) {
            sendScreenColorPickerStopRequestBroadcast()
            hasStartedScreenColorPicker = false
            return
        }

        stopFDB()

        val intent = Intent(context, ScreenColorPickerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(ScreenColorPickerActivity.EXTRA_FDB_ID, this@FdbWindow.timestamp)
        val pi = PendingIntent.getActivity(context.applicationContext, timestamp.hashCode(), intent, 0)
        pi.send()
    }

    override fun toString(): String {
        return "FdbWindow(timestamp=$timestamp)"
    }

    fun getFdbId(): Long {
        return timestamp
    }

    fun exit() {
        stopFDB()
        if (hasStartedScreenColorPicker) {
            sendScreenColorPickerStopRequestBroadcast()
            hasStartedScreenColorPicker = false
        }
        context.applicationContext.unregisterReceiver(receivers.main)
        receivers.screenColorPickerResult?.let { context.applicationContext.unregisterReceiver(it) }
        receivers.startScreenColorPicker?.let { context.applicationContext.unregisterReceiver(it) }
    }

    private fun sendScreenColorPickerStopRequestBroadcast() {
        val stopIntent = Intent(ScreenColorPickerService.StopRequestReceiver.ACTION_SCREEN_COLOR_PICKER_STOP)
        stopIntent.putExtra(ScreenColorPickerService.StopRequestReceiver.EXTRA_FDB_ID, timestamp)
        context.applicationContext.sendBroadcast(stopIntent)
    }

    private fun createTransformationSettingsDialog(): Dialog {
        val inflate = View.inflate(context, R.layout.fdb_transformation_settings_view, null)
        val moveCB = inflate.move!!
        val zoomCB = inflate.zoom!!
        val rotateCB = inflate.rotate!!

        moveCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isMoveTransformationEnabled = isChecked
        }
        zoomCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isZoomTransformationEnabled = isChecked
        }
        rotateCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isRotateTransformationEnabled = isChecked
        }

        val setAsDefaultTransformation = inflate.set_as_default!!
        val resetToDefaultTransformation = inflate.reset_to_default!!

        setAsDefaultTransformation.setOnClickListener {
            paintView.setAsDefaultTransformation()
            ToastUtils.show(context, R.string.set_done_toast)
        }
        resetToDefaultTransformation.setOnClickListener {
            paintView.resetDefaultTransformation()
            ToastUtils.show(context, R.string.reset_success_toast)
        }

        return createDialog(inflate)
    }

    private fun createLayerManagerDialog(): Dialog {
        val dialog = createDialog(layerManagerView).also {
            DialogUtils.setDialogAttr(it, width = MATCH_PARENT, height = MATCH_PARENT, overlayWindow = true)
        }

        dialog.setOnDismissListener {
            val layerState = layerManagerView.getLayerState()
            val checkedId = layerState.checkedId
            Common.doAssertion(checkedId != -1L)
            paintView.updateLayerState(layerState.orderList, checkedId)
            paintView.invalidate()
        }
        return dialog
    }

    companion object {
        private val externalPath = object {
            lateinit var path: File
            lateinit var image: File
        }
    }
}