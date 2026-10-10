package com.openbrows.app.ui.components

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import com.openbrows.app.R
import com.openbrows.app.databinding.DialogCustomPopupBinding

class CustomPopupDialog(
    context: Context,
    private val anchorBottom: Boolean,
    private val onNewTabClick: () -> Unit,
    private val onHomeClick: () -> Unit,
    private val onAddBookmarkClick: () -> Unit,
    private val onBookmarksClick: () -> Unit,
    private val onSettingsClick: () -> Unit
) : Dialog(context, R.style.Theme_OpenBrows_Popup) {

    private lateinit var binding: DialogCustomPopupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogCustomPopupBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val density = context.resources.displayMetrics.density
            w.setGravity(Gravity.END or (if (anchorBottom) Gravity.BOTTOM else Gravity.TOP))
            val attrs = w.attributes
            attrs.x = (12 * density).toInt()
            attrs.y = (76 * density).toInt()
            attrs.dimAmount = 0.2f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attrs.blurBehindRadius = 24
            }
            w.attributes = attrs
        }

        binding.btnNewTab.setOnClickListener { dismiss(); onNewTabClick() }
        binding.btnHome.setOnClickListener { dismiss(); onHomeClick() }
        binding.btnAddBookmark.setOnClickListener { dismiss(); onAddBookmarkClick() }
        binding.btnBookmarks.setOnClickListener { dismiss(); onBookmarksClick() }
        binding.btnSettings.setOnClickListener { dismiss(); onSettingsClick() }
    }
}
