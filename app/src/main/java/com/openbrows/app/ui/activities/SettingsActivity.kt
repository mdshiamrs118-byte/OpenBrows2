package com.openbrows.app.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.openbrows.app.R
import com.openbrows.app.data.BrowserPreferences
import com.openbrows.app.databinding.ActivitySettingsBinding
import com.openbrows.app.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: BrowserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = BrowserPreferences(applicationContext)

        val res = resources

        bindSpinner(
            binding.spinnerTextSize,
            res.getStringArray(R.array.text_size_entries),
            res.getIntArray(R.array.text_size_values).toList(),
            prefs.textSizeFlow
        ) { prefs.saveTextSize(it) }

        val scaleValues = res.getStringArray(R.array.scale_values).map { it.toFloat() }
        bindSpinner(
            binding.spinnerUiItemSize,
            res.getStringArray(R.array.scale_entries),
            scaleValues,
            prefs.uiItemSizeFlow
        ) { prefs.saveUiItemSize(it) }

        bindSpinner(
            binding.spinnerLayoutSize,
            res.getStringArray(R.array.scale_entries),
            scaleValues,
            prefs.layoutSizeFlow
        ) { prefs.saveLayoutSize(it) }

        bindSpinner(
            binding.spinnerUrlPosition,
            res.getStringArray(R.array.url_position_entries),
            res.getStringArray(R.array.url_position_values).toList(),
            prefs.urlPositionFlow
        ) { prefs.saveUrlPosition(it) }

        bindSpinner(
            binding.spinnerTabLayout,
            res.getStringArray(R.array.tab_layout_entries),
            res.getStringArray(R.array.tab_layout_values).toList(),
            prefs.tabLayoutFlow
        ) { prefs.saveTabLayout(it) }

        bindSpinner(
            binding.spinnerAutohide,
            res.getStringArray(R.array.autohide_entries),
            res.getStringArray(R.array.autohide_values).toList(),
            prefs.autohideFlow
        ) { prefs.saveAutohide(it) }

        lifecycleScope.launch {
            binding.etHomepage.setText(prefs.homepageFlow.first())
        }
        binding.etHomepage.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveHomepage()
        }
    }

    override fun onPause() {
        saveHomepage()
        super.onPause()
    }

    private fun saveHomepage() {
        val value = Constants.normalizeHomepage(binding.etHomepage.text.toString())
        lifecycleScope.launch { prefs.saveHomepage(value) }
    }

    private fun <T> bindSpinner(
        spinner: Spinner,
        entries: Array<String>,
        values: List<T>,
        flow: Flow<T>,
        save: suspend (T) -> Unit
    ) {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, entries)
        lifecycleScope.launch {
            val current = flow.first()
            spinner.setSelection(values.indexOf(current).coerceAtLeast(0), false)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    lifecycleScope.launch { save(values[position]) }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
    }
}
