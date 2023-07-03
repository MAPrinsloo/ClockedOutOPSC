package com.example.clockedout

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.example.clockedout.databinding.ActivityTimesheetBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*

class Timesheet : AppCompatActivity() {
    //Array of categories.
    private var arrCat = arrayListOf<String>()

    //----------------------------------------------------------------------------------------//
    //On create method.
    override fun onCreate(savedInstanceState: Bundle?) {
        //Initialize firebase.
        FirebaseApp.initializeApp(this)
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        super.onCreate(savedInstanceState)
        //Binding.
        val binding = ActivityTimesheetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Populate Spinner.
        populateSpinner(binding, db)
        //Using the camera - Code from Module Manual.
        val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                var bitmap = it.data!!.extras?.get("data") as Bitmap
                binding.ibtnAddImage.setImageBitmap(bitmap)
            }
        }
        //----------------------------------------------------------------------------------------//
        //https://www.baeldung.com/kotlin/string-to-date
        //btnClock click.
        binding.btnClock.setOnClickListener()
        {
            try {
                var requiredFieldsFilled = true
                val etDate = binding.etDate.text.toString()
                val etStartTime = binding.etStartTime.text.toString()
                val etEndTime = binding.etEndTime.text.toString()
                val etCategory = binding.spCategories.selectedItem.toString()
                val etDescription = binding.etDescription.text.toString()
                val image = binding.ibtnAddImage.drawable.toBitmap()
                //Data Validation.
                if (etDate.isEmpty()) {
                    binding.etDate.error = "enter a date dd/MM/yyyy."
                    requiredFieldsFilled = false
                }
                if (etStartTime.isEmpty()) {
                    binding.etStartTime.error = "please enter a start time eg. 13:00"
                    requiredFieldsFilled = false
                }
                if (etEndTime.isEmpty()) {
                    binding.etEndTime.error = "Please enter an end time eg. 14:00"
                    requiredFieldsFilled = false
                }
                if (requiredFieldsFilled == true) {
                    var date: Date = getDate(binding, etDate) as Date
                    var startTime: Time = getStartTime(binding, etStartTime) as Time
                    var endTime: Time = getEndTime(binding, etEndTime) as Time
                    saveData(date, startTime, endTime, etCategory, etDescription, image)
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
        //Image button for taking a picture.
        binding.ibtnAddImage.setOnClickListener()
        {
            try {
                var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                getResult.launch(intent)
            } catch (e: java.lang.IllegalArgumentException) {
                Toast.makeText(
                    this,
                    "Error Occurred, Ensure all values are entered correctly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    //----------------------------------------------------------------------------------------//
    //Saving the data entered into the shared preferences.
    private fun saveData(
        date: Date,
        startTime: Time,
        endTime: Time,
        category: String,
        description: String,
        image: Bitmap
    ) {
        //https://stackoverflow.com/questions/70367909/converting-timestamp-value-to-12-24-hour-time-value-in-kotlin
        val db = FirebaseFirestore.getInstance()
        val timesheetCollection =
            db.collection("/USER/TemplateUser/TemplateUser_CATEGORIES/$category/TIMESHEET")

        //Convert bitmap to Base64 encoded string.
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        val encodedImage = Base64.encodeToString(b, Base64.DEFAULT)
        //Calc time studied.
        var diff: Long = endTime.time - startTime.time
        var seconds = (diff / 1000).toInt()
        var minutes = seconds / 60
        var hours = minutes / 60
        //Data to save.
        val timesheetData = hashMapOf(
            "Date" to date,
            "StartTime" to startTime,
            "EndTime" to endTime,
            "Category" to category,
            "Description" to description,
            "HoursStudied" to hours,
            "EncodedImage" to encodedImage
        )
        //saving the data.
        timesheetCollection.add(timesheetData)
            .addOnSuccessListener { documentReference ->
                val documentId = documentReference.id
                Toast.makeText(this, "Timesheet saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving timesheet: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }

    }

    //----------------------------------------------------------------------------------------//
    //Populating the spinner with categories.
    private fun populateSpinner(binding: ActivityTimesheetBinding, db: FirebaseFirestore) {
        //Firestore ref.
        val categoriesCollection = db.collection("USER/TemplateUser/TemplateUser_CATEGORIES")
        categoriesCollection.get()
            .addOnSuccessListener { querySnapshot ->
                arrCat.clear()
                //Iterate through query snapshot to populate array.
                for (document in querySnapshot) {
                    val categoryName = document.getString("Name")
                    if (categoryName != null) {
                        arrCat.add(categoryName)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrCat)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spCategories.adapter = adapter
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
    }

    //----------------------------------------------------------------------------------------//
    //Returns a date if valid, if not populates error.
    private fun getDate(binding: ActivityTimesheetBinding, etDate: String): Date? {
        val formatter = SimpleDateFormat("dd/MM/yyyy")
        var date: Date? = null
        try {
            date = formatter.parse(etDate)
        } catch (e: java.text.ParseException) {
            binding.etDate.error = "Please enter a date dd/mm/yyyy"
        }
        return date
    }

    //----------------------------------------------------------------------------------------//
    //Returns a start time if valid, if not populates error.
    private fun getStartTime(binding: ActivityTimesheetBinding, etStartTime: String): Time? {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        var startTime: Time? = null
        try {
            startTime = Time(sdf.parse(etStartTime).time)
        } catch (e: java.text.ParseException) {
            binding.etStartTime.error = "Please enter a start time eg. 13:00"
        }
        return startTime
    }

    //----------------------------------------------------------------------------------------//
    //https://stackoverflow.com/questions/70367909/converting-timestamp-value-to-12-24-hour-time-value-in-kotlin
    //Returns a end time if valid, if not populates error.
    private fun getEndTime(binding: ActivityTimesheetBinding, etEndTime: String): Time? {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        var endTime: Time? = null

        try {
            endTime = Time(sdf.parse(etEndTime).time)
        } catch (e: java.text.ParseException) {
            binding.etEndTime.error = "Please enter a end time eg. 14:00"
        }
        return endTime
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
