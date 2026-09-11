package com.hassan.colorcraft.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hassan.colorcraft.databinding.ActivitySettingsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModel()

    private var isApplyingProgrammaticSwitchChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        viewModel.isDarkModeEnabled.observe(this) { isEnabled ->
            isApplyingProgrammaticSwitchChange = true
            binding.darkModeSwitch.isChecked = isEnabled
            isApplyingProgrammaticSwitchChange = false
        }

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isApplyingProgrammaticSwitchChange) {
                viewModel.setDarkMode(isChecked)
            }
        }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
        binding.appVersionValueText.text = versionName ?: ""

        binding.rateAppRow.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            }
        }

        binding.shareAppRow.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ColorCraft – a relaxing coloring book and drawing app!"
                )
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }

        binding.privacyPolicyRow.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy")))
        }

        binding.termsOfUseRow.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/terms")))
        }
    }
}
