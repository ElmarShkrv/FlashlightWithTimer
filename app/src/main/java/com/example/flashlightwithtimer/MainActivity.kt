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
import java.util.concurrent.TimeUnit

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

        setupNumberPickers()

        binding.flashlightButton.setOnClickListener {
            toggleFlashlight()
        }

        binding.startTimerButton.setOnClickListener {
            startCountdown()
//            val minutes = binding.timerInput.text.toString().toIntOrNull()
//            if (minutes != null && minutes > 0) {
//                startCountdown(minutes * 60 * 1000L)
//            } else {
//                Toast.makeText(this, "Enter a valid number of minutes", Toast.LENGTH_SHORT).show()
//            }
        }

    }

    private fun setupNumberPickers() {
        binding.hoursPicker.apply {
            minValue = 0
            maxValue = 23
        }

        binding.minutesPicker.apply {
            minValue = 0
            maxValue = 59
        }

        binding.secondsPicker.apply {
            minValue = 0
            maxValue = 59
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

    private fun startCountdown() {
        val hours = binding.hoursPicker.value
        val minutes = binding.minutesPicker.value
        val seconds = binding.secondsPicker.value

        val totalMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())

        if (totalMillis > 0) {
            turnOnFlashlight()

            // Cancel any existing timer
            countDownTimer?.cancel()

            // Start new timer
            countDownTimer = object : CountDownTimer(totalMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val hoursRemaining = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                    val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                    val secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60

                    // Update the countdown display directly in the NumberPickers
                    binding.hoursPicker.value = hoursRemaining.toInt()
                    binding.minutesPicker.value = minutesRemaining.toInt()
                    binding.secondsPicker.value = secondsRemaining.toInt()
                }

                override fun onFinish() {
                    turnOffFlashlight()
                    Toast.makeText(this@MainActivity, "Timer finished, flashlight turned off", Toast.LENGTH_SHORT).show()
                }
            }.start()
        } else {
            Toast.makeText(this, "Please set a time greater than 0", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun startCountdown(duration: Long) {
//        turnOnFlashlight()
//        countDownTimer?.cancel()
//        countDownTimer = object : CountDownTimer(duration, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                // Optionally update UI with remaining time
//                TODO("Update editText with remaining time")
//                // binding.timerInput.text = millisUntilFinished.toEditable()
//            }
//
//            override fun onFinish() {
//                turnOffFlashlight()
//                Toast.makeText(this@MainActivity, "Timer finished, flashlight turned off", Toast.LENGTH_SHORT).show()
//            }
//        }.start()
//    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (flashlightOn) {
            turnOffFlashlight()
        }
    }

}