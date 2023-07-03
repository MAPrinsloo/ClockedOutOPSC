package com.example.clockedout

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clockedout.databinding.ActivityProgressBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class Progress : AppCompatActivity() {
    //Array of the categories stored in the firebase.
    private var arrCat = arrayListOf<String>()

    //Will be primarily used to populate the Hours graphs.
    private var arrHours = arrayListOf<Double>()

    //Holds array of dates.
    private var arrDates = arrayListOf<String>()

    //Holds array of timesheet IDs
    private var arrTimesheetIDs = arrayListOf<String>()

    //Minimum hours goal.
    private var minGoal = 0

    //Maximum hours goal.
    private var maxGoal = 0

    //----------------------------------------------------------------------------------------//
    //On create method.
    override fun onCreate(savedInstanceState: Bundle?) {
        //Initialize firebase.
        FirebaseApp.initializeApp(this)
        val db = Firebase.firestore
        super.onCreate(savedInstanceState)
        //Binding.
        val binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            updateGoalsFromFirestore(binding)
            populateArrCat(binding, db)
        } catch (e: java.lang.IllegalArgumentException) {

        }
        //----------------------------------------------------------------------------------------//
        //Search button.
        binding.btnSearch.setOnClickListener()
        {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val startDateText = binding.etStartDate.text.toString()
                val endDateText = binding.etEndDate.text.toString()

                val startDate: Date = dateFormat.parse(startDateText)
                val endDate: Date = dateFormat.parse(endDateText)
                populateArrHours(binding, db, startDate, endDate)
            } catch (e: java.lang.IllegalArgumentException) {
                Toast.makeText(
                    this,
                    "Please ensure that all dates have been entered correctly.",
                    Toast.LENGTH_SHORT
                ).show()
                binding.etStartDate.error = "enter a date dd/MM/yyyy."
                binding.etEndDate.error = "enter a date dd/MM/yyyy."
            } catch (es: java.text.ParseException) {
                Toast.makeText(
                    this,
                    "Please ensure that all dates have been entered correctly.",
                    Toast.LENGTH_SHORT
                ).show()
                binding.etStartDate.error = "enter a date dd/MM/yyyy."
                binding.etEndDate.error = "enter a date dd/MM/yyyy."
            }

        }
    }

    //----------------------------------------------------------------------------------------//
    // Function to update minGoal and maxGoal from Firestore
    private fun updateGoalsFromFirestore(binding: ActivityProgressBinding) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocument = firestore.document("USER/TemplateUser")

        userDocument.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val data = documentSnapshot.data
                if (data != null) {
                    val minGoalValue = data["MinHours"] as? Long
                    val maxGoalValue = data["MaxHours"] as? Long

                    if (minGoalValue != null && maxGoalValue != null) {
                        minGoal = minGoalValue.toInt()
                        maxGoal = maxGoalValue.toInt()

                        binding.tvMinGoal.text = "Min Goal: $minGoal hours"
                        binding.tvMaxGoal.text = "Max Goal: $maxGoal hours"
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("Firestore", "Failed to retrieve goals", exception)
        }
    }

    //----------------------------------------------------------------------------------------//
    //Populates the array used by the graphs.
    fun populateGraphArrays(binding: ActivityProgressBinding, db: FirebaseFirestore) {
        try {
            val dates = getDatesInCurrentMonth()
            val stringToDateFormatter = SimpleDateFormat("dd/MM/yyyy")
            arrHours.clear()
            if (arrCat.size > 0) {
                for (date in dates) {
                    for (category in arrCat) {
                        val formattedDate = stringToDateFormatter.parse(date.toString()) as Date
                        val firebaseDate: com.google.firebase.Timestamp =
                            convertToFirebaseTimestamp(formattedDate)
                        checkCategoriesForTimesheets(binding, db, firebaseDate, category)
                    }
                }
            }
        } catch (e: java.lang.IllegalArgumentException) {

        }
    }

    //----------------------------------------------------------------------------------------//
    //Checks a specific category for all timesheets & if they match
    private fun checkCategoriesForTimesheets(
        binding: ActivityProgressBinding,
        db: FirebaseFirestore,
        date: com.google.firebase.Timestamp,
        selectedCategory: String
    ) {
        try {
            val timesheetCollection =
                db.collection("USER/TemplateUser/TemplateUser_CATEGORIES/$selectedCategory/TIMESHEET")
            timesheetCollection.get().addOnSuccessListener { querySnapshot ->
                arrTimesheetIDs.clear()

                //Populates array with timesheet IDs
                for (document in querySnapshot) {
                    arrTimesheetIDs.add(document.id)
                }

                //For loop iterating through all timesheets.
                for (timesheetId in arrTimesheetIDs) {
                    val timesheetIdRef = timesheetCollection.document(timesheetId)

                    timesheetIdRef.get().addOnSuccessListener { documentSnapshot ->
                        val document = documentSnapshot.data
                        if (document != null) {
                            val dateTimestamp = document["Date"] as? com.google.firebase.Timestamp
                            val hoursStudied = document["HoursStudied"] as? Long

                            if (dateTimestamp != null && hoursStudied != null) {
                                val dateValue = formatDate(dateTimestamp)
                                val hoursStudiedValue = hoursStudied.toDouble()
                                arrHours.add(hoursStudiedValue)
                            }
                        }
                    }
                }
            }
        } catch (e: java.lang.IllegalArgumentException) {

        }
    }

    //----------------------------------------------------------------------------------------//
    //Populates arrHours and outputs to the both graphs.
    private fun populateArrHours(binding: ActivityProgressBinding, db: FirebaseFirestore) {
        try {
            arrHours.clear()
            arrDates.clear()
            for (category in arrCat) {
                val timesheetCollection =
                    db.collection("USER/TemplateUser/TemplateUser_CATEGORIES/$category/TIMESHEET")
                timesheetCollection.get().addOnSuccessListener { querySnapshot ->
                    arrTimesheetIDs.clear()

                    for (document in querySnapshot) {
                        arrTimesheetIDs.add(document.id)
                    }

                    // For loop iterating through all timesheets
                    for (timesheetId in arrTimesheetIDs) {
                        val timesheetIdRef = timesheetCollection.document(timesheetId)

                        timesheetIdRef.get().addOnSuccessListener { documentSnapshot ->
                            val document = documentSnapshot.data
                            if (document != null) {
                                val dateTimestamp =
                                    document["Date"] as? com.google.firebase.Timestamp
                                val hoursStudied = document["HoursStudied"] as? Long

                                if (dateTimestamp != null && hoursStudied != null) {
                                    val dateValue = formatDate(dateTimestamp)
                                    val hoursStudiedValue = hoursStudied.toDouble()
                                    arrHours.add(hoursStudiedValue)
                                    arrDates.add(dateValue)
                                }
                            }
                            //----------------------------------------------------------------------------------------//
                            //Outputting to the hours graph.
                            val dates = getDatesInCurrentMonth()
                            val labelFormat = DecimalFormat("0")
                            val hoursSeries = LineGraphSeries(arrayOf())
                            var counter = 1.0
                            var arrCounter = 0
                            var entry = 0.0
                            var entryFound = false
                            for (date in dates) {
                                entryFound = false
                                arrCounter = 0
                                for (arrEntry in arrDates) {
                                    if (arrEntry.equals(date)) {
                                        entryFound = true
                                    }
                                }
                                if (entryFound == true) {
                                    hoursSeries.appendData(
                                        DataPoint(counter, arrHours[arrCounter]),
                                        true,
                                        31
                                    )
                                    counter++
                                    arrCounter++
                                } else {
                                    hoursSeries.appendData(DataPoint(counter, 0.0), true, 31)
                                    counter++
                                }
                            }
                            binding.gvHours.gridLabelRenderer.horizontalAxisTitle = "Days"
                            binding.gvHours.gridLabelRenderer.verticalAxisTitle = "Hours studied"
                            binding.gvHours.removeAllSeries()
                            binding.gvHours.addSeries(hoursSeries)
                            binding.gvHours.viewport.isYAxisBoundsManual = true
                            // x-axis label formatting.
                            binding.gvHours.viewport.setMinX(0.0)
                            binding.gvHours.viewport.setMaxX(31.0)
                            binding.gvHours.viewport.isScalable = true
                            binding.gvHours.gridLabelRenderer.labelFormatter =
                                object : DefaultLabelFormatter() {
                                    override fun formatLabel(
                                        value: Double,
                                        isValueX: Boolean
                                    ): String {
                                        return if (isValueX) {
                                            labelFormat.format(value) // Format x-axis labels as numbers
                                        } else {
                                            labelFormat.format(value) // Format y-axis labels as numbers
                                        }
                                    }
                                }

                            // y-axis label formatting.
                            binding.gvHours.viewport.setMinY(0.0)
                            binding.gvHours.viewport.setMaxY(8.0)
                            binding.gvHours.gridLabelRenderer.numVerticalLabels = 3
                            //==//
                            //----------------------------------------------------------------------------------------//
                            //Outputting to the performance graph
                            val categoryLabels = mapOf(
                                0.0 to GoalCategory.GOALS_MISSED,
                                1.0 to GoalCategory.NO_WORK_DONE,
                                2.0 to GoalCategory.GOALS_ACHIEVED
                            )
                            val performanceSeries = LineGraphSeries(arrayOf())
                            counter = 1.0
                            arrCounter = 0
                            entry = 0.0
                            for (date in dates) {
                                entryFound = false
                                arrCounter = 0
                                for (arrEntry in arrDates) {
                                    if (arrEntry.equals(date)) {
                                        entryFound = true
                                    }
                                    arrCounter++
                                }
                                if (entryFound == true) {
                                    if (arrHours[arrCounter] >= minGoal && arrHours[arrCounter] <= maxGoal) {
                                        performanceSeries.appendData(
                                            DataPoint(counter, 2.0),
                                            true,
                                            31
                                        )
                                        counter++
                                    } else {
                                        performanceSeries.appendData(
                                            DataPoint(counter, 0.0),
                                            true,
                                            31
                                        )
                                        counter++
                                    }
                                } else {
                                    performanceSeries.appendData(DataPoint(counter, 1.0), true, 31)
                                    counter++
                                }
                            }
                            binding.gvPerformance.gridLabelRenderer.horizontalAxisTitle =
                                "Days in current month"
                            binding.gvPerformance.removeAllSeries()
                            binding.gvPerformance.addSeries(performanceSeries)
                            binding.gvPerformance.viewport.isYAxisBoundsManual = true
                            //x-axis label formatting.
                            binding.gvPerformance.viewport.setMinX(0.0)
                            binding.gvPerformance.viewport.setMaxX(31.0)
                            binding.gvPerformance.viewport.isScalable = true
                            binding.gvPerformance.gridLabelRenderer.labelFormatter =
                                object : DefaultLabelFormatter() {
                                    override fun formatLabel(
                                        value: Double,
                                        isValueX: Boolean
                                    ): String {
                                        return if (isValueX) {
                                            labelFormat.format(value) //Format x-axis labels as numbers.
                                        } else {
                                            //Mapping the y-axis value to the labels.
                                            val category =
                                                categoryLabels[value] ?: return super.formatLabel(
                                                    value,
                                                    isValueX
                                                )
                                            category.name //Use the category name as the label.
                                        }
                                    }
                                }
                            //y-axis label formatting.
                            binding.gvPerformance.viewport.setMinY(0.0)
                            binding.gvPerformance.viewport.setMaxY(2.0)
                            binding.gvPerformance.gridLabelRenderer.numVerticalLabels = 3
                            binding.gvPerformance.gridLabelRenderer.labelFormatter =
                                object : DefaultLabelFormatter() {
                                    override fun formatLabel(
                                        value: Double,
                                        isValueX: Boolean
                                    ): String {
                                        return if (isValueX) {
                                            labelFormat.format(value) //Format x-axis labels as numbers.
                                        } else {
                                            //Mapping the x-axis value to the labels.
                                            val category = categoryLabels[value]
                                            category?.name ?: labelFormat.format(value)
                                        }
                                    }
                                }
                            //==//
                        }.addOnFailureListener { exception ->
                            Log.d(ContentValues.TAG, "get failed with ", exception)
                        }
                    }
                }
            }
        } catch (e: java.lang.IllegalArgumentException) {

        }
    }

    //----------------------------------------------------------------------------------------//
    //Populates arrHours and outputs to the hours graph while applying search date range.
    private fun populateArrHours(
        binding: ActivityProgressBinding,
        db: FirebaseFirestore,
        startDate: Date,
        endDate: Date
    ) {
        try {
            arrHours.clear()
            arrDates.clear()
            for (category in arrCat) {
                val timesheetCollection =
                    db.collection("USER/TemplateUser/TemplateUser_CATEGORIES/$category/TIMESHEET")
                timesheetCollection.get().addOnSuccessListener { querySnapshot ->
                    arrTimesheetIDs.clear()

                    for (document in querySnapshot) {
                        arrTimesheetIDs.add(document.id)
                    }

                    for (timesheetId in arrTimesheetIDs) {
                        val timesheetIdRef = timesheetCollection.document(timesheetId)

                        timesheetIdRef.get().addOnSuccessListener { documentSnapshot ->
                            val document = documentSnapshot.data
                            if (document != null) {
                                val dateTimestamp =
                                    document["Date"] as? com.google.firebase.Timestamp
                                val hoursStudied = document["HoursStudied"] as? Long

                                if (dateTimestamp != null && hoursStudied != null) {
                                    val dateValue = formatDate(dateTimestamp)
                                    val hoursStudiedValue = hoursStudied.toDouble()
                                    arrHours.add(hoursStudiedValue)
                                    arrDates.add(dateValue)
                                }
                            }
                            //----------------------------------------------------------------------------------------//
                            //Extracts numeric day of of dates
                            val dates = getDateRange(startDate, endDate)
                            val numericDays = dates.map { date ->
                                val calendar = Calendar.getInstance()
                                calendar.time = date
                                calendar.get(Calendar.DAY_OF_MONTH)
                            }
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val labelFormat = DecimalFormat("0")
                            val hoursSeries = LineGraphSeries(arrayOf())
                            var counter = 0
                            var arrCounter = 0
                            var entry = 0.0
                            var entryFound = false
                            for (date in dates) {
                                var formattedDate = dateFormat.format(date)
                                entryFound = false
                                arrCounter = 0
                                for (arrEntry in arrDates) {
                                    if (arrEntry.equals(formattedDate)) {
                                        entryFound = true
                                    }
                                }
                                if (entryFound == true) {
                                    hoursSeries.appendData(
                                        DataPoint(
                                            numericDays[counter].toDouble(),
                                            arrHours[arrCounter]
                                        ), true, numericDays.size
                                    )
                                    counter++
                                    arrCounter++
                                } else {
                                    hoursSeries.appendData(
                                        DataPoint(
                                            numericDays[counter].toDouble(),
                                            0.0
                                        ), true, numericDays.size
                                    )
                                    counter++
                                }
                            }
                            binding.gvHours.gridLabelRenderer.horizontalAxisTitle = "Days"
                            binding.gvHours.gridLabelRenderer.verticalAxisTitle = "Hours studied"
                            binding.gvHours.removeAllSeries()
                            binding.gvHours.addSeries(hoursSeries)
                            binding.gvHours.viewport.isYAxisBoundsManual = true
                            // x-axis label formatting.
                            binding.gvHours.viewport.setMinX(0.0)
                            binding.gvHours.viewport.setMaxX(31.0)
                            binding.gvHours.viewport.isScalable = true
                            binding.gvHours.gridLabelRenderer.labelFormatter =
                                object : DefaultLabelFormatter() {
                                    override fun formatLabel(
                                        value: Double,
                                        isValueX: Boolean
                                    ): String {
                                        return if (isValueX) {
                                            labelFormat.format(value) // Format x-axis labels as numbers
                                        } else {
                                            labelFormat.format(value) // Format y-axis labels as numbers
                                        }
                                    }
                                }

                            // y-axis label formatting.
                            binding.gvHours.viewport.setMinY(0.0)
                            binding.gvHours.viewport.setMaxY(8.0)
                            binding.gvHours.gridLabelRenderer.numVerticalLabels = 3
                            //==//
                        }.addOnFailureListener { exception ->
                            Log.d(ContentValues.TAG, "get failed with ", exception)
                        }
                    }
                }
            }
        } catch (e: java.lang.IllegalArgumentException) {

        } catch (es: java.lang.IndexOutOfBoundsException) {
            Toast.makeText(this, "$es", Toast.LENGTH_SHORT).show()
        }
    }

    //----------------------------------------------------------------------------------------//
    //Gets a list of dates in the current month the user is in.
    fun getDatesInCurrentMonth(): List<String> {
        try {
            val currentDate = Calendar.getInstance()
            val firstDayOfMonth = Calendar.getInstance()
            firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
            val lastDayOfMonth = Calendar.getInstance()
            lastDayOfMonth.set(
                Calendar.DAY_OF_MONTH,
                lastDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            )

            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dates = mutableListOf<String>()

            var date = firstDayOfMonth.time
            val lastDate = lastDayOfMonth.time

            while (!date.after(lastDate)) {
                val formattedDate = formatter.format(date)
                dates.add(formattedDate)
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.add(Calendar.DATE, 1)
                date = calendar.time
            }
            return dates
        } catch (e: java.lang.IllegalArgumentException) {
            val dates = mutableListOf<String>()
            return dates
        }
    }

    //----------------------------------------------------------------------------------------//
    //Generates date list from the start and end date within parameters.
    fun getDateRange(startDate: Date, endDate: Date): List<Date> {
        try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dates = mutableListOf<Date>()

            val calendar = Calendar.getInstance()
            calendar.time = startDate
            val lastDate = endDate

            while (!calendar.time.after(lastDate)) {
                val date = calendar.time
                dates.add(date)
                calendar.add(Calendar.DATE, 1)
            }
            return dates
        } catch (e: IllegalArgumentException) {
            return emptyList()
        }
    }

    //----------------------------------------------------------------------------------------//
    //Converts Date to com.google.firebase.Timestamp.
    fun convertToFirebaseTimestamp(date: Date): com.google.firebase.Timestamp {
        try {
            val milliseconds = date.time
            val seconds = milliseconds / 1000
            val nanoseconds = ((milliseconds % 1000) * 1000000).toInt()
            return com.google.firebase.Timestamp(seconds, nanoseconds)
        } catch (e: java.lang.IllegalArgumentException) {
            return com.google.firebase.Timestamp(0, 0)
        }
    }

    //----------------------------------------------------------------------------------------//
    //Populates arrCat with categories from firebase.
    fun populateArrCat(binding: ActivityProgressBinding, db: FirebaseFirestore) {
        try {
            arrCat.clear()
            // Get a reference to the "CATEGORIES" collection in Firestore
            val categoriesCollection = db.collection("USER/TemplateUser/TemplateUser_CATEGORIES")
            // Retrieve the categories from Firestore
            categoriesCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    // Iterate through the query snapshot and add the category names to the array
                    for (document in querySnapshot) {
                        val categoryName = document.getString("Name")
                        if (categoryName != null) {
                            arrCat.add(categoryName)
                        }
                        populateGraphArrays(binding, db)
                        populateArrHours(binding, db)
                    }
                }
                .addOnFailureListener { exception ->
                }
        } catch (e: java.lang.IllegalArgumentException) {

        }
    }

    //----------------------------------------------------------------------------------------//
    //Formats com.google.firebase.Timestamp to date string.
    private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
        val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        return dateFormat.format(Date(milliseconds))
    }

    //----------------------------------------------------------------------------------------//
    //used for custom labels.
    enum class GoalCategory {
        GOALS_ACHIEVED,
        NO_WORK_DONE,
        GOALS_MISSED
    }
}
//----------------------------------------------------------------------------------------//
//References
/*
Get started with Cloud Firestore
https://firebase.google.com/docs/firestore/quickstart#android
Delete data from Cloud Firestore
https://firebase.google.com/docs/firestore/manage-data/delete-data#kotlin+ktx
Add data to Cloud Firestore
https://firebase.google.com/docs/firestore/manage-data/add-data
Perform simple and compound queries in Cloud Firestore
https://firebase.google.com/docs/firestore/query-data/queries
Create app icons
https://developer.android.com/studio/write/create-app-icons
How to convert firebase timestamp into date and time
https://stackoverflow.com/questions/38016168/how-to-convert-firebase-timestamp-into-date-and-time
Line Graph View in Android with Example
https://www.geeksforgeeks.org/line-graph-view-in-android-with-example/
*/
