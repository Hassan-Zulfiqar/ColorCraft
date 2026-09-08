package com.hassan.colorcraft.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hassan.colorcraft.R
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import com.hassan.colorcraft.databinding.ItemColoringPageBinding

class HomeAdapter(
    private val onItemClick: (ColoringPageEntity) -> Unit
) : ListAdapter<ColoringPageEntity, HomeAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColoringPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemColoringPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ColoringPageEntity) {
            binding.coloringTitleText.text = item.title
            binding.difficultyLabelText.text = item.difficultyLabel

            val difficultyColorRes = when (item.difficultyLabel) {
                "Easy" -> R.color.color_difficulty_easy
                "Medium" -> R.color.color_difficulty_medium
                "Detailed" -> R.color.color_difficulty_detailed
                else -> R.color.color_difficulty_easy
            }
            binding.difficultyLabelText.setTextColor(
                ContextCompat.getColor(itemView.context, difficultyColorRes)
            )

            Glide.with(itemView.context)
                .load("file:///android_asset/" + item.thumbnailPath)
                .into(binding.coloringIconImage)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ColoringPageEntity>() {
        override fun areItemsTheSame(oldItem: ColoringPageEntity, newItem: ColoringPageEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ColoringPageEntity, newItem: ColoringPageEntity): Boolean =
            oldItem == newItem
    }
}
