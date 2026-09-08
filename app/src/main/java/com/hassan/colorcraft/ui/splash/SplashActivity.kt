package com.hassan.colorcraft.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hassan.colorcraft.MainActivity
import com.hassan.colorcraft.databinding.ActivitySplashBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.isReadyToNavigate.observe(this) { isReady ->
            if (isReady) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
