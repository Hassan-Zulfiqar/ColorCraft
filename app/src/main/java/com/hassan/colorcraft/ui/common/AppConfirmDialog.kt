package com.hassan.colorcraft.ui.common

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.hassan.colorcraft.databinding.DialogAppConfirmBinding

object AppConfirmDialog {

    fun show(
        context: Context,
        iconRes: Int,
        title: String,
        message: String,
        positiveText: String,
        onPositiveClick: () -> Unit,
        negativeText: String? = null,
        onNegativeClick: (() -> Unit)? = null,
        destructiveText: String? = null,
        onDestructiveClick: (() -> Unit)? = null
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DialogAppConfirmBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        binding.dialogIconImage.setImageResource(iconRes)
        binding.dialogTitleText.text = title
        binding.dialogMessageText.text = message

        binding.dialogPositiveButton.text = positiveText
        binding.dialogPositiveButton.setOnClickListener {
            onPositiveClick()
            dialog.dismiss()
        }

        binding.dialogCloseButton.setOnClickListener {
            dialog.dismiss()
        }

        if (negativeText != null) {
            binding.dialogNegativeButton.text = negativeText
            binding.dialogNegativeButton.visibility = View.VISIBLE
            binding.dialogNegativeButton.setOnClickListener {
                onNegativeClick?.invoke()
                dialog.dismiss()
            }
        } else {
            binding.dialogNegativeButton.visibility = View.GONE
        }

        if (destructiveText != null) {
            binding.dialogDestructiveButton.text = destructiveText
            binding.dialogDestructiveButton.visibility = View.VISIBLE
            binding.dialogDestructiveButton.setOnClickListener {
                onDestructiveClick?.invoke()
                dialog.dismiss()
            }
        } else {
            binding.dialogDestructiveButton.visibility = View.GONE
        }

        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }
}
