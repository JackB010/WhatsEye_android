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
import com.example.whatseye.LockScreenActivity
import com.example.whatseye.R
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
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
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
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var textDate: TextView
    private lateinit var scheduleContainer: LinearLayout
    private lateinit var settingsButton: MaterialButton
    private lateinit var profile : ChildProfile


    private var selectedDate: LocalDate = LocalDate.now()
    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        RetrofitClient.initialize(this)
        val childId = JwtTokenManager(this).getUserId()
        if (childId != null) {
            RetrofitClient.profileApi.getChildProfile(childId)
                .enqueue(object : Callback<ChildProfile> {
                    override fun onResponse(call: Call<ChildProfile>, response: Response<ChildProfile>) {
                        if (response.isSuccessful) {
                            profile = response.body()!!
                            saveProfileToLocal(this@ProfileActivity, profile)
                            if (profile.user.last_name.isEmpty() || profile.user.first_name.isEmpty()){
                                goAdd()
                            }
                        }
                    }
                    override fun onFailure(call: Call<ChildProfile>, t: Throwable) {

                    }
                })


            if(!PasskeyManager(this).getStatus()){
                val intent2 = Intent(this, LockScreenNewPINActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent2)
            }
        }

        barChart = findViewById(R.id.barChart)
        textHours = findViewById(R.id.textHours)
        textDate = findViewById(R.id.textDate)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        scheduleContainer = findViewById(R.id.scheduleContainer)
        settingsButton = findViewById(R.id.settingsButton)


        updateDateUI()
        updateSchedulesUI()
        updateProfile()


        btnPrev.setOnClickListener {
            selectedDate = selectedDate.minusDays(1)
            updateDateUI()
        }

        btnNext.setOnClickListener {
            val today = LocalDate.now()
            if (selectedDate.isBefore(today)) {
                selectedDate = selectedDate.plusDays(1)
                updateDateUI()
            }
        }


        settingsButton.setOnClickListener {
            val appName = applicationInfo.loadLabel(packageManager).toString()
            val isLocked = LockManager(this).getLockedStatus(appName)
            if(isLocked) {
                val intent = Intent(this, LockScreenActivity::class.java).apply {
                    putExtra("packageName", appName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }else {
                val intent2 = Intent(this, UpdateProfileActivity::class.java).apply {
                    putExtra("packageName", appName)
                }
                startActivity(intent2)
            }

        }

    }
    private fun goAdd(){
        val intent2 = Intent(this, AddProfileInfo::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent2)
        finish()
    }
    private fun updateProfile(){
        val sharedPref = getSharedPreferences("profile_data", Context.MODE_PRIVATE)

        val firstName = sharedPref.getString("first_name", "") ?: ""
        val lastName = sharedPref.getString("last_name", "") ?: ""
        val username = sharedPref.getString("username", "") ?: ""
        val email = sharedPref.getString("email", "") ?: ""
        val phoneNumber = sharedPref.getString("phone_number", "") ?: ""
        val photoPath = sharedPref.getString("photo_path", null)

        val fullName = "$firstName $lastName"

// Set text views
        findViewById<TextView>(R.id.textName).text = fullName
        findViewById<TextView>(R.id.textUsername).text = username
        findViewById<TextView>(R.id.textEmail).text = email
        findViewById<TextView>(R.id.textPhone).text = phoneNumber
        val imageView = findViewById<ShapeableImageView>(R.id.imageProfile)
        if (!photoPath.isNullOrEmpty()) {
            Glide.with(imageView)
                .load(photoPath)
                .placeholder(R.drawable.ic_user) // fallback image
                .error(R.drawable.rounded_button)          // in case of load error
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_user)
        }

    }
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun updateSchedulesUI() {
        val scheduleDb = ScheduleDataBase(this)
        val allSchedules = scheduleDb.getAllSchedules()

        scheduleContainer.removeAllViews()

        for (schedule in allSchedules) {
            val scheduleView = layoutInflater.inflate(R.layout.schedule_item, null)

            val scheduleTime = scheduleView.findViewById<TextView>(R.id.scheduleTime)
            val scheduleName = scheduleView.findViewById<TextView>(R.id.scheduleName)
            val scheduleDates = scheduleView.findViewById<TextView>(R.id.scheduleDates)
            val scheduleDaysContainer = scheduleView.findViewById<FlexboxLayout>(R.id.scheduleDaysContainer)

            // Set schedule times and name
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val timeS = LocalTime.parse(schedule.startTime).format(formatter)
            val timeE = LocalTime.parse(schedule.endTime).format(formatter)
            scheduleTime.text = "$timeS - $timeE"
            scheduleName.text = schedule.name
            scheduleDates.text = "Du: ${schedule.startDate} Au: ${schedule.endDate}"

            // Clear and populate day tags
            scheduleDaysContainer.removeAllViews()
            for (day in schedule.days) {
                val dayText = getDayName(day)
                val dayView = layoutInflater.inflate(R.layout.day_item, scheduleDaysContainer, false) as TextView
                dayView.text = dayText
                scheduleDaysContainer.addView(dayView)
            }

            // Click handling (optional)
            scheduleView.setOnClickListener {
                // Handle click if needed
            }

            scheduleContainer.addView(scheduleView)
        }

        // Close DB after usage
        scheduleDb.close()
    }


    // Helper function to get the name of the day
    private fun getDayName(dayValue: Int): String {
        return when (dayValue) {
            1 -> "Dim"    // Sunday
            2 -> "Lun"       // Monday
            3 -> "Mar"       // Tuesday
            4 -> "Mer"    // Wednesday
            5 -> "Jeu"       // Thursday
            6 -> "Ven"    // Friday
            7 -> "Sam"      // Saturday
            else -> ""  // Unknown
        }
    }

    private fun updateDateUI() {
        val dbHelper = UsageDatabase(this)
        val formattedDisplayDate = selectedDate.format(displayDateFormatter)
        val dateForDb = selectedDate.format(dbDateFormatter)
        val usageData = dbHelper.getUsageDataForDate(dateForDb)

        drawBarChart(barChart, usageData)

        textDate.text = formattedDisplayDate.capitalize(Locale.FRENCH)
        val totalMinutes = Math.round(usageData.sumOf { it.usage_seconds }.toDouble() / 60)
        textHours.text = if (totalMinutes > 0) {
            if (totalMinutes >= 60)
                "Total : ${totalMinutes / 60} heure(s) ${totalMinutes % 60} min"
            else
                "Total : $totalMinutes min"
        } else {
            "Aucune Activité"
        }
        dbHelper.close()

    }

    private fun drawBarChart(barChart: BarChart, usageData: List<UsageData>) {
        val fullDayUsage = (0..23).map { hour ->
            val usageSeconds = usageData.find { it.hour == hour }?.usage_seconds ?: 0
            val usageMinutes = usageSeconds.toFloat() / 60f
            BarEntry(hour.toFloat(), if (usageMinutes == 0f) 0.0f else usageMinutes) // min visible bar
        }

        val dataSet = BarDataSet(fullDayUsage, "Utilisation (minutes)").apply {
            color = Color.parseColor("#3F51B5")
            setDrawValues(false) // shows values above bars
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.8f
        barChart.data = barData

        // X-axis config
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.textColor = Color.DKGRAY
        xAxis.textSize = 12f
        xAxis.axisLineColor = Color.LTGRAY
        xAxis.labelCount = 24
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (val hour = value.toInt()) {
                    0, 3, 6, 9, 12, 15, 18, 21 -> hour.toString()
                    else -> ""
                }
            }
        }

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawLabels(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawGridLines(true)
        leftAxis.textSize = 12f
        leftAxis.gridColor = Color.LTGRAY

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setFitBars(true)
        barChart.setTouchEnabled(true)
        barChart.setScaleEnabled(false)
        barChart.animateY(600)

        val tooltip = Tooltip(barChart.context, R.layout.tooltip_marker)
        tooltip.chartView = barChart
        barChart.marker = tooltip

        barChart.invalidate()
    }

}
