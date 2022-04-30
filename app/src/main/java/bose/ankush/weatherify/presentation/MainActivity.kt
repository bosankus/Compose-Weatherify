package bose.ankush.weatherify.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import bose.ankush.weatherify.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this@MainActivity
        setContentView(binding.root)
    }

}