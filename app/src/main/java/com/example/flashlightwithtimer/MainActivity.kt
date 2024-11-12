package com.example.flashlightwithtimer

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.flashlightwithtimer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var flashlightOn = false
    private var countDownTimer: CountDownTimer? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:FlashlightWakelock")

        binding.flashlightButton.setOnClickListener {
            toggleFlashlight()
        }

        binding.startTimerButton.setOnClickListener {
            val minutes = binding.timerInput.text.toString().toIntOrNull()
            if (minutes != null && minutes > 0) {
                startCountdown(minutes * 60 * 1000L)
            } else {
                Toast.makeText(this, "Enter a valid number of minutes", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun toggleFlashlight() {
        if (flashlightOn) {
            turnOffFlashlight()
        } else {
            turnOnFlashlight()
        }
    }

    private fun turnOnFlashlight() {
        if (cameraId != null) {
            cameraManager.setTorchMode(cameraId!!, true)
            flashlightOn = true
            wakeLock.acquire()
        }
    }

    private fun turnOffFlashlight() {
        if (cameraId != null) {
            cameraManager.setTorchMode(cameraId!!, false)
            flashlightOn = false
            wakeLock.release()
        }
    }

    private fun startCountdown(duration: Long) {
        turnOnFlashlight()
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Optionally update UI with remaining time
                TODO("Update editText with remaining time")
                // binding.timerInput.text = millisUntilFinished.toEditable()
            }

            override fun onFinish() {
                turnOffFlashlight()
                Toast.makeText(this@MainActivity, "Timer finished, flashlight turned off", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (flashlightOn) {
            turnOffFlashlight()
        }
    }

}