package com.hassan.colorcraft.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hassan.colorcraft.R
import com.hassan.colorcraft.databinding.ItemOnboardingPageBinding

class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = PAGE_COUNT

    class ViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val (headlineRes, subtextRes, illustrationLayoutRes) = when (position) {
                0 -> Triple(R.string.onboarding_title_1, R.string.onboarding_subtitle_1, R.layout.onboarding_illustration_1)
                1 -> Triple(R.string.onboarding_title_2, R.string.onboarding_subtitle_2, R.layout.onboarding_illustration_2)
                else -> Triple(R.string.onboarding_title_3, R.string.onboarding_subtitle_3, R.layout.onboarding_illustration_3)
            }

            binding.pageHeadlineText.setText(headlineRes)
            binding.pageSubtextText.setText(subtextRes)

            binding.illustrationContainer.removeAllViews()
            LayoutInflater.from(binding.illustrationContainer.context)
                .inflate(illustrationLayoutRes, binding.illustrationContainer, true)
        }
    }

    companion object {
        private const val PAGE_COUNT = 3
    }
}
