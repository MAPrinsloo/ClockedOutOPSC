package com.example.clockedout

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.clockedout.databinding.ActivityHubBinding

class Hub : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Binding
        val binding = ActivityHubBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //----------------------------------------------------------------------------------------//
        //CLOCK OUT
        binding.btnTimesheet.setOnClickListener()
        {
            val intent = Intent(this, Timesheet::class.java)
            startActivity(intent)
        }
        //----------------------------------------------------------------------------------------//
        //MANAGE CATEGORIES
        binding.btnCategories.setOnClickListener()
        {
            val intent = Intent(this, Categories::class.java)
            startActivity(intent)
        }
        //----------------------------------------------------------------------------------------//
        //SET GOALS
        binding.btnGoals.setOnClickListener()
        {
            val intent = Intent(this, Goals::class.java)
            startActivity(intent)
        }
        //----------------------------------------------------------------------------------------//
        //VIEW HISTORY
        binding.btnHistory.setOnClickListener()
        {
            val intent = Intent(this, History::class.java)
            startActivity(intent)
        }
        //----------------------------------------------------------------------------------------//
        //CHECK PROGRESS
        binding.btnProgress.setOnClickListener()
        {
            val intent = Intent(this,  Progress::class.java)
            startActivity(intent)
        }
        //----------------------------------------------------------------------------------------//
        //CHECK PROGRESS
        binding.btnQuiz.setOnClickListener()
        {
            val intent = Intent(this,  Quiz::class.java)
            startActivity(intent)
        }
        //----------------------------------------------------------------------------------------//
        //CHECK PROGRESS
        binding.btnDoNotDisturb.setOnClickListener()
        {
            val intent = Intent(this,  DoNotDisturb::class.java)
            startActivity(intent)
        }
    }
}