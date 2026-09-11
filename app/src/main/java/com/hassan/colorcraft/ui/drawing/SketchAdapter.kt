package com.hassan.colorcraft.ui.drawing

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.hassan.colorcraft.data.db.entity.SketchEntity
import com.hassan.colorcraft.databinding.ItemSketchBinding
import java.io.File
import java.util.Date

class SketchAdapter(
    private val onItemClick: (SketchEntity) -> Unit,
    private val onItemLongClick: (SketchEntity) -> Unit
) : ListAdapter<SketchEntity, SketchAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSketchBinding.inflate(
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
        private val binding: ItemSketchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SketchEntity) {
            Glide.with(itemView.context)
                .load(File(item.filePath))
                .signature(ObjectKey(item.updatedAt))
                .into(binding.sketchThumbnailImage)

            binding.sketchTitleText.text = item.title
            binding.sketchDateText.text = DateFormat.getMediumDateFormat(itemView.context)
                .format(Date(item.updatedAt))

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SketchEntity>() {
        override fun areItemsTheSame(oldItem: SketchEntity, newItem: SketchEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SketchEntity, newItem: SketchEntity): Boolean =
            oldItem == newItem
    }
}
