package com.example.clockedout

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class DoNotDisturb : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donotdisturb)

        val button = findViewById<Button>(R.id.button)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)

var nm: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && !nm.isNotificationPolicyAccessGranted){
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        var am: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        button.setOnClickListener{
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
            Toast.makeText(applicationContext,"Ringer Mode Set",Toast.LENGTH_SHORT).show()
        }
        button2.setOnClickListener{
            am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            Toast.makeText(applicationContext,"Vibrate Mode Set",Toast.LENGTH_SHORT).show()
        }
        button3.setOnClickListener{
            am.ringerMode = AudioManager.RINGER_MODE_SILENT
            Toast.makeText(applicationContext,"Silent Mode Set",Toast.LENGTH_SHORT).show()
        }
        button4.setOnClickListener{
            when(am.ringerMode){
                AudioManager.RINGER_MODE_NORMAL -> {
                    Toast.makeText(applicationContext,"Current mode is Ringing",Toast.LENGTH_SHORT).show()
                }
                AudioManager.RINGER_MODE_SILENT -> {
                    Toast.makeText(applicationContext,"Current mode is Silent",Toast.LENGTH_SHORT).show()
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    Toast.makeText(applicationContext,"Current mode is Vibrate",Toast.LENGTH_SHORT).show()
                }
            }
        }


    }
}