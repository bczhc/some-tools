package pers.zhc.tools.fourierseries

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fourier_series_epicycle_item.view.*
import kotlinx.android.synthetic.main.fourier_series_main.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.*
import kotlin.math.abs

/**
 * @author bczhc
 */
class FourierSeriesActivity : BaseActivity() {
    private lateinit var evaluatorSpinner: AppCompatSpinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var periodET: EditText
    private lateinit var epicycleNumET: EditText
    private lateinit var threadsNumET: EditText
    private lateinit var integralSegNumET: EditText
    private lateinit var listAdapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fourier_series_main)

        val drawButton = draw_btn!!
        val computeButton = compute_btn!!
        val startButton = start_btn!!
        val sortButton = sort_btn!!
        integralSegNumET = integral_fragment_number!!.editText
        threadsNumET = threads_num!!.editText
        epicycleNumET = epicycles_number!!.editText
        periodET = period!!.editText
        recyclerView = recycler_view!!
        evaluatorSpinner = evaluator_spinner!!

        listAdapter = ListAdapter(this, epicycleData)
        recyclerView.adapter = listAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        configSpinner()

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
                        // no reach
                        throw RuntimeException()
                    }
                }
                epicycleData.sortWith(comparator)
                listAdapter.notifyDataSetChanged()
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun configSpinner() {
        val spinnerAdapter = object : ArrayAdapter<PathEvaluator>(
            this, android.R.layout.simple_list_item_1, arrayOf(
                PathEvaluator.LINEAR,
                PathEvaluator.TIME
            )
        ) {
            private fun setMyView(view: View, position: Int) {
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)!!.toString(context)
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getView(position, convertView, parent).also {
                    setMyView(it, position)
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getDropDownView(position, convertView, parent).also {
                    setMyView(it, position)
                }
            }
        }
        evaluatorSpinner.adapter = spinnerAdapter
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
                (evaluatorSpinner.selectedItem as PathEvaluator).enumInt
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
}

typealias Epicycles = ArrayList<Epicycle>
