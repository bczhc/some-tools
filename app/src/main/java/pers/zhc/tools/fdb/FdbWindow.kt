package pers.zhc.tools.fdb

import android.annotation.SuppressLint
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
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager.LayoutParams.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.colorpicker.*
import pers.zhc.tools.databinding.*
import pers.zhc.tools.filepicker.FilePickerRL
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener.ViewDimension
import pers.zhc.tools.floatingdrawing.PaintView
import pers.zhc.tools.utils.*
import pers.zhc.tools.views.CustomScrollView
import pers.zhc.tools.views.HSVAColorPickerRL
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * @author bczhc
 */
class FdbWindow(private val context: Context) {
    @Suppress("PrivatePropertyName")
    private val TAG = javaClass.name
    private val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val panelRL = PanelRL(context)
    private val panelSV = CustomScrollView(context)
    private val panelLL = LinearLayout(context)
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

    // TODO: refactor: combine floating windows
    private lateinit var layerManagerView: LayerManagerView
    private val layerManagerViewLP = WindowManager.LayoutParams()

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
    private val positionUpdater = FloatingViewOnTouchListener(panelLP, wm, panelLL, 0, 0, panelDimension)

    private val pathImportWindowDimension = ViewDimension()
    private val pathImportWindowPositionUpdater =
        FloatingViewOnTouchListener(pathImportWindowLP, wm, pathImportWindow, 0, 0, pathImportWindowDimension)
    private val layerManagerViewDimension = ViewDimension()
    private val layerManagerViewPositionUpdater by lazy {
        FloatingViewOnTouchListener(layerManagerViewLP, wm, layerManagerView, 0, 0, layerManagerViewDimension)
    }

    private val pathSaver = PathSaver(pathFiles.tmpPathFile.path)

