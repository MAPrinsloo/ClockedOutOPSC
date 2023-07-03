package com.example.clockedout

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clockedout.databinding.ActivityGoalsBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class Goals : AppCompatActivity() {
    //Holds the hours for the minimum goal.
    var minHours: Int = 0

    //Holds the hours for the maximum goal.
    var maxHours: Int = 0

    //----------------------------------------------------------------------------------------//
    //On create method.
    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        //Binding.
        val binding = ActivityGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //----------------------------------------------------------------------------------------//
        //Set Goals Button.
        binding.btnSetGoals.setOnClickListener()
        {
            try {
                minHours = binding.etMinHours.text.toString().toInt()
                maxHours = binding.etMaxHours.text.toString().toInt()
                //Data Validation.
                if (minHours > 0 && maxHours > 0) {
                    captureGoals(minHours, maxHours)
                }
                if (minHours <= 0) {
                    binding.etMinHours.error = "Must be greater than 0"
                }
                if (maxHours <= 0) {
                    binding.etMaxHours.error = "Must be greater than 0"
                }
            } catch (e: java.lang.IllegalArgumentException) {
                Toast.makeText(
                    this,
                    "Error Occurred, Ensure all values are entered correctly.",
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }

    //--------------------------------------------------------------------------------------------//
    //Used to Capture the goals.
    private fun captureGoals(minHours: Int, maxHours: Int) {
        val db = FirebaseFirestore.getInstance()
        //User collection.
        val userCollection = db.collection("USER")
        //TemplateUser is our user's account.
        val templateUserDocument = userCollection.document("TemplateUser")
        //Update the fields in the document.
        templateUserDocument.update("MinHours", minHours, "MaxHours", maxHours)
            .addOnSuccessListener {
                Toast.makeText(this, "Goals Captured successfully.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Goals Capture failed.", Toast.LENGTH_LONG).show()
            }
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
