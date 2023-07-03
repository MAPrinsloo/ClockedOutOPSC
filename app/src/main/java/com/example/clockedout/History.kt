package com.example.clockedout

import android.R
import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clockedout.databinding.ActivityHistoryBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*


class History : AppCompatActivity() {
    //Categories Array.
    private var arrCat = arrayListOf<String>()

    //Holds Timesheet items pre filtering.
    private var arrTimesheetItems = arrayListOf<String>()

    //Holds string values for encoded images.
    private var arrImages = arrayListOf<String>()

    //Holds an array of the hours studied for all timesheets.
    private var arrHoursStudied = arrayListOf<Int>()

    //An array for all the timesheets IDs
    private var arrTimesheetIDs = arrayListOf<String>()

    //Holds string value for the selected category.
    private var selectedCategory: String = ""

    //Is the application busy filtering.
    private var filtering = false

    //----------------------------------------------------------------------------------------//
    //On create method.
    override fun onCreate(savedInstanceState: Bundle?) {
        //Initialize firebase.
        FirebaseApp.initializeApp(this)
        val db = Firebase.firestore
        super.onCreate(savedInstanceState)
        //Binding.
        val binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Populate selection fields.
        populateSpinner(binding, db)
        //----------------------------------------------------------------------------------------//
        //Filter button.
        binding.btnFilter.setOnClickListener()
        {
            try {
                //Data Validations.
                if ((binding.etEndDate.text.isNotEmpty()) && (binding.etStartDate.text.isNotEmpty())) {
                    filterUpdatesListView(binding, db)
                }
                if (binding.etStartDate.text.isEmpty()) {
                    binding.etStartDate.error = "Please enter a start date dd/mm/yyyy"
                }
                if (binding.etEndDate.text.isEmpty()) {
                    binding.etEndDate.error = "Please enter a end date dd/mm/yyyy"
                }
            } catch (e: java.lang.IllegalArgumentException) {
                binding.etStartDate.error = "Please enter a start date dd/mm/yyyy"
                binding.etEndDate.error = "Please enter a end date dd/mm/yyyy"
                Toast.makeText(
                    this,
                    "Error Occurred, Ensure all values are entered correctly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        //----------------------------------------------------------------------------------------//
        //https://stackoverflow.com/questions/22462339/implementing-onitemselectedlistener-for-spinner
        //https://stackoverflow.com/questions/46447296/android-kotlin-onitemselectedlistener-for-spinner-not-working
        //When a category is selected.
        binding.spCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                binding.etStartDate.text.clear()
                binding.etEndDate.text.clear()
                selectedCategory = selectedItem
                //If the app is not currently filtering then execute.
                if (filtering != true) {
                    filterUpdatesListView(binding, db)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
        //----------------------------------------------------------------------------------------//
        //When a listview item is selected.
        binding.lvTimesheets.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = binding.lvTimesheets.getItemAtPosition(position).toString()
            var counter = 0

            arrTimesheetItems.forEach {
                if (it.equals(selectedItem)) {
                    val encodedImage = arrImages.get(counter)
                    val decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    binding.ivPicture.setImageBitmap(bitmap)
                }
                counter++
            }
        }
    }

    //----------------------------------------------------------------------------------------//
    //https://firebase.google.com/docs/firestore/query-data/queries#compound_and_queries
    //Filter updates the list view.
    private fun filterUpdatesListView(binding: ActivityHistoryBinding, db: FirebaseFirestore) {
        try {
            //
            val stringToDateFormatter = SimpleDateFormat("dd/MM/yyyy")
            val etStartDate: String = binding.etStartDate.text.toString()
            val etEndDate: String = binding.etEndDate.text.toString()
            //Start of filter.
            filtering = true
            //If start and end date has been populated by user.
            if (binding.etStartDate.text.isNotEmpty() && binding.etEndDate.text.isNotEmpty()) {
                val startDate = stringToDateFormatter.parse(etStartDate) as Date
                val endDate = stringToDateFormatter.parse(etEndDate) as Date

                val startTimestamp = convertToFirebaseTimestamp(startDate)
                val endTimestamp = convertToFirebaseTimestamp(endDate)
                //Filters category, and by date range.
                filter(binding, db, selectedCategory, startTimestamp, endTimestamp)
            } else if (!selectedCategory.equals("")) {
                //Filters category only.
                filter(binding, db, selectedCategory)
            }
        } catch (e: IllegalArgumentException) {
            Toast.makeText(
                this,
                "Error Occurred, Ensure all values are entered correctly.",
                Toast.LENGTH_LONG
            ).show()
            Toast.makeText(this, "$e", Toast.LENGTH_SHORT).show()
        } catch (es: java.text.ParseException) {
            binding.etStartDate.error = "Please enter a start date dd/mm/yyyy"
            binding.etEndDate.error = "Please enter a end date dd/mm/yyyy"
            Toast.makeText(
                this,
                "Error Occurred, Ensure all values are entered correctly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //----------------------------------------------------------------------------------------//
    //Filter by category only.
    private fun filter(
        binding: ActivityHistoryBinding,
        db: FirebaseFirestore,
        selectedCategory: String
    ) {
        try {
            //Firestore path.
            val timesheetCollection =
                db.collection("USER/TemplateUser/TemplateUser_CATEGORIES/$selectedCategory/TIMESHEET")
            timesheetCollection.get().addOnSuccessListener { querySnapshot ->
                arrHoursStudied.clear()
                arrTimesheetIDs.clear()
                arrTimesheetItems.clear()
                arrImages.clear()
                //populate the timesheetIDs
                for (document in querySnapshot) {
                    arrTimesheetIDs.add(document.id)
                }
                //Iterate through all timesheet entries.
                for (timesheetId in arrTimesheetIDs) {
                    val timesheetIdRef = timesheetCollection.document(timesheetId)

                    timesheetIdRef.get().addOnSuccessListener { documentSnapshot ->
                        val document = documentSnapshot.data
                        //If there is a document get fields.
                        if (document != null) {
                            val category = document["Category"] as? String ?: ""
                            val dateTimestamp = document["Date"] as? com.google.firebase.Timestamp
                            val startTimeTimestamp =
                                document["StartTime"] as? com.google.firebase.Timestamp
                            val endTimeTimestamp =
                                document["EndTime"] as? com.google.firebase.Timestamp
                            val description = document["Description"] as? String ?: ""
                            val hoursStudied = document["HoursStudied"] as? Long

                            //convert into format that is more usable and readable.
                            if (dateTimestamp != null && startTimeTimestamp != null && endTimeTimestamp != null && hoursStudied != null) {
                                val dateValue = formatDate(dateTimestamp)
                                val startTimeValue = formatTime(startTimeTimestamp)
                                val endTimeValue = formatTime(endTimeTimestamp)
                                val hoursStudiedValue = hoursStudied.toInt()
                                //add hours to array.
                                arrHoursStudied.add(hoursStudiedValue)
                                //call method to calc Hours.
                                calculateHours(binding)
                                //get base64 string and add to array.
                                val encodedImage = document["EncodedImage"] as? String ?: ""
                                arrImages.add(encodedImage)
                                //stringbuilder for output.
                                var timesheetEntry = "Category: $category \r\n" +
                                        "Date: $dateValue\r\n" +
                                        "StartTime: $startTimeValue\r\n" +
                                        "EndTime: $endTimeValue\r\n" +
                                        "Description: $description \r\n" +
                                        "Category: $category"
                                //used for adapter in list view.
                                arrTimesheetItems.add(timesheetEntry)
                                populateListView(binding)
                            }
                        }
                    }.addOnFailureListener { exception ->
                        Log.d(TAG, "", exception)
                    }
                }
            }
        } catch (e: java.lang.NullPointerException) {
            Toast.makeText(
                this,
                "Error Occurred, Ensure all values are entered correctly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //----------------------------------------------------------------------------------------//
    //Filter by category and date range.
    private fun filter(
        binding: ActivityHistoryBinding,
        db: FirebaseFirestore,
        selectedCategory: String,
        startDate: com.google.firebase.Timestamp,
        endDate: com.google.firebase.Timestamp
    ) {
        try {
            //Firestore path.
            val timesheetCollection =
                db.collection("USER/TemplateUser/TemplateUser_CATEGORIES/$selectedCategory/TIMESHEET")
            arrHoursStudied.clear()
            arrTimesheetIDs.clear()
            arrTimesheetItems.clear()
            arrImages.clear()
            //query where date matches.
            timesheetCollection.whereGreaterThanOrEqualTo("Date", startDate)
                .whereLessThanOrEqualTo("Date", endDate)
                .get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        arrTimesheetIDs.add(document.id)
                    }
                    //Iterate through all timesheet entries.
                    for (timesheetId in arrTimesheetIDs) {
                        val timesheetIdRef = timesheetCollection.document(timesheetId)

                        timesheetIdRef.get().addOnSuccessListener { documentSnapshot ->
                            val document = documentSnapshot.data
                            if (document != null) {
                                val category = document["Category"] as? String ?: ""
                                val dateTimestamp =
                                    document["Date"] as? com.google.firebase.Timestamp
                                val startTimeTimestamp =
                                    document["StartTime"] as? com.google.firebase.Timestamp
                                val endTimeTimestamp =
                                    document["EndTime"] as? com.google.firebase.Timestamp
                                val description = document["Description"] as? String ?: ""
                                val hoursStudied = document["HoursStudied"] as? Long

                                if (dateTimestamp != null && startTimeTimestamp != null && endTimeTimestamp != null && hoursStudied != null) {
                                    val dateValue = formatDate(dateTimestamp)
                                    val startTimeValue = formatTime(startTimeTimestamp)
                                    val endTimeValue = formatTime(endTimeTimestamp)
                                    val hoursStudiedValue = hoursStudied.toInt()

                                    arrHoursStudied.add(hoursStudiedValue)
                                    calculateHours(binding)
                                    val encodedImage = document["EncodedImage"] as? String ?: ""
                                    arrImages.add(encodedImage)

                                    var timesheetEntry = "Category: $category \r\n" +
                                            "Date: $dateValue\r\n" +
                                            "StartTime: $startTimeValue\r\n" +
                                            "EndTime: $endTimeValue\r\n" +
                                            "Description: $description \r\n" +
                                            "Category: $category"
                                    arrTimesheetItems.add(timesheetEntry)
                                    populateListView(binding)
                                }
                            }
                        }.addOnFailureListener { exception ->
                            Log.d(TAG, "get failed with ", exception)
                        }
                    }
                    calculateHours(binding)
                }
        } catch (e: java.lang.NullPointerException) {
            Toast.makeText(
                this,
                "Error Occurred, Ensure all values are entered correctly.",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    //----------------------------------------------------------------------------------------//
    //Populates the list view with timesheet items.
    private fun populateListView(binding: ActivityHistoryBinding) {
        //Display the total hours studied for the filter applied.
        var adapter = ArrayAdapter(this, R.layout.simple_list_item_1, arrTimesheetItems)
        adapter.setDropDownViewResource(R.layout.simple_list_item_1)
        binding.lvTimesheets.adapter = adapter
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //End of filter.
        filtering = false
    }

    //----------------------------------------------------------------------------------------//
    //Populates the spinner with categories.
    fun populateSpinner(binding: ActivityHistoryBinding, db: FirebaseFirestore) {
        val categoriesCollection = db.collection("USER/TemplateUser/TemplateUser_CATEGORIES")
        arrCat.clear()
        categoriesCollection.get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val categoryName = document.getString("Name")
                    if (categoryName != null) {
                        arrCat.add(categoryName)
                    }
                }
                //First time run need to set the selected category.
                selectedCategory = arrCat[0]
                filterUpdatesListView(binding, db)
                val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, arrCat)
                adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                binding.spCategories.adapter = adapter
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
    }

    //----------------------------------------------------------------------------------------//
    //Calculates the hours recorded and displays in the tvHours.text.
    private fun calculateHours(binding: ActivityHistoryBinding) {
        var totalHours = 0
        //Query and read from database to write to array.
        for (entry in arrHoursStudied) {
            totalHours += entry
        }
        binding.tvHours.text = "Hours: $totalHours"
        populateListView(binding)
    }

    //----------------------------------------------------------------------------------------//
    //Formats com.google.firebase.Timestamp to date string.
    private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
        val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        return dateFormat.format(Date(milliseconds))
    }

    //----------------------------------------------------------------------------------------//
    //Formats com.google.firebase.Timestamp to time string.
    private fun formatTime(timestamp: com.google.firebase.Timestamp): String {
        val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        return timeFormat.format(Date(milliseconds))
    }

    //----------------------------------------------------------------------------------------//
    //Converts Date to com.google.firebase.Timestamp.
    fun convertToFirebaseTimestamp(date: Date): com.google.firebase.Timestamp {
        val milliseconds = date.time
        val seconds = milliseconds / 1000
        val nanoseconds = ((milliseconds % 1000) * 1000000).toInt()
        return com.google.firebase.Timestamp(seconds, nanoseconds)
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
