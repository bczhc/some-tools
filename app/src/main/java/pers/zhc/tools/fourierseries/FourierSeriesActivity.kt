package pers.zhc.tools.fourierseries

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.android.synthetic.main.fourier_series_epicycle_item.view.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.FourierSeriesMainBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.*
import kotlin.math.abs

/**
 * @author bczhc
 */
class FourierSeriesActivity : BaseActivity() {
    private lateinit var floatTypeMenu: MaterialAutoCompleteTextView
    private lateinit var integratorMenu: MaterialAutoCompleteTextView
    private lateinit var pathEvaluatorMenu: MaterialAutoCompleteTextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var periodET: EditText
    private lateinit var epicycleNumET: EditText
    private lateinit var threadsNumET: EditText
    private lateinit var integralSegNumET: EditText
    private lateinit var listAdapter: ListAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = FourierSeriesMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val drawButton = bindings.drawBtn
        val computeButton = bindings.computeBtn
        val startButton = bindings.startBtn
        val sortButton = bindings.sortBtn
        integralSegNumET = bindings.integralFragmentNumber
        threadsNumET = bindings.threadNum
        epicycleNumET = bindings.epicyclesNumber
        periodET = bindings.period
        recyclerView = bindings.recyclerView
        pathEvaluatorMenu = bindings.pathEvaluatorMenu
        integratorMenu = bindings.integratorMenu
        floatTypeMenu = bindings.floatType

        configSpinner()

        listAdapter = ListAdapter(this, epicycleData)
        recyclerView.adapter = listAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // set the default values
        @Suppress("SetTextI18n")
        run {
            integralSegNumET.setText("100000")
            threadsNumET.setText(Runtime.getRuntime().availableProcessors().toString())
            epicycleNumET.setText("100")
            periodET.setText("100")
        }

