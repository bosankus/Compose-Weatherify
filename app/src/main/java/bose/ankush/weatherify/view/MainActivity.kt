package bose.ankush.weatherify.view

import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import bose.ankush.weatherify.R
import bose.ankush.weatherify.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val animation by lazy {
        AnimationUtils.loadAnimation(applicationContext, R.anim.slide_in_bottom)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.apply {
            lifecycleOwner = this@MainActivity
            viewmodel = viewModel
        }
        setContentView(binding.root)

        binding.activityMainLayoutWeather.layoutWeatherRecyclerview.startAnimation(animation)
        setClickListeners()
    }


    private fun setClickListeners() {
        binding.activityMainLayoutError.layoutErrorBtnRetry.setOnClickListener {
            viewModel.getCurrentTemperature()
        }
    }

}