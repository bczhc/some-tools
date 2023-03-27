package pers.zhc.tools.coursetable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEntity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.Info
import pers.zhc.tools.MyApplication.Companion.GSON
import pers.zhc.tools.MyApplication.Companion.HTTP_CLIENT_DEFAULT
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CourseTableCourseDetailDialogBinding
import pers.zhc.tools.databinding.CourseTableMainBinding
import pers.zhc.tools.utils.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class CourseTableMainActivity : BaseActivity() {
    private lateinit var bindings: CourseTableMainBinding
    private lateinit var currentFirstDay: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = CourseTableMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        showDialogAndFetch { body ->
            val fallInvalidData = {
                ToastUtils.show(this, R.string.invalid_response_toast)
            }
            val response = GSON.fromJsonOrNull(body, Response::class.java)
            if (response == null || response.status != 0) {
                fallInvalidData()
                return@showDialogAndFetch
            }

            val fetchTimeDate = Date(response.data.fetchedTime)
            if (!fetchTimeDate.isToday()) {
                val fetchTimeString = fetchTimeDate.format("yyyy-MM-dd")
                ToastUtils.show(this, getString(R.string.course_table_data_not_latest_toast, fetchTimeString))
                return@showDialogAndFetch
            }

            val courseData = response.data.courseTable
            if (courseData.isEmpty) {
                fallInvalidData()
                return@showDialogAndFetch
            }

            showCourseTable(courseData)
        }
    }

    private fun showCourseTable(courseData: JsonArray) {
        val events = courseData.flatMap {
            @Suppress("SpellCheckingInspection")
            it.asJsonObject["data"].asJsonObject["datas"].asJsonArray
        }.map {
            val obj = it.asJsonObject
            val courseTime = parseCourseTime(obj["RQ"].asString, obj["JC"].asString)
            Event(
                obj["JSXM"].asString,
                obj["SKDD"].asString,
                obj["KCMC"].asString,
                courseTime.first,
                courseTime.second
            )
        }

        val weekView = bindings.weekView
        val timetableAdapter = TimetableAdapter()
        weekView.apply {
            adapter = timetableAdapter
            setDateFormatter {
                val date = it.time
                val weekdayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
                val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
                val weekdayLabel = weekdayFormatter.format(date)
                val dateLabel = dateFormatter.format(date)
                weekdayLabel + "\n" + dateLabel
            }
        }
        timetableAdapter.submitList(events)

        currentFirstDay = getThisWeekFirstDay()

        val navigateDay = { delta: Int ->
            currentFirstDay.add(Calendar.DAY_OF_YEAR, delta)
            weekView.scrollToDate(currentFirstDay)
        }
        bindings.leftNavigationButton.setOnClickListener {
            navigateDay(-7)
        }
        bindings.rightNavigationButton.setOnClickListener {
            navigateDay(7)
        }

        weekView.scrollToDate(getThisWeekFirstDay())

        updateDateRangeTV(getThisWeekFirstDay(), getThisWeekFirstDay().also {
            it.add(Calendar.DAY_OF_YEAR, 6)
        })
    }

    private fun showDialogAndFetch(onFetched: (body: String) -> Unit) {
        val progressDialog = ProgressDialog(this)
        progressDialog.getProgressView().apply {
            setIsIndeterminateMode(true)
            setTitle(getString(R.string.network_fetching))
        }
        progressDialog.show()

        val serverRootURL = Info.serverRootURL

        lifecycleScope.launch(Dispatchers.IO) {
            val response = runCatching {
                HTTP_CLIENT_DEFAULT.get("$serverRootURL/ccit-info").bodyAsText()
            }
            withContext(Dispatchers.Main) {
                if (response.isSuccess) {
                    onFetched(response.getOrNull()!!)
                } else {
                    ToastUtils.showException(this@CourseTableMainActivity, response.exceptionOrNull()!!)
                }
                progressDialog.dismiss()
            }
        }
    }

    private val courseTimeMap by lazy {
        buildMap {
            val pair = { h: Int, m: Int ->
                Pair(h, m)
            }
            put(1, pair(8, 20))
            put(2, pair(9, 10))
            put(3, pair(10, 10))
            put(4, pair(11, 0))
            put(5, pair(13, 20))
            put(6, pair(14, 10))
            put(7, pair(15, 10))
            put(8, pair(16, 0))
            put(9, pair(18, 0))
            put(10, pair(18, 40))
            put(11, pair(19, 30))
            put(12, pair(20, 10))
        }
    }

    private fun buildDateRangeText(startDate: Calendar, endDate: Calendar): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedFirstDay = dateFormatter.format(startDate.time)
        val formattedLastDay = dateFormatter.format(endDate.time)
        return "$formattedFirstDay 至 $formattedLastDay"
    }

    private fun updateDateRangeTV(firstVisibleDate: Calendar, lastVisibleDate: Calendar) {
        bindings.dateRangeTextView.text = buildDateRangeText(firstVisibleDate, lastVisibleDate)
    }

    private fun parseCourseTime(dateString: String, courseNo: String): Pair<Calendar, Calendar> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString)!!

        val split = courseNo.split("-")

        val firstCourseTime = courseTimeMap[split[0].toInt()]!!
        val lastCourseTime = courseTimeMap[split[1].toInt()]!!

        val startTime = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, firstCourseTime.first)
            set(Calendar.MINUTE, firstCourseTime.second)
        }
        val endTime = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, lastCourseTime.first)
            set(Calendar.MINUTE, lastCourseTime.second)
            add(Calendar.MINUTE, 40)
        }

        return Pair(
            startTime,
            endTime
        )
    }

    data class Data(
        val courseTable: JsonArray,
        val fetchedTime: Long,
    )

    data class Response(
        val status: Int,
        val data: Data,
    )

    data class Event(
        val teacher: String,
        val location: String,
        val subject: String,
        val start: Calendar,
        val end: Calendar,
    )

    private fun getThisWeekFirstDay(): Calendar {
        return Calendar.getInstance().apply {
            time = Date()
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, -6)
            } else {
                add(Calendar.DAY_OF_YEAR, -(dayOfWeek - Calendar.MONDAY))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.course_table_action_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.today -> {
                currentFirstDay = getThisWeekFirstDay().also {
                    bindings.weekView.scrollToDate(it)
                }
                updateDateRangeTV(getThisWeekFirstDay(), getThisWeekFirstDay().also {
                    it.add(Calendar.DAY_OF_YEAR, 6)
                })
            }

            else -> unreachable()
        }
        return true
    }

    inner class TimetableAdapter : WeekView.SimpleAdapter<Event>() {
        private val id = AtomicLong(0)

        override fun onCreateEntity(item: Event): WeekViewEntity {
            return WeekViewEntity.Event.Builder(item)
                .setId(id.getAndIncrement())
                .setStartTime(item.start)
                .setEndTime(item.end)
                .setTitle(item.subject)
                .setSubtitle(item.location)
                .build()
        }

        override fun onRangeChanged(firstVisibleDate: Calendar, lastVisibleDate: Calendar) {
            super.onRangeChanged(firstVisibleDate, lastVisibleDate)
            updateDateRangeTV(firstVisibleDate, lastVisibleDate)
        }

        override fun onEventClick(data: Event) {
            super.onEventClick(data)

            val bindings = CourseTableCourseDetailDialogBinding.inflate(LayoutInflater.from(context))
            bindings.nameTv.text = context.getString(R.string.course_table_course_name_tv, data.subject)
            bindings.locationTv.text = context.getString(R.string.course_table_course_location_tv, data.location)
            bindings.teacherTv.text = context.getString(R.string.course_table_course_teacher_tv, data.teacher)
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(data.start.time)
            bindings.dateTv.text = context.getString(R.string.course_table_course_date_tv, dateString)
            val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(data.start.time) + "-" +
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(data.end.time)
            bindings.timeTv.text = context.getString(R.string.course_table_course_time_tv, timeString)

            MaterialAlertDialogBuilder(context)
                .setView(bindings.root)
                .show()
        }
    }
}
