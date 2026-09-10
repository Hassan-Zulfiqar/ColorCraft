package com.hassan.colorcraft.ui.coloring

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hassan.colorcraft.databinding.ItemColorSwatchBinding

class ColorSwatchAdapter(
    private var colors: List<Int>,
    private val onColorClick: (Int) -> Unit
) : RecyclerView.Adapter<ColorSwatchAdapter.ViewHolder>() {

    private var selectedColor: Int? = colors.firstOrNull()

    fun setSelectedColor(color: Int) {
        selectedColor = color
        notifyDataSetChanged()
    }

    fun updateColors(newColors: List<Int>) {
        colors = newColors
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColorSwatchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size

    inner class ViewHolder(
        private val binding: ItemColorSwatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(color: Int) {
            binding.root.backgroundTintList = ColorStateList.valueOf(color)

            val isSelected = color == selectedColor
            val scale = if (isSelected) 1.2f else 1.0f
            binding.root.scaleX = scale
            binding.root.scaleY = scale

            binding.root.setOnClickListener {
                onColorClick(color)
            }
        }
    }
}
