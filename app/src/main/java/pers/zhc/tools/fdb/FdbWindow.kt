package pers.zhc.tools.fdb

import android.app.Dialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PixelFormat.RGBA_8888
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager.LayoutParams.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.fdb_eraser_opacity_adjusting_view.view.*
import kotlinx.android.synthetic.main.fdb_panel_settings_view.view.*
import kotlinx.android.synthetic.main.fdb_panel_settings_view.view.ll
import kotlinx.android.synthetic.main.fdb_stoke_settings_view.view.*
import kotlinx.android.synthetic.main.fdb_transformation_settings_view.view.*
import kotlinx.android.synthetic.main.progress_bar.view.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.colorpicker.*
import pers.zhc.tools.databinding.FdbPathImportPromptDialogBinding
import pers.zhc.tools.databinding.FdbPathImportWindowBinding
import pers.zhc.tools.filepicker.FilePickerRL
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener.ViewDimension
import pers.zhc.tools.floatingdrawing.PaintView
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
class FdbWindow(activity: FdbMainActivity) {
    @Suppress("PrivatePropertyName")
    private val TAG = javaClass.name
    private val context = activity as Context
    private val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val panelRL = PanelRL(context)
    private val panelSV = ScrollView(context)
    private val panelLP = WindowManager.LayoutParams()

    private val paintView = PaintView(context)
    private val paintViewLP = WindowManager.LayoutParams()

    private val pathImportWindow by lazy {
        FdbPathImportWindowBinding.inflate(LayoutInflater.from(context)).root
    }
    private val pathImportWindowLP = WindowManager.LayoutParams()
    private val pathImportWindowBindings by lazy {
        FdbPathImportWindowBinding.bind(pathImportWindow)
    }

    private var operationMode = OperationMode.OPERATING
    private var brushMode = BrushMode.DRAWING

    private val colorPickers = ColorPickers()
    private val dialogs = Dialogs()

    private var followBrushColor = false
    private var invertTextColor = false
    val timestamp = System.currentTimeMillis()
    val fdbId
        get() = timestamp
    private val pathFiles = object {
        val tmpPathDir = File(context.filesDir, "path")
        val tmpPathFile = File(tmpPathDir, "$timestamp.path")
    }

    // TODO: 7/29/21 rewrite position updater
    private val panelDimension = ViewDimension()
    private val positionUpdater = FloatingViewOnTouchListener(panelLP, wm, panelSV, 0, 0, panelDimension)

    private val pathImportWindowDimension = ViewDimension()
    private val pathImportWindowPositionUpdater =
        FloatingViewOnTouchListener(pathImportWindowLP, wm, pathImportWindow, 0, 0, pathImportWindowDimension)

    private val pathSaver = PathSaver(pathFiles.tmpPathFile.path)

    private val receivers = object {
        lateinit var main: FdbBroadcastReceiver
        var colorPickerCheckpoint: ScreenColorPickerCheckpointReceiver? = null
        var colorPickerResult: ScreenColorPickerResultReceiver? = null
    }

    private var layerManagerView: LayerManagerView

    private var hasStartedScreenColorPicker = false

    var onExitListener: OnExitListener? = null
    private lateinit var updateEraserOpacitySlider: (Float) -> Unit

    var hardwareAcceleration = false
        set(value) {
            field = value
            paintView.setLayerType(
                if (value) {
                    View.LAYER_TYPE_HARDWARE
                } else {
                    View.LAYER_TYPE_SOFTWARE
                }, null
            )
        }

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
            @Suppress("DEPRECATION")
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
                                brushMode = brushMode.gerReverse()
                                paintView.isEraserMode = when (brushMode) {
                                    BrushMode.DRAWING -> false
                                    BrushMode.ERASING -> true
                                    else -> unreachable()
                                }
                                updateBrushModeText()
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

