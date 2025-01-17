package com.example.flashlightwithtimer

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.widget.EditText
import android.widget.NumberPicker
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

        val hourPicker: NumberPicker = findViewById(R.id.hoursPicker)
        val minutePicker: NumberPicker = findViewById(R.id.minutesPicker)
        val secondPicker: NumberPicker = findViewById(R.id.secondsPicker)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:FlashlightWakelock")

        customizeNumberPicker(hourPicker)
        customizeNumberPicker(minutePicker)
        customizeNumberPicker(secondPicker)

        setupNumberPickers()

        binding.flashlightButton.setOnClickListener {
            toggleFlashlight()
        }

        binding.startTimerButton.setOnClickListener {
            startCountdown()
        }

        // Apply the style again when changing the value, if needed
        hourPicker.setOnValueChangedListener { _, _, _ ->
            customizeNumberPicker(hourPicker)
        }
        minutePicker.setOnValueChangedListener { _, _, _ ->
            customizeNumberPicker(minutePicker)
        }
        secondPicker.setOnValueChangedListener { _, _, _ ->
            customizeNumberPicker(secondPicker)
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

        val totalMillis =
            TimeUnit.HOURS.toMillis(hours.toLong()) + TimeUnit.MINUTES.toMillis(minutes.toLong()) + TimeUnit.SECONDS.toMillis(
                seconds.toLong()
            )

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
                    Toast.makeText(
                        this@MainActivity,
                        "Timer finished, flashlight turned off",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.start()
        } else {
            Toast.makeText(this, "Please set a time greater than 0", Toast.LENGTH_SHORT).show()
        }
    }

    fun customizeNumberPicker(numberPicker: NumberPicker) {
        numberPicker.postDelayed({
            for (i in 0 until numberPicker.childCount) {
                val child = numberPicker.getChildAt(i)
                if (child is EditText) {
                    // Apply color and text size
                    child.setTextColor(Color.RED) // Change to your desired color
                    child.textSize = 20f // Change text size
                }
            }
        }, 50) // Delay to ensure layout is completed before applying customizations
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (flashlightOn) {
            turnOffFlashlight()
        }
    }

}