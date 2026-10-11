package com.openbrows.app.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.openbrows.app.R
import com.openbrows.app.data.TabModel
import com.openbrows.app.databinding.ActivityMainBinding
import com.openbrows.app.ui.adapters.TabsAdapter
import com.openbrows.app.ui.components.CustomPopupDialog
import com.openbrows.app.ui.viewmodels.BrowserViewModel
import com.openbrows.app.utils.BrowserWebChromeClient
import com.openbrows.app.utils.BrowserWebViewClient
import com.openbrows.app.utils.Constants
import com.openbrows.app.utils.DownloadHandler
import com.openbrows.app.utils.hide
import com.openbrows.app.utils.setVisible
import com.openbrows.app.utils.show
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BrowserViewModel by viewModels()
    private val downloadHandler by lazy { DownloadHandler(this) }

    private val webViews = mutableMapOf<String, WebView>()

    private lateinit var stripAdapter: TabsAdapter
    private lateinit var leftAdapter: TabsAdapter
    private lateinit var rightAdapter: TabsAdapter
    private lateinit var panelAdapter: TabsAdapter

    private var urlPosition = "bottom"
    private var tabLayout = "off"
    private var autohide = "off"
    private var textSize = 100
    private var itemScale = 1f
    private var layoutScale = 1f
    private var toolbarHidden = false
    private var shownTabId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabLists()
        setupListeners()
        setupBackHandling()
        observeViewModel()

        if (savedInstanceState == null) openExternalIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openExternalIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        currentWebView()?.onResume()
    }

    override fun onPause() {
        currentWebView()?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webViews.values.forEach { destroyWebView(it) }
        webViews.clear()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- setup

    private fun setupTabLists() {
        val onClick: (TabModel) -> Unit = {
            viewModel.switchTab(it.id)
            binding.tabSlidingPanel.close()
        }
        val onClose: (TabModel) -> Unit = { viewModel.closeTab(it.id) }

        stripAdapter = TabsAdapter(true, onClick, onClose)
        leftAdapter = TabsAdapter(false, onClick, onClose)
        rightAdapter = TabsAdapter(false, onClick, onClose)
        panelAdapter = TabsAdapter(false, onClick, onClose)

        binding.tabStripBottom.setup(stripAdapter, RecyclerView.HORIZONTAL)
        binding.tabListLeft.setup(leftAdapter, RecyclerView.VERTICAL)
        binding.tabListRight.setup(rightAdapter, RecyclerView.VERTICAL)
        binding.panelTabList.setup(panelAdapter, RecyclerView.VERTICAL)
    }

    private fun RecyclerView.setup(tabsAdapter: TabsAdapter, orientation: Int) {
        layoutManager = LinearLayoutManager(this@MainActivity, orientation, false)
        adapter = tabsAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            currentWebView()?.takeIf { it.canGoBack() }?.goBack()
        }
        binding.btnForward.setOnClickListener {
            currentWebView()?.takeIf { it.canGoForward() }?.goForward()
        }
        binding.btnReload.setOnClickListener { currentWebView()?.reload() }

        binding.btnTabs.setOnClickListener {
            if (tabLayout == "off") binding.tabSlidingPanel.toggle() else viewModel.addTab()
        }
        binding.btnPanelAddTab.setOnClickListener {
            viewModel.addTab()
            binding.tabSlidingPanel.close()
        }

        binding.btnMenu.setOnClickListener { showMenu() }

        binding.btnShowToolbar.setOnClickListener {
            setToolbarHidden(false)
            binding.etUrlBar.requestFocus()
        }

        binding.etUrlBar.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event != null &&
                event.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_GO || enterPressed) {
                submitUrl()
                true
            } else {
                false
            }
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this) {
            val webView = currentWebView()
            when {
                binding.tabSlidingPanel.isOpen -> binding.tabSlidingPanel.close()
                webView != null && webView.canGoBack() -> webView.goBack()
                viewModel.tabs.value.size > 1 -> viewModel.closeTab(viewModel.currentTabId.value)
                else -> finish()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.urlPositionFlow.collect { urlPosition = it; applyLayout() }
                }
                launch {
                    viewModel.tabLayoutFlow.collect { tabLayout = it; applyLayout() }
                }
                launch {
                    viewModel.autohideFlow.collect { autohide = it; applyAutohide() }
                }
                launch {
                    viewModel.textSizeFlow.collect { size ->
                        textSize = size.coerceIn(50, 400)
                        webViews.values.forEach { it.settings.textZoom = textSize }
                    }
                }
                launch {
                    viewModel.uiItemSizeFlow.collect { itemScale = it; applyScale() }
                }
                launch {
                    viewModel.layoutSizeFlow.collect { layoutScale = it; applyScale() }
                }
                launch {
                    combine(viewModel.tabs, viewModel.currentTabId) { tabs, id -> tabs to id }
                        .collect { (tabs, id) -> syncTabs(tabs, id) }
                }
            }
        }
    }

    // ----------------------------------------------------------------- tabs

    private fun syncTabs(tabs: List<TabModel>, currentId: String) {
        val ids = tabs.map { it.id }.toSet()
        webViews.keys.filter { it !in ids }.forEach { id ->
            webViews.remove(id)?.let { destroyWebView(it) }
        }
        tabs.forEach { tab -> if (!webViews.containsKey(tab.id)) createWebView(tab) }

        webViews.forEach { (id, webView) ->
            if (id == currentId) {
                webView.show()
                webView.onResume()
            } else {
                webView.hide()
                webView.onPause()
            }
        }

        stripAdapter.update(tabs, currentId)
        leftAdapter.update(tabs, currentId)
        rightAdapter.update(tabs, currentId)
        panelAdapter.update(tabs, currentId)

        if (shownTabId != currentId) {
            shownTabId = currentId
            binding.progressBar.hide()
            binding.etUrlBar.setText(tabs.firstOrNull { it.id == currentId }?.url.orEmpty())
        }
        updateNavButtons()
    }

    private fun currentWebView(): WebView? = webViews[viewModel.currentTabId.value]

    private fun createWebView(tab: TabModel) {
        val webView = buildWebView(tab.id)
        webViews[tab.id] = webView
        binding.webViewContainer.addView(webView)
        if (tab.url.isBlank()) {
            lifecycleScope.launch { webView.loadUrl(viewModel.currentHomepage()) }
        } else {
            webView.loadUrl(tab.url)
        }
    }

    private fun buildWebView(tabId: String): WebView {
        val webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            textZoom = this@MainActivity.textSize.coerceIn(50, 400)
        }

        webView.webViewClient = BrowserWebViewClient(
            onPageStartedCallback = { url ->
                if (url != null) {
                    viewModel.updateTab(tabId, url = url)
                    if (isCurrent(tabId) && !binding.etUrlBar.hasFocus()) {
                        binding.etUrlBar.setText(url)
                    }
                }
                updateNavButtons()
            },
            onPageFinishedCallback = { url ->
                if (url != null) {
                    viewModel.updateTab(tabId, url = url)
                    val title = viewModel.tabs.value.firstOrNull { it.id == tabId }?.title.orEmpty()
                    viewModel.addHistory(title, url)
                }
                if (isCurrent(tabId)) binding.progressBar.hide()
                updateNavButtons()
            }
        )

        webView.webChromeClient = BrowserWebChromeClient(
            onProgressChangedCallback = { progress ->
                if (isCurrent(tabId)) {
                    binding.progressBar.progress = progress
                    binding.progressBar.setVisible(progress in 1..99)
                }
            },
            onTitleReceivedCallback = { title ->
                if (title != null) viewModel.updateTab(tabId, title = title)
            }
        )

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadHandler.downloadFile(url, userAgent, contentDisposition, mimeType)
        }

        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (autohide == "on" && isCurrent(tabId)) {
                val dy = scrollY - oldScrollY
                if (dy > SCROLL_THRESHOLD) {
                    setToolbarHidden(true)
                } else if (dy < -SCROLL_THRESHOLD) {
                    setToolbarHidden(false)
                }
            }
        }
        return webView
    }

    private fun destroyWebView(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.destroy()
    }

    private fun isCurrent(tabId: String) = viewModel.currentTabId.value == tabId

    private fun updateNavButtons() {
        val webView = currentWebView()
        binding.btnBack.alpha = if (webView?.canGoBack() == true) 1f else 0.4f
        binding.btnForward.alpha = if (webView?.canGoForward() == true) 1f else 0.4f
    }

    // ---------------------------------------------------------- url + menu

    private fun submitUrl() {
        val input = binding.etUrlBar.text.toString().trim()
        if (input.isEmpty()) return
        currentWebView()?.loadUrl(Constants.resolveInput(input))
        hideKeyboard()
        if (autohide == "on_and_lock") setToolbarHidden(true)
    }

    private fun hideKeyboard() {
        binding.etUrlBar.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etUrlBar.windowToken, 0)
    }

    private fun openExternalIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.let { viewModel.openUrl(it) }
        }
    }

    private fun showMenu() {
        CustomPopupDialog(
            context = this,
            anchorBottom = urlPosition != "top",
            onNewTabClick = { viewModel.addTab() },
            onHomeClick = {
                lifecycleScope.launch { currentWebView()?.loadUrl(viewModel.currentHomepage()) }
            },
            onAddBookmarkClick = {
                val tab = viewModel.tabs.value.firstOrNull { it.id == viewModel.currentTabId.value }
                if (tab != null && viewModel.addBookmark(tab.title, tab.url)) {
                    Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
                }
            },
            onBookmarksClick = { showBookmarks() },
            onSettingsClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        ).show()
    }

    private fun showBookmarks() {
        val items = viewModel.getBookmarks()
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.bookmarks_empty, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_bookmarks)
            .setItems(items.map { it.title }.toTypedArray()) { _, which ->
                currentWebView()?.loadUrl(items[which].url)
            }
            .show()
    }

    // ------------------------------------------------- layout / appearance

    private fun applyLayout() {
        val column = binding.mainColumn
        column.removeAllViews()
        if (urlPosition == "top") {
            column.addView(binding.toolbarContainer)
            column.addView(binding.middleRow)
            column.addView(binding.tabStripBottom)
        } else {
            column.addView(binding.middleRow)
            column.addView(binding.tabStripBottom)
            column.addView(binding.toolbarContainer)
        }

        binding.tabStripBottom.setVisible(tabLayout == "bottom_horizontal")
        binding.tabListLeft.setVisible(tabLayout == "left_vertical")
        binding.tabListRight.setVisible(tabLayout == "right_vertical")
        if (tabLayout != "off") binding.tabSlidingPanel.close()

        binding.btnShowToolbar.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = Gravity.START or (if (urlPosition == "top") Gravity.TOP else Gravity.BOTTOM)
        }

        binding.toolbarContainer.animate().cancel()
        binding.toolbarContainer.translationY = 0f
        binding.toolbarContainer.alpha = 1f
        binding.toolbarContainer.setVisible(!toolbarHidden)
        binding.btnShowToolbar.setVisible(toolbarHidden)

        applyScale()
        applyAutohide()
    }

    private fun applyScale() {
        val density = resources.displayMetrics.density
        val button = (BUTTON_DP * density * itemScale).roundToInt()
        listOf(
            binding.btnBack, binding.btnForward, binding.btnReload,
            binding.btnTabs, binding.btnMenu
        ).forEach { view ->
            view.updateLayoutParams {
                width = button
                height = button
            }
        }
        binding.etUrlBar.updateLayoutParams { height = button }
        binding.etUrlBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f * itemScale)

        val toolbarHeight = max(
            (TOOLBAR_DP * density * layoutScale).roundToInt(),
            button + (8 * density).roundToInt()
        )
        binding.urlToolbar.updateLayoutParams { height = toolbarHeight }
        binding.tabStripBottom.updateLayoutParams {
            height = (TAB_BAR_DP * density * layoutScale).roundToInt()
        }
        val sidePanelWidth = (SIDE_PANEL_DP * density * layoutScale).roundToInt()
        binding.tabListLeft.updateLayoutParams { width = sidePanelWidth }
        binding.tabListRight.updateLayoutParams { width = sidePanelWidth }
    }

    private fun applyAutohide() {
        when (autohide) {
            "off" -> setToolbarHidden(false)
            "on_and_lock" -> setToolbarHidden(true)
            else -> Unit
        }
    }

    private fun setToolbarHidden(hidden: Boolean) {
        if (toolbarHidden == hidden) return
        toolbarHidden = hidden
        val container = binding.toolbarContainer
        container.animate().cancel()
        if (hidden) {
            val direction = if (urlPosition == "top") -1f else 1f
            container.animate()
                .translationY(direction * container.height)
                .alpha(0f)
                .setDuration(200)
                .withEndAction { if (toolbarHidden) container.hide() }
                .start()
            binding.btnShowToolbar.show()
        } else {
            container.show()
            container.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(200)
                .start()
            binding.btnShowToolbar.hide()
        }
    }

    private companion object {
        const val SCROLL_THRESHOLD = 12
        const val BUTTON_DP = 40f
        const val TOOLBAR_DP = 56f
        const val TAB_BAR_DP = 48f
        const val SIDE_PANEL_DP = 160f
    }
}
