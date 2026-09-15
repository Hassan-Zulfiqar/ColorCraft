package com.hassan.colorcraft.ui.onboarding

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.hassan.colorcraft.R
import com.hassan.colorcraft.databinding.ActivityOnboardingBinding
import com.hassan.colorcraft.ui.home.HomeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModel()
    private val indicatorDots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingViewPager.adapter = OnboardingAdapter()

        setupIndicatorDots()

        binding.onboardingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageState(position)
            }
        })
        updatePageState(0)

        binding.skipButton.setOnClickListener {
            completeOnboarding()
        }

        binding.nextButton.setOnClickListener {
            val currentItem = binding.onboardingViewPager.currentItem
            if (currentItem < LAST_PAGE_INDEX) {
                binding.onboardingViewPager.currentItem = currentItem + 1
            } else {
                completeOnboarding()
            }
        }
    }

    private fun setupIndicatorDots() {
        val dotSizePx = (DOT_SIZE_DP * resources.displayMetrics.density).toInt()
        val dotMarginPx = (DOT_MARGIN_DP * resources.displayMetrics.density).toInt()

        repeat(PAGE_COUNT) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(dotSizePx, dotSizePx)
            params.marginStart = dotMarginPx
            params.marginEnd = dotMarginPx
            dot.layoutParams = params
            binding.pageIndicatorContainer.addView(dot)
            indicatorDots.add(dot)
        }
    }

    private fun updatePageState(position: Int) {
        updateIndicatorDots(position)

        binding.skipButton.visibility = if (position == LAST_PAGE_INDEX) View.GONE else View.VISIBLE
        binding.nextButtonText.text = if (position == LAST_PAGE_INDEX) {
            getString(R.string.get_started_button_text)
        } else {
            getString(R.string.next_button_text)
        }
    }

    private fun updateIndicatorDots(activePosition: Int) {
        val dotSizePx = (DOT_SIZE_DP * resources.displayMetrics.density).toInt()
        val activeDotWidthPx = (ACTIVE_DOT_WIDTH_DP * resources.displayMetrics.density).toInt()

        indicatorDots.forEachIndexed { index, dot ->
            val isActive = index == activePosition

            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = if (isActive) activeDotWidthPx else dotSizePx
            dot.layoutParams = params

            dot.background = if (isActive) {
                ContextCompat.getDrawable(this, R.drawable.gradient_primary_pill)
            } else {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(this@OnboardingActivity, R.color.color_surface_alt))
                }
            }
        }
    }

    private fun completeOnboarding() {
        viewModel.markOnboardingComplete()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    companion object {
        private const val PAGE_COUNT = 3
        private const val LAST_PAGE_INDEX = 2
        private const val DOT_SIZE_DP = 8f
        private const val DOT_MARGIN_DP = 4f
        private const val ACTIVE_DOT_WIDTH_DP = 24f
    }
}
