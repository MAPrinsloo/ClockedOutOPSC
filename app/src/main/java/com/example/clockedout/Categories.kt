package com.example.clockedout

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clockedout.databinding.ActivityCategoriesBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore


class Categories : AppCompatActivity() {
    //Categories Array.
    private var arrCat = arrayListOf<String>()

    //----------------------------------------------------------------------------------------//
    //On create method.
    override fun onCreate(savedInstanceState: Bundle?) {
        //Initialize firebase.
        FirebaseApp.initializeApp(this)
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        super.onCreate(savedInstanceState)
        //Binding.
        val binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Populating spinner.
        populateSpinner(binding, db)

        //----------------------------------------------------------------------------------------//
        //Create Button is clicked.
        binding.btnCreate.setOnClickListener()
        {
            try {
                var category: String = binding.etCatName.text.toString()
                if (category != "") {
                    capture(category, binding, db)
                    populateSpinner(binding, db)
                } else {
                    binding.etCatName.error = "Please enter a Category"
                }
            } catch (e: java.lang.IllegalArgumentException) {
                Toast.makeText(
                    this,
                    "Error Occurred, Ensure all values are entered correctly.",
                    Toast.LENGTH_LONG
                ).show()
                binding.etCatName.error = "Please enter a Category"
            }
        }
        //----------------------------------------------------------------------------------------//
        //Delete Button is clicked.
        binding.btnDelete.setOnClickListener()
        {
            try {
                val selectedCategory: String = binding.spCategories.selectedItem.toString()
                delete(selectedCategory, db, binding)
                populateSpinner(binding, db)
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
    //Capture Method
    private fun capture(
        Category: String,
        binding: ActivityCategoriesBinding,
        db: FirebaseFirestore
    ) {
        //Firestore path.
        val categoriesCollection = db.collection("USER/TemplateUser/TemplateUser_CATEGORIES")
        //New category ref.
        val categoryDocument = categoriesCollection.document(Category)
        //Assigning the category a name.
        val categoryData: Map<String, Any> = mapOf("Name" to Category)
        //Save the category to Firestore.
        categoryDocument.set(categoryData)
            .addOnSuccessListener {
                binding.etCatName.text.clear()
                Toast.makeText(this, "Category Captured", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed Category Capture", Toast.LENGTH_SHORT).show()
            }
    }

    //----------------------------------------------------------------------------------------//
    //Delete Method.
    private fun delete(
        itemToDelete: String,
        db: FirebaseFirestore,
        binding: ActivityCategoriesBinding
    ) {
        //Firestore path ref.
        val categoriesCollection = db.collection("USER/TemplateUser/TemplateUser_CATEGORIES")

        //Query the collection to find the category to delete.
        categoriesCollection
            .whereEqualTo("Name", itemToDelete)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (categoryDocument in querySnapshot.documents) {
                    //Delete the category document from Firestore.
                    categoryDocument.reference.delete()
                }
                Toast.makeText(this, "Category Deleted", Toast.LENGTH_SHORT).show()

                //Repopulate the spinner after deleting the category.
                populateSpinner(binding, db)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error Occurred Category not deleted.", Toast.LENGTH_LONG)
                    .show()
            }
    }

    //----------------------------------------------------------------------------------------//
    //Populates the spinner with Categories.
    private fun populateSpinner(binding: ActivityCategoriesBinding, db: FirebaseFirestore) {
        //Firestore ref.
        val categoriesCollection = db.collection("USER/TemplateUser/TemplateUser_CATEGORIES")
        //Get categories from Firestore.
        categoriesCollection.get()
            .addOnSuccessListener { querySnapshot ->
                //Clear the categories array.
                arrCat.clear()
                //Iterate through database and populate arrCat.
                for (document in querySnapshot) {
                    val categoryName = document.getString("Name")
                    if (categoryName != null) {
                        arrCat.add(categoryName)
                    }
                }
                //arrCat for spinner.
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrCat)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spCategories.adapter = adapter
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error occurred trying to populate spinner.",
                    Toast.LENGTH_SHORT
                ).show()
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


