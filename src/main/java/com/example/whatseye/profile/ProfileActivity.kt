package com.example.whatseye.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.whatseye.*
import com.example.whatseye.access.LockScreenNewPINActivity
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.managers.LockManager
import com.example.whatseye.api.managers.PasskeyManager
import com.example.whatseye.dataType.data.ChildProfile
import com.example.whatseye.dataType.data.UsageData
import com.example.whatseye.dataType.db.ScheduleDataBase
import com.example.whatseye.dataType.db.UsageDatabase
import com.example.whatseye.utils.Tooltip
import com.example.whatseye.utils.saveProfileToLocal
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
class ProfileActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var textHours: TextView
    private lateinit var textDate: TextView
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var scheduleContainer: LinearLayout
    private lateinit var settingsButton: MaterialButton
    private lateinit var profile: ChildProfile

    private var selectedDate: LocalDate = LocalDate.now()
    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        RetrofitClient.initialize(this)
        validatePasskey()

        initializeViews()
        setupListeners()

        loadProfile()
        updateDateUI()
        updateSchedulesUI()
    }

    private fun initializeViews() {
        barChart = findViewById(R.id.barChart)
        textHours = findViewById(R.id.textHours)
        textDate = findViewById(R.id.textDate)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        scheduleContainer = findViewById(R.id.scheduleContainer)
        settingsButton = findViewById(R.id.settingsButton)
    }

    private fun setupListeners() {
        btnPrev.setOnClickListener {
            selectedDate = selectedDate.minusDays(1)
            updateDateUI()
        }

        btnNext.setOnClickListener {
            if (selectedDate.isBefore(LocalDate.now())) {
                selectedDate = selectedDate.plusDays(1)
                updateDateUI()
            }
        }

        settingsButton.setOnClickListener {
            val appName = applicationInfo.loadLabel(packageManager).toString()
            val intent = if (LockManager(this).getLockedStatus(appName)) {
                Intent(this, LockScreenActivity::class.java).apply {
                    putExtra("packageName", appName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(this, UpdateProfileActivity::class.java).apply {
                    putExtra("packageName", appName)
                }
            }
            startActivity(intent)
        }
    }

    private fun validatePasskey() {
        if (!PasskeyManager(this).getStatus()) {
            startActivity(Intent(this, LockScreenNewPINActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun loadProfile() {
        JwtTokenManager(this).getUserId()?.let { childId ->
            RetrofitClient.profileApi.getChildProfile(childId).enqueue(object : Callback<ChildProfile> {
                override fun onResponse(call: Call<ChildProfile>, response: Response<ChildProfile>) {
                    response.body()?.let {
                        profile = it
                        saveProfileToLocal(this@ProfileActivity, profile)
                        if (it.user.first_name.isEmpty() || it.user.last_name.isEmpty()) {
                            goToAddProfile()
                        } else {
                            updateProfile()
                        }
                    }
                }

                override fun onFailure(call: Call<ChildProfile>, t: Throwable) {
                    // TODO: Show error or retry mechanism
                }
            })
        }
    }

    private fun goToAddProfile() {
        startActivity(Intent(this, AddProfileInfo::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun updateProfile() {
        getSharedPreferences("profile_data", Context.MODE_PRIVATE).apply {
            val fullName = "${getString("first_name", "")} ${getString("last_name", "")}"
            findViewById<TextView>(R.id.textName).text = fullName
            findViewById<TextView>(R.id.textUsername).text = getString("username", "")
            findViewById<TextView>(R.id.textEmail).text = getString("email", "")
            findViewById<TextView>(R.id.textPhone).text = getString("phone_number", "")
            val photoPath = getString("photo_path", null)

            val imageView = findViewById<ShapeableImageView>(R.id.imageProfile)
            if (!photoPath.isNullOrEmpty()) {
                Glide.with(this@ProfileActivity)
                    .load(photoPath)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.rounded_button)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_user)
            }
        }
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun updateSchedulesUI() {
        val scheduleDb = ScheduleDataBase(this)
        val allSchedules = scheduleDb.getAllSchedules()
        scheduleDb.close()

        scheduleContainer.removeAllViews()

        for (schedule in allSchedules) {
            val scheduleView = layoutInflater.inflate(R.layout.schedule_item, null)
            scheduleView.findViewById<TextView>(R.id.scheduleTime).text =
                "${LocalTime.parse(schedule.startTime).format(DateTimeFormatter.ofPattern("HH:mm"))} - ${LocalTime.parse(schedule.endTime).format(DateTimeFormatter.ofPattern("HH:mm"))}"
            scheduleView.findViewById<TextView>(R.id.scheduleName).text = schedule.name
            scheduleView.findViewById<TextView>(R.id.scheduleDates).text = "Du: ${schedule.startDate} Au: ${schedule.endDate}"

            val scheduleDaysContainer = scheduleView.findViewById<FlexboxLayout>(R.id.scheduleDaysContainer)
            scheduleDaysContainer.removeAllViews()

            for (day in schedule.days) {
                val dayView = layoutInflater.inflate(R.layout.day_item, scheduleDaysContainer, false) as TextView
                dayView.text = getDayName(day)
                scheduleDaysContainer.addView(dayView)
            }

            scheduleContainer.addView(scheduleView)
        }
    }

    private fun getDayName(day: Int) = when (day) {
        1 -> "Dim"
        2 -> "Lun"
        3 -> "Mar"
        4 -> "Mer"
        5 -> "Jeu"
        6 -> "Ven"
        7 -> "Sam"
        else -> ""
    }

    private fun updateDateUI() {
        val dbHelper = UsageDatabase(this)
        val dateForDb = selectedDate.format(dbDateFormatter)
        val usageData = dbHelper.getUsageDataForDate(dateForDb)
        dbHelper.close()

        drawBarChart(usageData)

        textDate.text = selectedDate.format(displayDateFormatter).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
        }

        val totalMinutes = (usageData.sumOf { it.usage_seconds } / 60.0).toInt()
        textHours.text = when {
            totalMinutes >= 60 -> "Total : ${totalMinutes / 60} heure(s) ${totalMinutes % 60} min"
            totalMinutes > 0 -> "Total : $totalMinutes min"
            else -> "Aucune Activité"
        }
    }

    private fun drawBarChart(usageData: List<UsageData>) {
        val entries = (0..23).map { hour ->
            val minutes = (usageData.find { it.hour == hour }?.usage_seconds ?: 0) / 60f
            BarEntry(hour.toFloat(), if (minutes > 0f) minutes else 0f)
        }

        val dataSet = BarDataSet(entries, "Utilisation (minutes)").apply {
            color = Color.parseColor("#3F51B5")
            setDrawValues(false)
        }

        barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.8f }
            description.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            setScaleEnabled(false)
            animateY(600)
            setFitBars(true)
            marker = Tooltip(context, R.layout.tooltip_marker).apply { chartView = barChart }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.DKGRAY
                textSize = 12f
                axisLineColor = Color.LTGRAY
                labelCount = 24
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) =
                        if (value.toInt() % 3 == 0) value.toInt().toString() else ""
                }
            }

            axisLeft.apply {
                setDrawLabels(false)
                setDrawAxisLine(false)
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }

            axisRight.isEnabled = false
            invalidate()
        }
    }
}