        drawButton.setOnClickListener {
            startActivity(Intent(this, DrawingActivity::class.java))
        }
        computeButton.setOnClickListener {
            showComputeDialog()
        }
        startButton.setOnClickListener {
            startActivity(Intent(this, EpicycleDrawingActivity::class.java))
        }
        sortButton.setOnClickListener {
            val menu = PopupMenuUtil.create(this, it, R.menu.fourier_series_sort_btn)
            menu.show()
            menu.setOnMenuItemClickListener { item ->

                when (item.itemId) {
                    R.id.shuffle -> {
                        epicycleData.shuffle()
                        listAdapter.notifyDataSetChanged()
                        return@setOnMenuItemClickListener true
                    }
                }

                val comparator: Comparator<Epicycle> = when (item.itemId) {
                    R.id.radius_ascent -> {
                        Comparator { o1, o2 ->
                            val radius1 = o1.radius()
                            val radius2 = o2.radius()
                            return@Comparator when {
                                radius1 < radius2 -> {
                                    -1
                                }
                                radius1 == radius2 -> {
                                    0
                                }
                                else -> {
                                    1
                                }
                            }
                        }
                    }
                    R.id.radius_descent -> {
                        Comparator { o1, o2 ->
                            val radius1 = o1.radius()
                            val radius2 = o2.radius()
                            return@Comparator when {
                                radius1 < radius2 -> {
                                    1
                                }
                                radius1 == radius2 -> {
                                    0
                                }
                                else -> {
                                    -1
                                }
                            }
                        }
                    }
                    R.id.speed_ascent -> {
                        Comparator { o1, o2 ->
                            val speed1 = abs(o1.p)
                            val speed2 = abs(o2.p)
                            return@Comparator when {
                                speed1 < speed2 -> {
                                    -1
                                }
                                speed1 == speed2 -> {
                                    0
                                }
                                else -> {
                                    1
                                }
                            }
                        }
                    }
                    R.id.speed_descent -> {
                        Comparator { o1, o2 ->
                            val speed1 = abs(o1.p)
                            val speed2 = abs(o2.p)
                            return@Comparator when {
                                speed1 < speed2 -> {
                                    1
                                }
                                speed1 == speed2 -> {
                                    0
                                }
                                else -> {
                                    -1
                                }
                            }
                        }
                    }
                    else -> {
                        unreachable()
                    }
                }
                epicycleData.sortWith(comparator)
                listAdapter.notifyDataSetChanged()
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun configSpinner() {
        pathEvaluatorMenu.apply {
            setText(PathEvaluator.TIME.textRes)
            setSimpleItems(PathEvaluator.values().map { it.toString(this@FourierSeriesActivity) }.toTypedArray())
        }
        integratorMenu.apply {
            setText(Integrator.SIMPSON.displayName)
            setSimpleItems(Integrator.values().map { it.displayName }.toTypedArray())
        }
        floatTypeMenu.apply {
            setText(FloatType.F64.display)
            setSimpleItems(FloatType.values().map { it.display }.toTypedArray())
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showComputeDialog() {
        val points = DrawingActivity.points
        if (points == null) {
            ToastUtils.show(this, R.string.fourier_series_no_curve_toast)
            return
        }

        epicycleData.clear()

        val epicycleNum = epicycleNumET.text.toString().toInt()
        val integralSegments = integralSegNumET.text.toString().toInt()
        val period = periodET.text.toString().toDouble()
        val threadsNum = threadsNumET.text.toString().toInt()

        val dialog = ProgressDialog(this).apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            DialogUtils.setDialogAttr(this, width = MATCH_PARENT)
        }
        val progressView = dialog.getProgressView().apply {
            setIsIndeterminateMode(false)
            setTitle(getString(R.string.fourier_series_computing_dialog_title))
        }
        dialog.show()

        val asyncTryDo = AsyncTryDo()
        Thread {
            val array = points.toArray(Array(0) { return@Array InputPoint(0F, 0F) })
            JNI.FourierSeries.compute(
                array,
                integralSegments,
                period,
                epicycleNum,
                threadsNum,
                // TODO: bad method to get index of selected item
                PathEvaluator.values().find { getString(it.textRes) == pathEvaluatorMenu.text.toString() }!!.enumInt,
                Integrator.values().find { it.displayName == integratorMenu.text.toString() }!!.enumInt,
                FloatType.values().find { it.display == floatTypeMenu.text.toString() }!!.enumInt
            ) { re, im, n, p ->
                val epicycle = Epicycle(n, ComplexValue(re, im), p)
                // sequential result fetching; not require mutex lock
                epicycleData.add(epicycle)
                asyncTryDo.tryDo { _, notifier ->
                    runOnUiThread {
                        progressView.setProgress(epicycleData.size.toFloat() / epicycleNum.toFloat())
                        progressView.setText(
                            getString(
                                R.string.epicycles_calc_progress,
                                epicycleData.size, epicycleNum
                            )
                        )
                    }
                    notifier.finish()
                }
            }
            runOnUiThread {
                dialog.dismiss()
                listAdapter.notifyDataSetChanged()
            }
        }.start()
    }

    class ListAdapter(private val context: Context, private val epicycleData: Epicycles) :
        RecyclerView.Adapter<ListAdapter.Holder>() {
        class Holder(val view: View) : RecyclerView.ViewHolder(view) {
            val textView = view.text_view!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val inflate = LayoutInflater.from(context).inflate(R.layout.fourier_series_epicycle_item, parent, false)
            return Holder(inflate)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.textView.text = epicycleData[position].toString()
        }

        override fun getItemCount(): Int {
            return epicycleData.size
        }
    }

    companion object {
        var epicycleData: Epicycles = Epicycles()
    }

    enum class PathEvaluator(val enumInt: Int, @StringRes val textRes: Int) {
        LINEAR(0, R.string.fourier_series_linear_path_evaluator_name),
        TIME(1, R.string.fourier_series_time_path_evaluator_name);

        fun toString(context: Context): String {
            return context.getString(textRes)
        }
    }

    enum class Integrator(val enumInt: Int, val displayName: String) {
        TRAPEZOID(0, "Trapezoid"),
        LEFT_RECTANGLE(1, "Left rectangle"),
        RIGHT_RECTANGLE(2, "Right rectangle"),
        SIMPSON(3, "Simpson's 1/3 rule"),
        SIMPSON38(4, "Simpson's 3/8 rule"),
        BOOLE(5, "Boole's rule"),
    }

    enum class FloatType(val enumInt: Int, val display: String) {
        F32(0, "f32"),
        F64(1, "f64"),
    }
}

typealias Epicycles = ArrayList<Epicycle>