    private val receivers = object {
        lateinit var main: FdbBroadcastReceiver
        var colorPickerCheckpoint: ScreenColorPickerCheckpointReceiver? = null
        var colorPickerResult: ScreenColorPickerResultReceiver? = null
    }

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
        panelLL.addView(panelSV)

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
                .xor(FLAG_LAYOUT_NO_LIMITS)
            type = floatingWindowType
            format = RGBA_8888
            width = DisplayUtil.getScreenSize(context).x
            height = DisplayUtil.getScreenSize(context).y
        }
        // 监听屏幕方向改变事件
        val orientationListener = object : OrientationEventListener(context) {
            private var previousWidth = 0
            private var previousHeight = 0
            override fun onOrientationChanged(orientation: Int) {
                val width = DisplayUtil.getScreenSize(context).x
                val height = DisplayUtil.getScreenSize(context).y
                if (width != previousWidth && height != previousHeight) {
                    paintViewLP.width = width
                    paintViewLP.height = height
                    // 当屏幕方向改变时，重新设置 paintViewRL 的宽高
                    wm.updateViewLayout(paintView, paintViewLP)
                    previousWidth = width
                    previousHeight = height
                }
            }
        }
        // 启动屏幕方向改变监听器
        orientationListener.enable()

        panelLP.apply {
            flags = FLAG_NOT_FOCUSABLE
            type = floatingWindowType
            format = RGBA_8888
            width = WRAP_CONTENT
            height = WRAP_CONTENT
        }

        panelRL.apply {
            onButtonClickedListener = { mode, buttonIndex ->
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
                                            .xor(FLAG_LAYOUT_NO_LIMITS)
                                        wm.updateViewLayout(paintView, paintViewLP)

                                        tv.text = context.getString(R.string.fdb_panel_drawing_mode)
                                        operationMode = OperationMode.DRAWING
                                    }

                                    OperationMode.DRAWING -> {
                                        paintViewLP.flags = FLAG_NOT_TOUCHABLE
                                            .xor(FLAG_NOT_FOCUSABLE)
                                            .xor(FLAG_LAYOUT_NO_LIMITS)
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
                                }, titleRes = R.string.fdb_clear_confirmation_dialog).show()
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
                                    orientationListener.disable()
                                    exit()
                                }, titleRes = R.string.fdb_exit_confirmation_dialog).show()
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

            onButtonLongClickedListener = { mode, index ->
                if (mode == PanelRL.MODE_PANEL && index == 7) {
                    // long click "clear": clear all layers
                    createConfirmationDialog({ _, _ ->
                        paintView.clearAllLayers()
                        layerManagerView.restoreDefault()
                        paintView.updateLayerState(layerManagerView.getLayerState())
                    }, titleRes = R.string.fdb_whether_to_clear_all_layer_dialog).show()
                }
            }

            @Suppress("ClickableViewAccessibility")
            setOnTouchListener { _, event ->
                return@setOnTouchListener positionUpdater.onTouch(panelLL, event, false)
            }
        }

        pathImportWindowLP.apply {
            flags = FLAG_NOT_FOCUSABLE
            type = floatingWindowType
            format = RGBA_8888
            width = WRAP_CONTENT
            height = WRAP_CONTENT
        }

        layerManagerViewLP.apply {
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

            brush.setOnColorPickedInterface { _, _, color, fromUser ->
                if (!fromUser) return@setOnColorPickedInterface
                updateBrushColor(color)
            }

            panel.setOnColorPickedInterface { _, _, color, fromUser ->
                if (!fromUser) return@setOnColorPickedInterface
                updatePanelColor(color)
            }

            panelText.setOnColorPickedInterface { _, _, color, fromUser ->
                if (!fromUser) return@setOnColorPickedInterface
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
            setUpLayerManagerWindow()

            setOnScreenDimensionChangedListener { width, height ->
                positionUpdater.updateParentDimension(width, height)
                pathImportWindowPositionUpdater.updateParentDimension(width, height)
                layerManagerViewPositionUpdater.updateParentDimension(width, height)
            }
            post {
                // add the default layer
                val defaultLayerInfo = Layer.defaultLayerInfo(context)
                val id = defaultLayerInfo.id

                paintView.add1Layer(defaultLayerInfo)
                paintView.switchLayer(id)
                layerManagerView.add1Layer(defaultLayerInfo)
                layerManagerView.setChecked(id)
            }

            setOnImportLayerAddedListener {
                awaitRunOnUiThread {
                    layerManagerView.add1Layer(it)
                }
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
            pressureSensingSettings = createPressureSensingSettingsDialog()
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

        val screenSize = DisplayUtil.getScreenSize(context)
        val screenWidth = screenSize.x
        val screenHeight = screenSize.y
        positionUpdater.updateParentDimension(screenWidth, screenHeight)
        pathImportWindowPositionUpdater.updateParentDimension(screenWidth, screenHeight)
        layerManagerViewPositionUpdater.updateParentDimension(screenWidth, screenHeight)

        receivers.main = FdbBroadcastReceiver(this)
        val filter = IntentFilter()
        filter.apply {
            addAction(FdbBroadcastReceiver.ACTION_FDB_SHOW)
            addAction(FdbBroadcastReceiver.ACTION_ON_CAPTURE_SCREEN_PERMISSION_DENIED)
            addAction(FdbBroadcastReceiver.ACTION_ON_CAPTURE_SCREEN_PERMISSION_GRANTED)
            addAction(FdbBroadcastReceiver.ACTION_ON_SCREEN_ORIENTATION_CHANGED)
        }
        context.applicationContext.registerReceiverCompat(receivers.main, filter)

        setUpLayerManagerWindow()

        val asyncTryDo = AsyncTryDo()
        paintView.setOnColorChangedCallback {
            if (!paintView.isShowDrawing) return@setOnColorChangedCallback
            asyncTryDo.tryDo { _, notifier ->
                runOnUiThread {
                    colorPickers.brush.color = it
                    if (followBrushColor) {
                        updatePanelColor(it)
                    }
                    notifier.finish()
                }
            }
        }
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
        val bindings = FdbStokeSettingsViewBinding.inflate(LayoutInflater.from(context))

        val rg = bindings.rg
        val widthSlider = bindings.slider
        val infoTV = bindings.tv
        val lockBrushCB = bindings.cb
        val hardnessSlider = bindings.hardnessSlider
        val strokeShowView = bindings.strokeShow

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

        return bindings.root
    }

    fun startFDB() {
        wm.addView(paintView, paintViewLP)
        wm.addView(panelLL, panelLP)
    }

    private fun stopFDB() {
        wm.removeView(panelLL)
        wm.removeView(paintView)
        wm.runCatching { removeView(pathImportWindow) }
        wm.runCatching { removeView(layerManagerView) }
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
        lateinit var pressureSensingSettings: Dialog
    }

    private fun createConfirmationDialog(
        positiveAction: DialogInterface.OnClickListener,
        negativeAction: DialogInterface.OnClickListener? = null,
        @StringRes titleRes: Int
    ): AlertDialog {
        return DialogUtils.createConfirmationAlertDialog(
            context,
            positiveAction = positiveAction,
            negativeAction = negativeAction ?: DialogInterface.OnClickListener { _, _ -> },
            titleRes = titleRes,
            applicationOverlay = true
        )
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
        defaultFilename: String = "",
        onPickResultCallback: (dialog: Dialog, picker: FilePickerRL, path: String) -> Unit
    ): Dialog {
        val dialog = Dialog(context)

        val filePickerRL = FilePickerRL(context, type, initialPath, {
            dialog.dismiss()
        }, { picker, path ->
            dialog.dismiss()
            onPickResultCallback(dialog, picker, path)
        }, defaultFilename, enableFilenameET).apply {
            dialogOverlay = true
        }

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
        val bindings = FdbPanelSettingsViewBinding.inflate(LayoutInflater.from(context))
        val dialog = createDialog(bindings.root)

        val panelColorBtn = bindings.panelColor
        val textColorBtn = bindings.textColor
        val followBrushColorSwitch = bindings.followPaintingColor
        val invertTextColorSwitch = bindings.invertTextColor

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
        var dismissDialog = { unreachable<Unit>() }

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
                        showImportPathFilePicker(externalPath.path)
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
                            true,
                            defaultFilename = ".path"
                        ) { _, picker, path ->
                            dialogs.moreMenu.dismiss()

                            val destFile = File(path, picker.filenameET!!.text.toString())
                            exportPath(destFile)
                        }.show()
                    }

                    4 -> {
                        // reset transformation
                        paintView.resetTransformation()
                    }

                    5 -> {
                        // manage layers
                        // toggle
                        if (wm.runCatching { wm.removeView(layerManagerView) }.isFailure) {
                            wm.addView(layerManagerView, layerManagerViewLP)
                        }
                        dismissDialog()
                    }

                    6 -> {
                        dialogs.pressureSensingSettings.show()

                    }

                    7 -> {
                        // hide drawing board
                        createConfirmationDialog({ _, _ ->
                            hideFDB()
                            dialogs.moreMenu.dismiss()
                        }, titleRes = R.string.fdb_hide_fdb_confirmation_dialog).show()
                    }

                    8 -> {
                        // drawing statistics
                        showPathStatDialog()
                    }

                    9 -> {
                        // transformation settings
                        dialogs.transformationSettings.show()
                    }

                    else -> {
                    }
                }
            }
        }

        val inflate = View.inflate(context, R.layout.fdb_panel_more_view, null)
        val ll = inflate.findViewById<LinearLayout>(R.id.ll)!!

        val btnStrings = context.resources.getStringArray(R.array.fdb_more_menu)
        btnStrings.forEachIndexed { i, btnString ->
            val button = MaterialButton(context)
            button.text = btnString
            button.setOnClickListener(onClickActions(i))

            ll.addView(button)
        }
        val dialog = createDialog(inflate)
        dismissDialog = { dialog.dismiss() }
        return dialog
    }

    fun showImportPathFilePicker(dir: File) {
        createFilePickerDialog(FilePickerRL.TYPE_PICK_FILE, dir) { _, _, path ->
            dialogs.moreMenu.dismiss()
            showImportPathDialog(path)
        }.show()
    }

    fun showImportPathDialog(path: String) {
        val performImporting = { file: File ->
            val pathVersion = PathVersion.getPathVersion(file)

            val tryDo = AsyncTryDo()

            Thread {

                val stopwatch = Stopwatch.start()

                try {
                    pathImportWindowBindings.pauseButton.setText(R.string.fdb_path_import_pause_button)
                    paintView.importPathFile(file, { progress, layerName, layerNumber, layerCount ->
                        // progress callback
                        tryDo.tryDo { _, notifier ->
                            runOnUiThread {
                                pathImportWindowBindings.progressCircular.setProgressCompat(
                                    (progress * 100F).toInt(),
                                    true
                                )
                                if (layerName != null) {
                                    pathImportWindowBindings.layerName.visibility = View.VISIBLE
                                    pathImportWindowBindings.layerProgress.visibility = View.VISIBLE
                                    pathImportWindowBindings.layerName.text =
                                        context.getString(R.string.fdb_importing_path_layer_name, layerName)
                                    pathImportWindowBindings.layerProgress.text =
                                        context.getString(
                                            R.string.fdb_importing_path_layer_progress,
                                            layerNumber,
                                            layerCount
                                        )
                                } else {
                                    pathImportWindowBindings.layerName.visibility = View.GONE
                                    pathImportWindowBindings.layerProgress.visibility = View.GONE
                                }
                                notifier.finish()
                            }
                        }
                    }, pathVersion)
                } catch (e: Exception) {
                    ToastUtils.showError(context, R.string.fdb_import_failed, e)
                    runOnUiThread {
                        wm.runCatching { removeView(pathImportWindow) }
                    }
                    return@Thread
                }

                Log.i(TAG, "importPathFile time elapsed: ${stopwatch.stop()} ms")

                Common.runOnUiThread {
                    // done action
                    pathImportWindowBindings.progressCircular.setProgressCompat(100, true)
                    wm.runCatching { removeView(pathImportWindow) }
                    if (!paintView.isImportingTerminated) {
                        ToastUtils.show(context, R.string.fdb_importing_path_succeeded_toast)
                    } else {
                        ToastUtils.show(context, R.string.fdb_importing_canceled)
                    }
                    pathImportWindowBindings.layerName.visibility = View.GONE
                    pathImportWindowBindings.layerProgress.visibility = View.GONE
                    pathImportWindowBindings.layerName.text = ""
                    pathImportWindowBindings.layerProgress.text = ""
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

                            extraInfo!!.savedColors?.let {
                                val newSavedColors = ArrayList(colorPickers.brush.savedColors)
                                newSavedColors.addAll(it)
                                colorPickers.brush.setSavedColor(newSavedColors.distinct())
                            }
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

        val bindings = FdbPathImportPromptDialogBinding.inflate(LayoutInflater.from(context), null, false)
        bindings.showDrawingCb.setOnCheckedChangeListener { _, isChecked ->
            bindings.fdbDefaultDrawingIntervalTil.visibility = if (isChecked) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        bindings.pathFileTv.text = context.getString(R.string.fdb_path_import_prompt_dialog_filepath_tv, path)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.import_)
            .setNegativeAction()
            .setPositiveAction { _, _ ->
                val interval = bindings.fdbDefaultDrawingIntervalTil.editText!!.text.toString().toInt()
                paintView.isShowDrawing = bindings.showDrawingCb.isChecked
                paintView.drawingInterval = if (paintView.isShowDrawing) {
                    interval
                } else {
                    0
                }
                wm.addView(pathImportWindow, pathImportWindowLP)
                setUpPathImportWindow()
                performImporting(File(path))
            }
            .setView(bindings.root)
            .create().apply {
                DialogUtils.setDialogAttr(this, width = MATCH_PARENT, overlayWindow = true)
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
        val bindings = FdbEraserOpacityAdjustingViewBinding.inflate(LayoutInflater.from(context))
        val opacitySlider = bindings.opacity
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

        val dialog = createDialog(bindings.root, dim = false)
        DialogUtils.setDialogAttr(dialog, width = MATCH_PARENT, overlayWindow = true)
        return dialog
    }

    private fun exportPath(destFile: File) {
        // path "undo" optimization
        val bindings = ProgressBarBinding.inflate(LayoutInflater.from(context))
        val progressTitle = bindings.progressBarTitle
        val progressBar = bindings.progressBar
        val progressTV = bindings.progressTv

        val dialog = createDialog(bindings.root).apply {
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
            FileUtil.copy(pathFiles.tmpPathFile, destFile)

            val database = SQLite3.open(destFile.path)
            val pathLayerTables = database.getTables().filter { it.startsWith("path_layer_") }

            pathLayerTables.forEachIndexed { layerIndex, table ->
                PathProcessor.optimizePath(destFile, table) { progress, phase ->
                    tryDo.tryDo { _, notifier ->
                        runOnUiThread {
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
                        runOnUiThread {
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
                runOnUiThread {
                    Common.showException(e, context)
                    dialog.dismiss()
                }
            }
        }.start()
    }

    private fun hideFDB() {
        showHideNotification()

        wm.removeView(panelLL)
    }

    fun restoreFDB() {
        wm.addView(panelLL, panelLP)
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
                    paintView.drawingColor = color
                    if (followBrushColor) {
                        updatePanelColor(color)
                    }
                    ToastUtils.show(context, ColorUtils.getHexString(color, false))
                }
            }.also {
                context.applicationContext.registerReceiverCompat(it, IntentFilter().apply {
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
                context.applicationContext.registerReceiverCompat(it, IntentFilter().apply {
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
        paintView.isImportingTerminated = true
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
        val bindings = FdbTransformationSettingsViewBinding.inflate(LayoutInflater.from(context))
        val moveCB = bindings.move
        val zoomCB = bindings.zoom
        val rotateCB = bindings.rotate

        moveCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isMoveTransformationEnabled = isChecked
        }
        zoomCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isZoomTransformationEnabled = isChecked
        }
        rotateCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isRotateTransformationEnabled = isChecked
        }

        val setAsDefaultTransformation = bindings.setAsDefault
        val resetToDefaultTransformation = bindings.resetToDefault

        setAsDefaultTransformation.setOnClickListener {
            paintView.setAsDefaultTransformation()
            ToastUtils.show(context, R.string.set_done_toast)
        }
        resetToDefaultTransformation.setOnClickListener {
            paintView.resetDefaultTransformation()
            ToastUtils.show(context, R.string.reset_success_toast)
        }

        return createDialog(bindings.root)
    }

    private fun createPressureSensingSettingsDialog(): Dialog {
        val bindings = FdbPressureSensingSettingsViewBinding.inflate(LayoutInflater.from(context))
        val touchStrokeVariableCB = bindings.touchStrokeVariableCb
        val touchStrokePenPressureCoefficientSlider = bindings.touchStrokePenPressureCoefficientSlider
        val touchTransparencyVariableCB = bindings.touchTransparencyVariableCb
        val touchTransparencyPenPressureCoefficientSlider = bindings.touchTransparencyPenPressureCoefficientSlider
        val handPaintedPlateStrokeVariableCB = bindings.handPaintedPlateStrokeVariableCb
        val handPaintedPlateStrokePenPressureCoefficientSlider =
            bindings.handPaintedPlateStrokePenPressureCoefficientSlider
        val handPaintedPlateTransparencyVariableCB = bindings.handPaintedPlateTransparencyVariableCb
        val handPaintedPlateTransparencyPenPressureCoefficientSlider =
            bindings.handPaintedPlateTransparencyPenPressureCoefficientSlider
        touchStrokeVariableCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isTouchStrokeVariableEnabled = isChecked
            paintView.touchStrokePenPressureCoefficient = touchStrokePenPressureCoefficientSlider.value
        }
        touchTransparencyVariableCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isTouchTransparencyVariableEnabled = isChecked
            paintView.touchTransparencyPenPressureCoefficient = touchTransparencyPenPressureCoefficientSlider.value
        }
        handPaintedPlateStrokeVariableCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isHandPaintedPlateStrokeVariableEnabled = isChecked
            paintView.handPaintedPlateStrokePenPressureCoefficient =
                handPaintedPlateStrokePenPressureCoefficientSlider.value
        }
        handPaintedPlateTransparencyVariableCB.setOnCheckedChangeListener { _, isChecked ->
            paintView.isHandPaintedPlateTransparencyVariableEnabled = isChecked
            paintView.handPaintedPlateTransparencyPenPressureCoefficient =
                handPaintedPlateTransparencyPenPressureCoefficientSlider.value
        }
        touchStrokePenPressureCoefficientSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                paintView.touchStrokePenPressureCoefficient = value
            }
        }
        touchTransparencyPenPressureCoefficientSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                paintView.touchTransparencyPenPressureCoefficient = value
            }
        }
        handPaintedPlateStrokePenPressureCoefficientSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                paintView.handPaintedPlateStrokePenPressureCoefficient = value
            }
        }
        handPaintedPlateTransparencyPenPressureCoefficientSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                paintView.handPaintedPlateTransparencyPenPressureCoefficient = value
            }
        }
        return createDialog(bindings.root)
    }

    private fun getSelectedStatisticsIntData(db: SQLite3, mark: Int): Int {
        return db.getRowCount("SELECT COUNT() FROM path WHERE mark is ?", arrayOf(mark))
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

    @Suppress("ClickableViewAccessibility")
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
            stopButton.setOnClickListener {
                paintView.isImportingTerminated = true
            }
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

            // when the touching time exceeds this value, `drawingInterval` will speed up its increasing/decreasing
            val longClickDelay = 500L
            val createButtonTouchHandler = { decrease: Boolean ->
                var count = 1
                var runnable: Runnable? = null
                val handler = Handler(Looper.getMainLooper());

                { v: View, event: MotionEvent ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            runnable = object : Runnable {
                                override fun run() {
                                    if (decrease) {
                                        if (drawingInterval >= drawingIntervalStep) {
                                            drawingInterval -= drawingIntervalStep
                                        }
                                    } else {
                                        drawingInterval += drawingIntervalStep
                                    }
                                    updateDrawingInterval()
                                    if (count == 1) {
                                        defaultVibrator().oneShotVibrate(55)
                                    }
                                    ++count
                                    val interval =
                                        450 / (1 + exp(3 + (1.6 * count - 10))) + 50
                                    handler.postDelayed(this, interval.toLong())
                                }
                            }
                            handler.postDelayed(runnable!!, longClickDelay)
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            runnable?.let { handler.removeCallbacks(it) }
                            runnable = null
                            count = 1

                            if (event.action == MotionEvent.ACTION_UP && event.eventTime - event.downTime < ViewConfiguration.getTapTimeout()) {
                                v.performClick()
                            }
                        }
                    }
                    Unit
                }
            }

            val buttonTouchHandler = object {
                val add = createButtonTouchHandler(false)
                val minus = createButtonTouchHandler(true)
            }

            addBtn.setOnTouchListener { v, event ->
                buttonTouchHandler.add(v, event)
                true
            }

            minusBtn.setOnTouchListener { v, event ->
                buttonTouchHandler.minus(v, event)
                true
            }

            speedTv.setOnClickListener {
                createPromptDialog(R.string.fdb_change_drawing_speed) { _, et ->
                    try {
                        drawingInterval = et.text.toString().toInt()
                        if (drawingInterval < 0) {
                            drawingInterval = 0
                            speedTv.text = "0"
                        }
                    } catch (e: Exception) {
                        ToastUtils.show(context, R.string.please_enter_correct_value_toast)
                        return@createPromptDialog
                    }
                    updateDrawingInterval()
                }.show()
            }

            if (paintView.isShowDrawing) {
                speedTv.visibility = View.VISIBLE
                pauseMinusButtonGroup.visibility = View.VISIBLE
            } else {
                speedTv.visibility = View.GONE
                pauseMinusButtonGroup.visibility = View.GONE
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpLayerManagerWindow() {
        val bindings = FdbLayerManagerViewBinding.bind(layerManagerView.getView())
        bindings.dragIcon.setOnTouchListener { v, event ->
            layerManagerViewPositionUpdater.onTouch(v, event, false)
        }
        val updateLayerStates = {
            paintView.updateLayerState(layerManagerView.getLayerState())
        }
        layerManagerView.apply {
            onNameChangedListener = { updateLayerStates() }
            onVisibilityChangedListener = { updateLayerStates() }
            onCheckedListener = { updateLayerStates() }
            onLayerOrderChangedListener = updateLayerStates
            onDeleteNotifier = { notifier ->
                createConfirmationDialog({ _, _ ->
                    notifier.delete()
                }, { _, _ ->
                    notifier.revert()
                }, titleRes = R.string.whether_to_delete).also {
                    it.setOnCancelListener {
                        notifier.revert()
                    }
                }.show()
            }
        }
    }
}

typealias OnExitListener = () -> Unit