        pathImportWindowLP.apply {
            flags = FLAG_NOT_FOCUSABLE
            type = floatingWindowType
            format = RGBA_8888
            width = WRAP_CONTENT
            height = WRAP_CONTENT
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

            layerManagerView = LayerManagerView(context) { layerInfo ->
                paintView.add1Layer(layerInfo)
            }

            setOnScreenDimensionChangedListener { width, height ->
                positionUpdater.updateParentDimension(width, height)
                pathImportWindowPositionUpdater.updateParentDimension(width, height)
            }
            post {
                // add the default layer
                val id = System.currentTimeMillis()
                val defaultLayerInfo = LayerInfo(id, context.getString(R.string.fdb_layer_default_name), true)

                paintView.add1Layer(defaultLayerInfo)
                paintView.switchLayer(id)
                layerManagerView.add1Layer(defaultLayerInfo)
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
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        positionUpdater.updateParentDimension(screenWidth, screenHeight)
        pathImportWindowPositionUpdater.updateParentDimension(screenWidth, screenHeight)

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

    private fun updateBrushModeText() {
        val tv = panelRL.getPanelTextView(6)
        tv.text = brushMode.getString(context)
    }

    private fun showBrushWidthAdjustingDialog() {
        val dialog = createDialog(createStrokeSettingsView(), width = MATCH_PARENT)
        dialog.show()
    }

    private fun createStrokeSettingsView(): View {
        val inflate = View.inflate(context, R.layout.fdb_stoke_settings_view, null)!!

        val rg = inflate.rg!!
        val widthSlider = inflate.slider!!
        val infoTV = inflate.tv!!
        val lockBrushCB = inflate.cb!!
        val hardnessSlider = inflate.hardness_slider!!
        val strokeShowView = inflate.stroke_show!!

        lockBrushCB.isChecked = paintView.isLockStrokeEnabled
        rg.check(
            if (paintView.isEraserMode) {
                R.id.eraser_radio
            } else {
                R.id.brush_radio
            }
        )

        val getStrokeWidth = { mode: BrushMode ->
            when (mode) {
                BrushMode.DRAWING -> paintView.drawingStrokeWidth
                BrushMode.ERASING -> paintView.eraserStrokeWidth
                BrushMode.IN_USE -> paintView.strokeWidthInUse
            }
        }
        val getHardness = { mode: BrushMode ->
            when (mode) {
                BrushMode.DRAWING -> paintView.strokeHardness
                BrushMode.ERASING -> paintView.eraserHardness
                BrushMode.IN_USE -> if (paintView.isEraserMode) {
                    paintView.eraserHardness
                } else {
                    paintView.strokeHardness
                }
            }
        }

        // for non-linear width adjustment
        val base = 1.07
        val updateDisplay = { mode: BrushMode ->
            val width = getStrokeWidth(mode)
            val hardness = getHardness(mode)

            strokeShowView.apply {
                setWidth(width * paintView.scale)
                strokeHardness = hardness
                setColor(paintView.drawingColor)
            }

            widthSlider.value = (ln(width.toDouble() * paintView.scale) / ln(base)).toFloat().coerceIn(0F, 100F)
            infoTV.text = context.getString(
                R.string.fdb_stroke_width_info,
                width,
                paintView.scale * width,
                paintView.scale * 100F
            )

            hardnessSlider.value = hardness
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
                val displayWidth = base.pow(value.toDouble()).toFloat()
                val realWidth = displayWidth / paintView.scale
                updateWidthAndDisplay(realWidth)
            }
        }

        rg.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.brush_radio -> {
                    updateDisplay(BrushMode.DRAWING)
                }

                R.id.eraser_radio -> {
                    updateDisplay(BrushMode.ERASING)
                }

                else -> {
                }
            }
        }

        lockBrushCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isLockStrokeEnabled = isChecked
        }

        hardnessSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) {
                return@addOnChangeListener
            }
            when (rg.checkedRadioButtonId) {
                R.id.brush_radio -> {
                    paintView.strokeHardness = value
                    updateDisplay(BrushMode.DRAWING)
                }

                R.id.eraser_radio -> {
                    paintView.eraserHardness = value
                    updateDisplay(BrushMode.ERASING)
                }

                else -> {
                }
            }
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
        IN_USE;

        fun getString(context: Context): String {
            val res = when (this) {
                DRAWING -> R.string.fdb_panel_drawing_mode
                ERASING -> R.string.fdb_panel_erasing_mode
                else -> {
                    throw RuntimeException("invalid \"in use\" state")
                }
            }
            return context.getString(res)
        }

        fun gerReverse(): BrushMode {
            return when (this) {
                DRAWING -> ERASING
                ERASING -> DRAWING
                else -> {
                    throw RuntimeException("invalid \"in use\" state")
                }
            }
        }
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

    private fun createPromptDialog(
        @StringRes titleRes: Int,
        editText: EditText,
        callback: PromptDialogCallback
    ): AlertDialog {
        val dialog = DialogUtils.createPromptDialog(context, titleRes, callback, editText = editText)
        DialogUtils.setDialogAttr(dialog, isTransparent = false, overlayWindow = true)
        return dialog
    }

    private fun createFilePickerDialog(
        type: Int = FilePickerRL.TYPE_PICK_FILE,
        initialPath: File = externalPath.path,
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

        // set to defaults
        updatePanelColor(colorPickers.panel.color)
        updatePanelTextColor(colorPickers.panelText.color)

        return dialog
    }

    private fun exportImgWithDialog(output: File, width: Int, height: Int) {
        val progressDialog = ProgressDialog(context).apply {
            DialogUtils.setDialogAttr(this, width = MATCH_PARENT, overlayWindow = true)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }.also { it.show() }
        val progressView = progressDialog.getProgressView().also { it.setIsIndeterminateMode(false) }

        val tryDo = AsyncTryDo()
        val handler = Handler(Looper.getMainLooper())

        Thread {
            paintView.exportImg(output, width, height) { type, layerName, progress ->

                when (type) {
                    PaintView.ImageExportProgressType.REDRAWING -> {
                        tryDo.tryDo { _, notifier ->
                            handler.post {
                                progressView.setTitle(
                                    context.getString(
                                        R.string.fdb_export_image_redrawing_phase_title,
                                        layerName
                                    )
                                )
                                progressView.setProgressAndText(progress)
                                notifier.finish()
                            }
                        }
                    }

                    PaintView.ImageExportProgressType.COMPRESSING -> {
                        handler.post {
                            progressView.setIsIndeterminateMode(true)
                            progressView.setTitle(context.getString(R.string.fdb_export_image_compressing_phase_title))
                        }
                    }
                }
            }
            handler.post {
                progressDialog.dismiss()
                ToastUtils.show(context, R.string.save_success_toast)
            }
        }.start()
    }

    private fun createMoreOptionDialog(): Dialog {
        val onClickActions: ((index: Int) -> View.OnClickListener) = { index ->
            View.OnClickListener {
                when (index) {
                    0 -> {
                        // import image
                        Common.toastTodo(context)
                    }

                    1 -> {
                        // export image
                        createPromptDialog(
                            R.string.fdb_export_image_size_multiplier_prompt_title,
                            EditText(context).apply { setText(1.toString()) }) { _, editText ->
                            val multiple = editText.text.toString().toIntOrNull() ?: run {
                                ToastUtils.show(context, R.string.please_enter_correct_value_toast)
                                return@createPromptDialog
                            }
                            val imageWidth = paintView.bitmapWidth * multiple
                            val imageHeight = paintView.bitmapHeight * multiple

                            createFilePickerDialog(
                                FilePickerRL.TYPE_PICK_FOLDER,
                                externalPath.image,
                                true
                            ) { _, picker, path ->
                                var filename = picker.filenameText!!
                                val extension = File(filename).extension
                                if (extension.lowercase(Locale.US) != ".png") {
                                    filename += ".png"
                                }
                                val imgFile = File(path, filename)

                                exportImgWithDialog(imgFile, imageWidth, imageHeight)
                            }.show()
                        }.show()
                    }

                    2 -> {
                        // import path
                        showImportPathDialog(externalPath.path)
                    }

                    3 -> {
                        // export path

                        val extraInfo = ExtraInfo(
                            paintView.isLockStrokeEnabled,
                            paintView.lockedDrawingStrokeWidth,
                            paintView.lockedEraserStrokeWidth,
                            colorPickers.brush.savedColors,
                            paintView.defaultTransformation.getValuesNew(),
                            layerManagerView.getLayersInfo()
                        )
                        pathSaver.setExtraInfos(extraInfo)
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
                        showPathStatDialog()
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

    fun showImportPathDialog(dir: File) {
        val performImporting = { file: File ->
            val pathVersion = PathVersion.getPathVersion(file)

            val tryDo = AsyncTryDo()

            Thread {

                val stopwatch = Stopwatch.start()

                try {
                    paintView.importPathFile(file, { progress ->
                        // progress callback
                        tryDo.tryDo { _, notifier ->
                            progress!!
                            context.runOnUiThread {
                                pathImportWindowBindings.progressCircular.setProgressCompat(
                                    (progress * 100F).toInt(),
                                    true
                                )
                                notifier.finish()
                            }
                        }
                    }, pathVersion)
                } catch (e: Exception) {
                    ToastUtils.showError(context, R.string.fdb_import_failed, e)
                    context.runOnUiThread {
                        wm.removeView(pathImportWindow)
                    }
                    return@Thread
                }

                Log.i(TAG, "importPathFile time elapsed: ${stopwatch.stop()} ms")

                Common.runOnUiThread(context) {
                    // done action
                    pathImportWindowBindings.progressCircular.setProgressCompat(100, true)
                    wm.removeView(pathImportWindow)
                    ToastUtils.show(
                        context,
                        context.getString(R.string.fdb_importing_path_succeeded_toast)
                    )

                    colorPickers.brush.color = paintView.drawingColor
                    brushMode = if (paintView.isEraserMode) {
                        BrushMode.ERASING
                    } else {
                        BrushMode.DRAWING
                    }
                    updateBrushModeText()

                    updateEraserOpacitySlider(paintView.eraserAlpha.toFloat() / 255F)

                    when (pathVersion) {
                        PathVersion.VERSION_3_0, PathVersion.VERSION_3_1, PathVersion.VERSION_4_0 -> {
                            var extraInfo: ExtraInfo? = null
                            SQLite3::class.withNew(file.path) {
                                extraInfo = ExtraInfo.getExtraInfo(it)
                            }
                            extraInfo ?: return@runOnUiThread

                            extraInfo!!.savedColors?.let { colorPickers.brush.setSavedColor(it) }
                            extraInfo!!.defaultTransformation?.let {
                                paintView.defaultTransformation = Matrix::class.fromValues(it)
                            }
                        }

                        else -> {
                        }
                    }
                }

            }.start()
        }

        createFilePickerDialog(FilePickerRL.TYPE_PICK_FILE, dir) { _, _, path ->
            dialogs.moreMenu.dismiss()

            val bindings = FdbPathImportPromptDialogBinding.inflate(LayoutInflater.from(context), null, false)
            bindings.showDrawingSwitch.setOnCheckedChangeListener { _, isChecked ->
                bindings.fdbDefaultDrawingIntervalTil.visibility = if (isChecked) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.import_)
                .setNegativeAction()
                .setPositiveAction { _, _ ->
                    val interval = bindings.fdbDefaultDrawingIntervalTil.editText!!.text.toString().toInt()
                    paintView.drawingInterval = interval
                    paintView.isShowDrawing = bindings.showDrawingSwitch.isChecked
                    wm.addView(pathImportWindow, pathImportWindowLP)
                    setUpPathImportWindow()
                    performImporting(File(path))
                }
                .setView(bindings.root)
                .create().apply {
                    DialogUtils.setDialogAttr(this, width = MATCH_PARENT, overlayWindow = true)
                }.show()
        }.show()
    }

    private fun showPathStatDialog() {
        createFilePickerDialog { _, _, path ->
            val db = SQLite3.open(path)
            try {
                val infoStr = getPathV3_0StatisticsInfoStr(db)
                createDialog(TextView(context).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    text = infoStr
                }).show()
            } catch (e: Exception) {
                Common.showException(e, context)
            }
            db.close()
        }.show()
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

        updateEraserOpacitySlider = { value: Float ->
            opacitySlider.value = value
        }

        val dialog = createDialog(inflate, dim = false)
        DialogUtils.setDialogAttr(dialog, width = MATCH_PARENT, overlayWindow = true)
        return dialog
    }

    private fun exportPath(internalPath: String, filename: String) {
        // path "undo" optimization
        val progressView = View.inflate(context, R.layout.progress_bar, null)
        val progressTitle = progressView.progress_bar_title!!
        val progressBar = progressView.progress_bar!!
        val progressTV = progressView.progress_tv!!

        val dialog = createDialog(progressView).apply {
            setCanceledOnTouchOutside(false)
        }.also { it.show() }

        val setProgress = { layerNumber: Int, progress: Float, phase: PathProcessor.ProgressPhase ->
            progressTitle.text = context.getString(
                R.string.fdb_process_path_progress_title,
                layerNumber, phase.number
            )
            progressTV.text = context.getString(R.string.progress_tv, progress * 100F)
            progressBar.setProgressCompat((progress * 100F).toInt(), true)
        }

        val tryDo = AsyncTryDo()
        val threadAction = {
            val destFile = File(internalPath, filename)
            FileUtil.copy(pathFiles.tmpPathFile, destFile)

            val database = SQLite3.open(destFile.path)
            val pathLayerTables = database.getTables().filter { it.startsWith("path_layer_") }

            pathLayerTables.forEachIndexed { layerIndex, table ->
                PathProcessor.optimizePath(destFile, table) { progress, phase ->
                    tryDo.tryDo { _, notifier ->
                        context.runOnUiThread {
                            when (phase) {
                                PathProcessor.ProgressPhase.PHASE1, PathProcessor.ProgressPhase.PHASE2 -> {
                                    setProgress(layerIndex + 1, progress, phase)
                                }

                                PathProcessor.ProgressPhase.DONE -> {
                                    setProgress(layerIndex + 1, 100F, PathProcessor.ProgressPhase.PHASE2)
                                }
                            }
                            notifier.finish()
                        }
                    }
                    if (phase == PathProcessor.ProgressPhase.DONE && layerIndex == pathLayerTables.lastIndex) {
                        context.runOnUiThread {
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
        Thread {
            try {
                threadAction()
                ToastUtils.show(context, R.string.fdb_exporting_path_succeeded_toast)
            } catch (e: IOException) {
                context.runOnUiThread {
                    Common.showException(e, context)
                    dialog.dismiss()
                }
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
        val pi = PendingIntent.getBroadcast(
            context, notificationId, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )!!

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
        // when an old one exists, this action is to close it
        if (hasStartedScreenColorPicker) {
            sendScreenColorPickerStopRequestBroadcast()
            hasStartedScreenColorPicker = false
            return
        }

        val sendStartPickerViewRequest = {
            val intent = Intent(ScreenColorPickerOperationReceiver.ACTION_START).apply {
                putExtra(ScreenColorPickerOperationReceiver.EXTRA_REQUEST_ID, fdbId.toString())
            }
            context.applicationContext.sendBroadcast(intent)

            receivers.colorPickerResult = ScreenColorPickerResultReceiver { requestId, color ->
                if (requestId == fdbId.toString()) {
                    // result belongs to this FDB
                    colorPickers.brush.color = color
                    ToastUtils.show(context, ColorUtils.getHexString(color, false))
                }
            }.also {
                context.applicationContext.registerReceiver(it, IntentFilter().apply {
                    addAction(ScreenColorPickerResultReceiver.ACTION_ON_COLOR_PICKED)
                })
            }
            hasStartedScreenColorPicker = true
        }

        receivers.colorPickerCheckpoint =
            receivers.colorPickerCheckpoint ?: ScreenColorPickerCheckpointReceiver { requestId, action ->
                when (action) {
                    ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_DENIED,
                    ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_GRANTED -> {
//                        startFDB()
                    }

                    ScreenColorPickerCheckpointReceiver.ACTION_SERVICE_STARTED -> {
                        requestId!!
                        if (requestId == fdbId.toString()) {
                            sendStartPickerViewRequest()
                        }
                    }

                    else -> {}
                }
            }.also {
                context.applicationContext.registerReceiver(it, IntentFilter().apply {
                    addAction(ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_GRANTED)
                    addAction(ScreenColorPickerCheckpointReceiver.ACTION_PERMISSION_DENIED)
                    addAction(ScreenColorPickerCheckpointReceiver.ACTION_SERVICE_STARTED)
                })
            }


        if (ScreenColorPickerDemoActivity.serviceRunning) {
            // service is already running, so this means the `MediaProjection` can be reused
            // just send the color picker view request broadcast
            sendStartPickerViewRequest()
        } else {
            // request the permission
            // hide the FDB first, otherwise the popup permission requesting dialog may be unclickable to the user
//            stopFDB()
            val intent = Intent(context, ScreenColorPickerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ScreenColorPickerCheckpointReceiver.EXTRA_REQUEST_ID, fdbId.toString())
            }
            PendingIntent.getActivity(
                context.applicationContext,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
            ).send()
        }
    }

    override fun toString(): String {
        return "FdbWindow(timestamp=$timestamp)"
    }

    fun exit() {
        stopFDB()
        if (hasStartedScreenColorPicker) {
            sendScreenColorPickerStopRequestBroadcast()
            hasStartedScreenColorPicker = false
        }
        context.applicationContext.unregisterReceiver(receivers.main)
        receivers.colorPickerCheckpoint?.let { context.applicationContext.unregisterReceiver(it) }
        receivers.colorPickerResult?.let { context.applicationContext.unregisterReceiver(it) }

        onExitListener?.invoke()
    }

    private fun sendScreenColorPickerStopRequestBroadcast() {
        val intent = Intent(ScreenColorPickerOperationReceiver.ACTION_STOP).apply {
            putExtra(ScreenColorPickerOperationReceiver.EXTRA_REQUEST_ID, fdbId.toString())
        }
        context.applicationContext.sendBroadcast(intent)
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
            paintView.updateLayerState(layerState)
        }
        return dialog
    }

    private fun getSelectedStatisticsIntData(db: SQLite3, mark: Int): Int {
        var r = 0
        val statement = db.compileStatement(
            """
                    SELECT COUNT(*)
                    FROM path
                    WHERE mark is ?
                    """.trimIndent()
        )
        statement.reset()
        statement.bind(1, mark)
        val cursor = statement.cursor
        cursor.step()
        r = cursor.getInt(0)
        statement.release()
        return r
    }

    @Suppress("FunctionName")
    private fun getPathV3_0StatisticsInfoStr(db: SQLite3): String {
        /*
         * info:
         * creation time
         * total recorded points number,
         * total recorded paths number,
         * drawing paths number,
         * erasing paths number,
         * undo times,
         * redo times
         */
        val totalPointsNum = intArrayOf(0)
        val drawingPathsNum: Int = getSelectedStatisticsIntData(db, 0x01)
        val erasingPathsNum: Int = getSelectedStatisticsIntData(db, 0x11)
        val totalPathsNum = drawingPathsNum + erasingPathsNum
        val createTime = arrayOfNulls<String>(1)
        db.exec(
            """
            SELECT create_timestamp
            FROM info
            """.trimIndent()
        ) { contents: Array<String> ->
            createTime[0] = Date(contents[0].toLong()).toString()
            0
        }
        db.exec(
            """
                SELECT COUNT(*)
                FROM path
                WHERE mark IS NOT 0x01
                  AND mark IS NOT 0x11
                  AND mark IS NOT 0x20
                  AND mark IS NOT 0x30
                """.trimIndent()
        ) { contents: Array<String> ->
            totalPointsNum[0] = contents[0].toInt()
            0
        }
        return context.getString(
            R.string.fdb_path_info,
            createTime[0],
            totalPointsNum[0],
            totalPathsNum,
            drawingPathsNum,
            erasingPathsNum,
            getSelectedStatisticsIntData(db, 0x20),
            getSelectedStatisticsIntData(db, 0x30)
        )
    }

    enum class PathImportState {
        PAUSED,
        IMPORTING,
    }

    companion object {
        private val externalPath = object {
            lateinit var path: File
            lateinit var image: File
        }
    }

    private fun setUpPathImportWindow() {
        pathImportWindowBindings.apply {
            var pathImportState = PathImportState.IMPORTING

            pauseButton.setOnClickListener {
                when (pathImportState) {
                    PathImportState.PAUSED -> {
                        pauseButton.setText(R.string.fdb_path_import_pause_button)
                        pathImportState = PathImportState.IMPORTING
                        paintView.isPathImportPaused = false
                        ToastUtils.show(context, R.string.fdb_path_import_resume_button)
                    }

                    PathImportState.IMPORTING -> {
                        pauseButton.setText(R.string.fdb_path_import_resume_button)
                        pathImportState = PathImportState.PAUSED
                        paintView.isPathImportPaused = true
                        ToastUtils.show(context, R.string.fdb_path_import_pause_button)
                    }
                }
            }
            @Suppress("ClickableViewAccessibility")
            dragIcon.setOnTouchListener { v, event ->
                pathImportWindowPositionUpdater.onTouch(v, event, true)
            }

            // in nanoseconds
            var drawingInterval = paintView.drawingInterval
            val updateDrawingInterval = {
                paintView.drawingInterval = drawingInterval
                speedTv.text = context.getString(R.string.fdb_path_import_speed_tv, drawingInterval)
            }.also { it() }

            val drawingIntervalStep = 100
            minusBtn.setOnClickListener {
                if (drawingInterval >= drawingIntervalStep) {
                    drawingInterval -= drawingIntervalStep
                }
                updateDrawingInterval()
            }

            addBtn.setOnClickListener {
                drawingInterval += drawingIntervalStep
                updateDrawingInterval()
            }

            if (!paintView.isShowDrawing) {
                speedTv.visibility = View.GONE
                pauseMinusButtonGroup.visibility = View.GONE
            }
        }
    }
}

typealias OnExitListener = () -> Unit
