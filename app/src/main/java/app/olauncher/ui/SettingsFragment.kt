package app.olauncher.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.bundleOf
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.BuildConfig
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.ContextualRule
import app.olauncher.data.DeviceContextSignals
import app.olauncher.data.Prefs
import app.olauncher.data.SettingsBackup
import app.olauncher.databinding.DialogTextSizeBinding
import app.olauncher.databinding.FragmentSettingsBinding
import app.olauncher.helper.appUsagePermissionGranted
import app.olauncher.helper.createDialog
import app.olauncher.helper.dpToPx
import app.olauncher.helper.getColorFromAttr
import app.olauncher.helper.hideStatusBar
import app.olauncher.helper.isAccessServiceEnabled
import app.olauncher.helper.isDarkThemeOn
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isOlauncherDefault
import app.olauncher.helper.isTablet
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.openUrl
import app.olauncher.helper.setPlainWallpaper
import app.olauncher.helper.OlDialog
import app.olauncher.helper.showPopupMenu
import app.olauncher.helper.showStatusBar
import app.olauncher.helper.showToast
import app.olauncher.listener.DeviceAdmin
import java.util.Locale
import java.util.Calendar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.concurrent.Executors

class SettingsFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var dialog: OlDialog? = null
    private var afterBluetoothPermission: (() -> Unit)? = null
    private val settingsExecutor = Executors.newSingleThreadExecutor()
    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        prefs.agendaEnabled = granted
        populateInfoWidgets()
    }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        prefs.weatherEnabled = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        populateInfoWidgets()
    }
    private val homeLocationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true || grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            captureHomeLocation()
        }
    }
    private val wifiPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) saveCurrentWifiAsHome()
        else requireContext().showToast(R.string.wifi_details_unavailable, Toast.LENGTH_LONG)
    }
    private val bluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterBluetoothPermission?.invoke()
        afterBluetoothPermission = null
    }
    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        val appContext = requireContext().applicationContext
        settingsExecutor.execute {
            val result = runCatching { SettingsBackup(appContext).exportTo(uri) }
            result.exceptionOrNull()?.let { Log.e("SettingsBackup", "Could not export settings", it) }
            activity?.runOnUiThread { appContext.showToast(if (result.isSuccess) R.string.backup_exported else R.string.backup_export_failed) }
        }
    }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        val appContext = requireContext().applicationContext
        settingsExecutor.execute {
            val success = runCatching { SettingsBackup(appContext).importFrom(uri) }.isSuccess
            activity?.runOnUiThread {
                appContext.showToast(if (success) R.string.backup_imported else R.string.backup_failed)
                if (success) requireActivity().recreate()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        viewModel.isOlauncherDefault()

        deviceManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()

        binding.homeAppsNum.text = prefs.homeAppsNum.toString()
        populateKeyboardText()
        populateScreenTimeOnOff()
        populateMindfulApps()
        populateContextualSuggestions()
        populateLockSettings()
        // Home button for recents feature disabled
        // populateHomeButtonRecents()
        populateWallpaperText()
        populateAppThemeText()
        populateTextSize()
        populateBoldFont()
        populateAlignment()
        populateStatusBar()
        populateDateTime()
        populateInfoWidgets()
        populateSwipeApps()
        initClickListeners()
        initObservers()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.olauncherHiddenApps -> showHiddenApps()
            R.id.screenTimeOnOff -> viewModel.showDialog.postValue(Constants.Dialog.DIGITAL_WELLBEING)
            R.id.mindfulApps -> showAppListIfEnabled(Constants.FLAG_MINDFUL_APPS)
            R.id.contextualSuggestions -> toggleContextualSuggestions()
            R.id.contextualApps -> showAppListIfEnabled(Constants.FLAG_CONTEXTUAL_APPS)
            R.id.suggestionDisplay -> toggleSuggestionDisplay()
            R.id.suggestionRules -> showSuggestionRules()
            R.id.homeLocation -> saveCurrentLocationAsHome()
            R.id.homeWifi -> saveCurrentWifiAsHome()
            R.id.carBluetooth -> selectCarBluetooth()
            R.id.exportSettings -> createBackup.launch("olauncher-settings-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())}.json")
            R.id.importSettings -> openBackup.launch(arrayOf("application/json", "text/plain"))
            R.id.appInfo -> openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            R.id.setLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.toggleLock -> toggleLockMode()
            // Home button for recents feature disabled
            // R.id.homeButtonRecents -> toggleHomeButtonRecents()
            R.id.autoShowKeyboard -> toggleKeyboardText()
            R.id.homeAppsNum -> showHomeAppsNumMenu(view)
            R.id.appSpacing -> showAppSpacingDialog()
            R.id.dailyWallpaperUrl -> requireContext().openUrl(prefs.dailyWallpaperUrl)
            R.id.dailyWallpaper -> toggleDailyWallpaperUpdate()
            R.id.alignment -> showAlignmentMenu(view)
            R.id.statusBar -> toggleStatusBar()
            R.id.dateTime -> showDateTimeMenu(view)
            R.id.agendaOnOff -> toggleAgenda()
            R.id.weatherOnOff -> toggleWeather()
            R.id.appThemeText -> showAppThemeMenu(view, showSystem = false)
            R.id.textSizeValue -> showTextSizeDialog()
            R.id.boldFont -> toggleBoldFont()

            R.id.swipeLeftApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_LEFT_APP)
            R.id.swipeRightApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_RIGHT_APP)

        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.alignment -> {
                prefs.appLabelAlignment = prefs.homeAlignment
                findNavController().navigate(R.id.action_settingsFragment_to_appListFragment)
                requireContext().showToast(getString(R.string.alignment_changed))
            }

            R.id.dailyWallpaper -> removeWallpaper()
            R.id.appThemeText -> showAppThemeMenu(view, showSystem = true)
            R.id.swipeLeftApp -> toggleSwipeLeft()
            R.id.swipeRightApp -> toggleSwipeRight()
            R.id.toggleLock -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Turning the service off needs no disclosure, turning it on does
                if (isAccessServiceEnabled(requireContext()))
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                else
                    showAccessibilityDialog()
            }
            R.id.weatherOnOff -> showManualLocationDialog()
        }
        return true
    }

    private fun initClickListeners() {
        binding.olauncherHiddenApps.setOnClickListener(this)
        binding.appInfo.setOnClickListener(this)
        binding.setLauncher.setOnClickListener(this)
        binding.autoShowKeyboard.setOnClickListener(this)
        binding.toggleLock.setOnClickListener(this)
        // Home button for recents feature disabled
        // binding.homeButtonRecents.setOnClickListener(this)
        binding.homeAppsNum.setOnClickListener(this)
        binding.appSpacing.setOnClickListener(this)
        binding.screenTimeOnOff.setOnClickListener(this)
        binding.mindfulApps.setOnClickListener(this)
        binding.contextualSuggestions.setOnClickListener(this)
        binding.contextualApps.setOnClickListener(this)
        binding.suggestionDisplay.setOnClickListener(this)
        binding.suggestionRules.setOnClickListener(this)
        binding.homeLocation.setOnClickListener(this)
        binding.homeWifi.setOnClickListener(this)
        binding.carBluetooth.setOnClickListener(this)
        binding.exportSettings.setOnClickListener(this)
        binding.importSettings.setOnClickListener(this)
        binding.dailyWallpaperUrl.setOnClickListener(this)
        binding.dailyWallpaper.setOnClickListener(this)
        binding.alignment.setOnClickListener(this)
        binding.statusBar.setOnClickListener(this)
        binding.dateTime.setOnClickListener(this)
        binding.agendaOnOff?.setOnClickListener(this)
        binding.weatherOnOff?.setOnClickListener(this)
        binding.weatherOnOff?.setOnLongClickListener(this)
        binding.swipeLeftApp.setOnClickListener(this)
        binding.swipeRightApp.setOnClickListener(this)
        binding.appThemeText.setOnClickListener(this)
        binding.textSizeValue.setOnClickListener(this)
        binding.boldFont.setOnClickListener(this)

        binding.dailyWallpaper.setOnLongClickListener(this)
        binding.alignment.setOnLongClickListener(this)
        binding.appThemeText.setOnLongClickListener(this)
        binding.swipeLeftApp.setOnLongClickListener(this)
        binding.swipeRightApp.setOnLongClickListener(this)
        binding.toggleLock.setOnLongClickListener(this)
    }

    private fun initObservers() {
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner) {
            if (it) {
                binding.setLauncher.text = getString(R.string.change_default_launcher)
            }
        }
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            populateAlignment()
        }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) {
            populateSwipeApps()
        }
    }

    // Popup menus

    private fun showHomeAppsNumMenu(anchor: View) {
        anchor.showPopupMenu(
            configure = { menu ->
                for (num in 0..8) menu.add(Menu.NONE, num, num, num.toString())
            }
        ) { item -> updateHomeAppsNum(item.itemId) }
    }

    private fun showDateTimeMenu(anchor: View) {
        anchor.showPopupMenu(R.menu.date_time) { item ->
            when (item.itemId) {
                R.id.dateTimeOn -> toggleDateTime(Constants.DateTime.ON)
                R.id.dateTimeOff -> toggleDateTime(Constants.DateTime.OFF)
                R.id.dateOnly -> toggleDateTime(Constants.DateTime.DATE_ONLY)
            }
        }
    }

    private fun showAlignmentMenu(anchor: View) {
        anchor.showPopupMenu(
            R.menu.alignment,
            configure = { menu ->
                menu.findItem(R.id.alignmentBottom).setTitle(
                    if (prefs.homeBottomAlignment) R.string.bottom_on else R.string.bottom_off
                )
            }
        ) { item ->
            when (item.itemId) {
                R.id.alignmentLeft -> viewModel.updateHomeAlignment(Gravity.START)
                R.id.alignmentCenter -> viewModel.updateHomeAlignment(Gravity.CENTER)
                R.id.alignmentRight -> viewModel.updateHomeAlignment(Gravity.END)
                R.id.alignmentBottom -> updateHomeBottomAlignment()
            }
        }
    }

    // "System" stays hidden unless the row is long pressed
    private fun showAppThemeMenu(anchor: View, showSystem: Boolean) {
        anchor.showPopupMenu(
            R.menu.app_theme,
            configure = { menu -> menu.findItem(R.id.themeSystem).isVisible = showSystem }
        ) { item ->
            when (item.itemId) {
                R.id.themeLight -> updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
                R.id.themeDark -> updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
                R.id.themeSystem -> updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    // Dialogs

    private fun showDialog(newDialog: OlDialog) {
        dialog?.dismiss()
        dialog = newDialog
        newDialog.showRespectingStatusBar()
    }

    private fun showTextSizeDialog() {
        var stepper: DialogTextSizeBinding? = null
        val dialog = requireContext().createDialog(R.string.text_size, R.string.okay) { container ->
            DialogTextSizeBinding.inflate(layoutInflater, container, false).also { stepper = it }.root
        }
        stepper?.apply {
            textSizeCurrent.text = formatScale(pendingOrCurrentTextSizeScale())
            textSizeMinus.setOnClickListener { adjustTextSizePreview(-0.1f, this) }
            textSizePlus.setOnClickListener { adjustTextSizePreview(0.1f, this) }
        }
        dialog.setOnDismissListener { applyTextSizeScale() }
        showDialog(dialog)
    }

    private fun showAppSpacingDialog() {
        var stepper: DialogTextSizeBinding? = null
        val dialog = requireContext().createDialog(R.string.app_spacing, R.string.okay) { container ->
            DialogTextSizeBinding.inflate(layoutInflater, container, false).also { stepper = it }.root
        }
        stepper?.apply {
            textSizeCurrent.text = formatScale(pendingOrCurrentAppSpacingScale())
            textSizeMinus.setOnClickListener { adjustAppSpacingPreview(-0.1f, this) }
            textSizePlus.setOnClickListener { adjustAppSpacingPreview(0.1f, this) }
        }
        dialog.setOnDismissListener { applyAppSpacingScale() }
        showDialog(dialog)
    }

    // Play policy: prominent disclosure and an explicit Accept before sending the user to
    // accessibility settings. Not now, the close icon, back or tapping outside never count as consent.
    private fun showAccessibilityDialog() {
        showDialog(
            requireContext().createDialog(
                title = R.string.accessibility_permission,
                action = R.string.accept,
                message = R.string.accessibility_disclosure,
                neutral = R.string.not_now,
                onAction = { openAccessibilityService() },
            )
        )
    }

    private fun toggleSwipeLeft() {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        if (prefs.swipeLeftEnabled) {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_left_app_enabled))
        } else {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_left_app_disabled))
        }
    }

    private fun toggleSwipeRight() {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        if (prefs.swipeRightEnabled) {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_right_app_enabled))
        } else {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_right_app_disabled))
        }
    }

    private fun toggleStatusBar() {
        prefs.showStatusBar = !prefs.showStatusBar
        populateStatusBar()
    }

    private fun populateStatusBar() {
        if (prefs.showStatusBar) {
            requireActivity().window.showStatusBar()
            binding.statusBar.text = getString(R.string.on)
        } else {
            requireActivity().window.hideStatusBar()
            binding.statusBar.text = getString(R.string.off)
        }
    }

    private fun toggleDateTime(selected: Int) {
        prefs.dateTimeVisibility = selected
        populateDateTime()
        viewModel.toggleDateTime()
    }

    private fun populateDateTime() {
        binding.dateTime.text = getString(
            when (prefs.dateTimeVisibility) {
                Constants.DateTime.DATE_ONLY -> R.string.date
                Constants.DateTime.ON -> R.string.on
                else -> R.string.off
            }
        )
    }

    private fun toggleAgenda() {
        if (prefs.agendaEnabled) {
            prefs.agendaEnabled = false
            populateInfoWidgets()
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            prefs.agendaEnabled = true
            populateInfoWidgets()
        } else calendarPermission.launch(Manifest.permission.READ_CALENDAR)
    }

    private fun toggleWeather() {
        if (prefs.weatherEnabled) {
            prefs.weatherEnabled = false
            populateInfoWidgets()
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            prefs.weatherEnabled = true
            populateInfoWidgets()
        } else requestLocationPermission()
    }

    private fun populateInfoWidgets() {
        binding.agendaOnOff?.setText(if (prefs.agendaEnabled) R.string.on else R.string.off)
        binding.weatherOnOff?.text = when {
            !prefs.weatherEnabled -> getString(R.string.off)
            prefs.weatherManual -> prefs.weatherLocationName
            else -> getString(R.string.on)
        }
    }

    @Suppress("DEPRECATION")
    private fun showManualLocationDialog() {
        var input: EditText? = null
        val locationDialog = requireContext().createDialog(
            title = R.string.set_location,
            action = R.string.okay,
            neutral = R.string.automatic_location,
            onNeutral = {
                prefs.weatherManual = false
                prefs.weatherUpdatedAt = 0L
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    prefs.weatherEnabled = true
                    populateInfoWidgets()
                } else requestLocationPermission()
            },
            onAction = {
                val query = input?.text?.toString()?.trim().orEmpty()
                val appContext = requireContext().applicationContext
                settingsExecutor.execute {
                    val address = runCatching { Geocoder(appContext, Locale.getDefault()).getFromLocationName(query, 1)?.firstOrNull() }.getOrNull()
                    activity?.runOnUiThread {
                        if (address == null) appContext.showToast(R.string.location_not_found)
                        else {
                            prefs.weatherLatitude = address.latitude.toFloat()
                            prefs.weatherLongitude = address.longitude.toFloat()
                            prefs.weatherLocationName = query.ifBlank {
                                address.subLocality ?: address.locality ?: address.subAdminArea ?: address.adminArea.orEmpty()
                            }
                            prefs.weatherManual = true
                            prefs.weatherEnabled = true
                            prefs.weatherUpdatedAt = 0L
                            if (_binding != null) populateInfoWidgets()
                        }
                    }
                }
            },
        ) { container ->
            EditText(requireContext()).also {
                input = it
                it.hint = getString(R.string.manual_location_hint)
                it.setSingleLine(true)
            }
        }
        showDialog(locationDialog)
    }

    private fun requestLocationPermission() {
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )
    }

    private fun showHiddenApps() {
        if (prefs.hiddenApps.isEmpty()) {
            requireContext().showToast(getString(R.string.no_hidden_apps))
            return
        }
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS)
        )
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun openAccessibilityService() {
        // prefs.lockModeOn = true
        populateLockSettings()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun toggleLockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!prefs.lockModeOn && !isAccessServiceEnabled(requireContext())) {
                showAccessibilityDialog()
                return
            }
            prefs.lockModeOn = !prefs.lockModeOn
        } else {
            val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
            if (isAdmin) {
                removeActiveAdmin("Admin permission removed.")
                prefs.lockModeOn = false
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_permission_message)
                )
                requireActivity().startActivityForResult(intent, Constants.REQUEST_CODE_ENABLE_ADMIN)
            }
        }
        populateLockSettings()
    }

    private fun removeActiveAdmin(toastMessage: String? = null) {
        try {
            deviceManager.removeActiveAdmin(componentName) // for backward compatibility
            requireContext().showToast(toastMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeWallpaper() {
        if (requireContext().isEinkDisplay()) {
            prefs.appTheme = AppCompatDelegate.MODE_NIGHT_NO
            setPlainWallpaper(requireContext(), android.R.color.white)
        } else {
            prefs.appTheme = AppCompatDelegate.MODE_NIGHT_YES
            setPlainWallpaper(requireContext(), android.R.color.black)
        }
        if (!prefs.dailyWallpaper) return
        prefs.dailyWallpaper = false
        populateWallpaperText()
        viewModel.cancelWallpaperWorker()
    }

    private fun toggleDailyWallpaperUpdate() {
        if (prefs.dailyWallpaper.not() && prefs.appTheme == AppCompatDelegate.MODE_NIGHT_YES && viewModel.isOlauncherDefault.value == false) {
            requireContext().showToast(R.string.set_as_default_launcher_first)
            return
        }
        prefs.dailyWallpaper = !prefs.dailyWallpaper
        populateWallpaperText()
        if (prefs.dailyWallpaper) {
            viewModel.setWallpaperWorker()
            showWallpaperToasts()
        } else viewModel.cancelWallpaperWorker()
    }

    private fun showWallpaperToasts() {
        if (isOlauncherDefault(requireContext()))
            requireContext().showToast(getString(R.string.your_wallpaper_will_update_shortly))
        else
            requireContext().showToast(getString(R.string.olauncher_is_not_default_launcher), Toast.LENGTH_LONG)
    }

    private fun updateHomeAppsNum(num: Int) {
        binding.homeAppsNum.text = num.toString()
        prefs.homeAppsNum = num
        viewModel.refreshHome(true)
    }

    private var pendingTextSizeScale: Float = -1f

    private fun pendingOrCurrentTextSizeScale(): Float =
        if (pendingTextSizeScale > 0) pendingTextSizeScale else prefs.textSizeScale

    private fun formatScale(scale: Float): String = String.format("%.1f", scale)

    private fun adjustTextSizePreview(delta: Float, dialogBinding: DialogTextSizeBinding) {
        val maxScale = if (isTablet(requireContext())) 2.0f else 1.5f
        val current = pendingOrCurrentTextSizeScale()
        val newScale = Math.round((current + delta) * 10f) / 10f
        val clamped = newScale.coerceIn(0.5f, maxScale)
        if (clamped == current) return
        pendingTextSizeScale = clamped
        val formatted = formatScale(clamped)
        binding.textSizeValue.text = formatted
        dialogBinding.textSizeCurrent.text = formatted
    }

    private fun applyTextSizeScale() {
        if (pendingTextSizeScale < 0 || prefs.textSizeScale == pendingTextSizeScale) {
            pendingTextSizeScale = -1f
            return
        }
        prefs.textSizeScale = pendingTextSizeScale
        pendingTextSizeScale = -1f
        val activity = activity ?: return
        if (activity.isChangingConfigurations.not())
            activity.recreate()
    }

    private var pendingAppSpacingScale: Float = -1f

    private fun pendingOrCurrentAppSpacingScale(): Float =
        if (pendingAppSpacingScale >= 0) pendingAppSpacingScale else prefs.appSpacingScale

    private fun adjustAppSpacingPreview(delta: Float, dialogBinding: DialogTextSizeBinding) {
        val current = pendingOrCurrentAppSpacingScale()
        val newScale = Math.round((current + delta) * 10f) / 10f
        val clamped = newScale.coerceIn(0f, 2f)
        if (clamped == current) return
        pendingAppSpacingScale = clamped
        val formatted = formatScale(clamped)
        binding.appSpacing.text = formatted
        dialogBinding.textSizeCurrent.text = formatted
    }

    private fun applyAppSpacingScale() {
        if (pendingAppSpacingScale < 0 || prefs.appSpacingScale == pendingAppSpacingScale) {
            pendingAppSpacingScale = -1f
            return
        }
        prefs.appSpacingScale = pendingAppSpacingScale
        pendingAppSpacingScale = -1f
        viewModel.refreshHome(false)
    }

    private fun toggleKeyboardText() {
        if (prefs.autoShowKeyboard && prefs.keyboardMessageShown.not()) {
            viewModel.showDialog.postValue(Constants.Dialog.KEYBOARD)
            prefs.keyboardMessageShown = true
        } else {
            prefs.autoShowKeyboard = !prefs.autoShowKeyboard
            populateKeyboardText()
        }
    }

    private fun updateTheme(appTheme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == appTheme) return
        if (prefs.dailyWallpaper) applyTheme(appTheme)
        else showThemeWallpaperDialog(appTheme)
    }

    private fun applyTheme(appTheme: Int) {
        prefs.appTheme = appTheme
        populateAppThemeText(appTheme)
        setAppTheme(appTheme)
    }

    // Daily wallpaper off: keep the user's own wallpaper, or switch to a plain one matching the theme.
    // Closing the dialog (X or back) cancels the theme change.
    private fun showThemeWallpaperDialog(appTheme: Int) {
        val dark = when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> Resources.getSystem().configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        val color = if (dark) android.R.color.black else android.R.color.white
        showDialog(
            requireContext().createDialog(
                title = R.string.wallpaper,
                action = if (dark) R.string.black else R.string.white,
                message = R.string.theme_wallpaper_message,
                neutral = R.string.keep_current,
                onNeutral = { applyTheme(appTheme) },
                onAction = {
                    setPlainWallpaper(requireContext(), color)
                    applyTheme(appTheme)
                },
            )
        )
    }

    private fun setAppTheme(theme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == theme) return
        if (prefs.dailyWallpaper) {
            setPlainWallpaper(theme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun setPlainWallpaper(appTheme: Int) {
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> setPlainWallpaper(requireContext(), android.R.color.black)
            AppCompatDelegate.MODE_NIGHT_NO -> setPlainWallpaper(requireContext(), android.R.color.white)
            else -> {
                if (requireContext().isDarkThemeOn())
                    setPlainWallpaper(requireContext(), android.R.color.black)
                else setPlainWallpaper(requireContext(), android.R.color.white)
            }
        }
    }

    private fun populateAppThemeText(appTheme: Int = prefs.appTheme) {
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> binding.appThemeText.text = getString(R.string.dark)
            AppCompatDelegate.MODE_NIGHT_NO -> binding.appThemeText.text = getString(R.string.light)
            else -> binding.appThemeText.text = getString(R.string.system_default)
        }
    }

    private fun populateTextSize() {
        binding.textSizeValue.text = formatScale(prefs.textSizeScale)
    }

    private fun toggleBoldFont() {
        prefs.boldFont = !prefs.boldFont
        populateBoldFont()
        requireActivity().recreate()
    }

    private fun populateBoldFont() {
        binding.boldFont.text = getString(if (prefs.boldFont) R.string.on else R.string.off)
    }

    private fun populateScreenTimeOnOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (requireContext().appUsagePermissionGranted()) binding.screenTimeOnOff.text = getString(R.string.on)
            else binding.screenTimeOnOff.text = getString(R.string.off)
        } else binding.screenTimeLayout.visibility = View.GONE
    }

    private fun populateMindfulApps() {
        binding.mindfulApps.text = NumberFormat.getIntegerInstance().format(prefs.mindfulApps.size)
    }

    private fun toggleContextualSuggestions() {
        prefs.contextualSuggestionsEnabled = !prefs.contextualSuggestionsEnabled
        populateContextualSuggestions()
    }

    private fun populateContextualSuggestions() {
        val enabled = prefs.contextualSuggestionsEnabled
        binding.contextualSuggestions.setText(if (enabled) R.string.on else R.string.off)
        binding.contextualApps.text = NumberFormat.getIntegerInstance().format(prefs.contextualApps.size)
        binding.suggestionDisplay.setText(if (prefs.contextualSuggestionsMixed) R.string.mixed else R.string.separate)
        binding.homeLocation.setText(if (prefs.homeLocationSet) R.string.on else R.string.set)
        val homeWifiNames = prefs.homeWifiNames.sorted().joinToString(", ")
        binding.homeWifi.text = when {
            homeWifiNames.isBlank() -> getString(R.string.set)
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ->
                getString(R.string.permission_required_value, homeWifiNames)
            else -> homeWifiNames
        }
        binding.carBluetooth.text = prefs.carBluetoothName.ifBlank { getString(R.string.set) }
        listOf(
            binding.contextualAppsRow to binding.contextualApps,
            binding.suggestionDisplayRow to binding.suggestionDisplay,
            binding.suggestionRulesRow to binding.suggestionRules,
            binding.homeLocationRow to binding.homeLocation,
            binding.homeWifiRow to binding.homeWifi,
            binding.carBluetoothRow to binding.carBluetooth,
        ).forEach { (row, action) ->
            (row ?: action).alpha = if (enabled) 1f else 0.45f
            action.isEnabled = enabled
        }
        populateAppSpacing()
    }

    private fun populateAppSpacing() {
        binding.appSpacing.text = formatScale(prefs.appSpacingScale)
    }

    private fun toggleSuggestionDisplay() {
        prefs.contextualSuggestionsMixed = !prefs.contextualSuggestionsMixed
        populateContextualSuggestions()
    }

    private fun showSuggestionRules() {
        if (prefs.contextualApps.isEmpty()) {
            requireContext().showToast(R.string.select_suggested_apps_first)
            return
        }
        val launcher = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val configuredRules = prefs.contextualRules()
        val apps = prefs.contextualApps.mapNotNull { identity ->
            val separator = identity.indexOf('|')
            if (separator <= 0) return@mapNotNull null
            val packageName = identity.substring(0, separator)
            val user = app.olauncher.helper.getUserHandleFromString(requireContext(), identity.substring(separator + 1))
            launcher.getActivityList(packageName, user).firstOrNull()?.let { identity to it.label.toString() }
        }.sortedBy { it.second.lowercase(Locale.getDefault()) }
        showDialog(requireContext().createDialog(R.string.suggestion_rules, R.string.close) { _ ->
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                apps.forEach { (identity, label) ->
                    addView(TextView(context).apply {
                        setTextAppearance(R.style.TextSmall)
                        text = label
                        if (identity in configuredRules) {
                            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
                            compoundDrawablePadding = 8.dpToPx()
                        }
                        setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                        setOnClickListener {
                            dialog?.dismiss()
                            showRuleEditor(identity, label)
                        }
                    })
                }
            }
        })
    }

    private fun showRuleEditor(identity: String, label: String) {
        val existing = prefs.contextualRules()[identity] ?: ContextualRule(identity)
        var startInput: EditText? = null
        var endInput: EditText? = null
        var keywordsInput: EditText? = null
        var cooldownInput: EditText? = null
        var outsideHomeInput: TextView? = null
        var atHomeInput: TextView? = null
        var outsideHome = existing.outsideHome
        var atHome = existing.atHome
        var drivingAndroidAuto = existing.drivingAndroidAuto
        val selectedBluetoothDevices = existing.bluetoothDeviceAddresses.toMutableSet().apply {
            if (existing.drivingBluetooth) prefs.carBluetoothAddress.takeIf(String::isNotBlank)?.let(::add)
        }
        val preserveLegacyCarRule = existing.drivingBluetooth && prefs.carBluetoothAddress.isBlank()
        val selectedDays = existing.daysOfWeek.toMutableSet()
        val editor = requireContext().createDialog(
            title = R.string.configure_rule,
            action = R.string.okay,
            onAction = {
                val parsedStart = parseTime(startInput?.text?.toString().orEmpty())
                val parsedEnd = parseTime(endInput?.text?.toString().orEmpty())
                val hasCompleteTimeRange = parsedStart != null && parsedEnd != null
                prefs.setContextualRule(ContextualRule(
                    appIdentity = identity,
                    startMinutes = parsedStart.takeIf { hasCompleteTimeRange },
                    endMinutes = parsedEnd.takeIf { hasCompleteTimeRange },
                    eventKeywords = keywordsInput?.text?.toString().orEmpty()
                        .split(',').map(String::trim).filter(String::isNotEmpty).toSet(),
                    outsideHome = outsideHome,
                    atHome = atHome,
                    drivingBluetooth = preserveLegacyCarRule,
                    bluetoothDeviceAddresses = selectedBluetoothDevices.toSet(),
                    drivingAndroidAuto = drivingAndroidAuto,
                    cooldownMinutes = cooldownInput?.text?.toString()?.toIntOrNull()?.coerceIn(0, 240) ?: 0,
                    daysOfWeek = selectedDays.toSet(),
                ))
            },
        ) { _ ->
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                addView(FrameLayout(context).apply {
                    addView(TextView(context).apply {
                        setTextAppearance(R.style.TextSmallBold)
                        text = label
                        setPadding(0, 12.dpToPx(), 48.dpToPx(), 12.dpToPx())
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
                    addView(ImageView(context).apply {
                        setImageResource(R.drawable.ic_info)
                        contentDescription = getString(R.string.info)
                        setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
                        setOnClickListener { showRuleInfo() }
                    }, FrameLayout.LayoutParams(48.dpToPx(), 48.dpToPx(), Gravity.END))
                })
                startInput = EditText(context).apply {
                    hint = getString(R.string.start_time_hint)
                    setText(formatMinutes(existing.startMinutes))
                    inputType = android.text.InputType.TYPE_CLASS_DATETIME or android.text.InputType.TYPE_DATETIME_VARIATION_TIME
                }.also(::addView)
                endInput = EditText(context).apply {
                    hint = getString(R.string.end_time_hint)
                    setText(formatMinutes(existing.endMinutes))
                    inputType = android.text.InputType.TYPE_CLASS_DATETIME or android.text.InputType.TYPE_DATETIME_VARIATION_TIME
                }.also(::addView)
                addView(TextView(context).apply {
                    setTextAppearance(R.style.TextSmall)
                    setText(R.string.rule_days_of_week)
                    setPadding(0, 16.dpToPx(), 0, 8.dpToPx())
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val dayValues = intArrayOf(
                        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY,
                    )
                    val dayLetters = resources.getStringArray(R.array.rule_day_letters)
                    val dayNames = resources.getStringArray(R.array.rule_day_names)
                    dayValues.forEachIndexed { index, day ->
                        addView(TextView(context).apply {
                            gravity = Gravity.CENTER
                            text = dayLetters[index]
                            contentDescription = dayNames[index]
                            fun updateSelectedState() {
                                val selected = day in selectedDays
                                setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
                                setTextColor(context.getColorFromAttr(if (selected) R.attr.primaryInverseColor else R.attr.primaryColorTrans50))
                                background = if (selected) GradientDrawable().apply {
                                    shape = GradientDrawable.OVAL
                                    setColor(context.getColorFromAttr(R.attr.primaryColor))
                                } else null
                                isSelected = selected
                            }
                            updateSelectedState()
                            setOnClickListener {
                                if (!selectedDays.add(day)) selectedDays.remove(day)
                                updateSelectedState()
                            }
                        }, LinearLayout.LayoutParams(0, 36.dpToPx(), 1f).apply {
                            marginStart = 2.dpToPx()
                            marginEnd = 2.dpToPx()
                        })
                    }
                })
                keywordsInput = EditText(context).apply {
                    hint = getString(R.string.event_keywords_hint)
                    setText(existing.eventKeywords.joinToString(", "))
                }.also(::addView)
                addView(TextView(context).apply {
                    setTextAppearance(R.style.TextSmall)
                    fun update() {
                        text = "${getString(R.string.outside_home_rule)}: ${getString(if (outsideHome) R.string.on else R.string.off)}"
                    }
                    update()
                    setPadding(0, 16.dpToPx(), 0, 12.dpToPx())
                    setOnClickListener {
                        outsideHome = !outsideHome
                        if (outsideHome) atHome = false
                        update()
                        atHomeInput?.text = "${getString(R.string.at_home_rule)}: ${getString(if (atHome) R.string.on else R.string.off)}"
                    }
                }.also { outsideHomeInput = it })
                addView(TextView(context).apply {
                    setTextAppearance(R.style.TextSmall)
                    fun update() {
                        text = "${getString(R.string.at_home_rule)}: ${getString(if (atHome) R.string.on else R.string.off)}"
                    }
                    update()
                    setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                    setOnClickListener {
                        atHome = !atHome
                        if (atHome) outsideHome = false
                        update()
                        outsideHomeInput?.text = "${getString(R.string.outside_home_rule)}: ${getString(if (outsideHome) R.string.on else R.string.off)}"
                    }
                }.also { atHomeInput = it })
                addView(TextView(context).apply {
                    setTextAppearance(R.style.TextSmall)
                    fun update() {
                        val devicesByAddress = DeviceContextSignals.pairedBluetoothDevices(context)
                            .associate { it.address to it.name }
                        val names = selectedBluetoothDevices.map { devicesByAddress[it] ?: it }.sorted()
                        text = "${getString(R.string.bluetooth_devices_rule)}: ${names.ifEmpty { listOf(getString(R.string.none)) }.joinToString(", ")}"
                    }
                    update()
                    setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                    setOnClickListener {
                        selectRuleBluetoothDevices(selectedBluetoothDevices, ::update)
                    }
                })
                addView(TextView(context).apply {
                    setTextAppearance(R.style.TextSmall)
                    fun update() {
                        text = "${getString(R.string.driving_android_auto_rule)}: ${getString(if (drivingAndroidAuto) R.string.on else R.string.off)}"
                    }
                    update()
                    setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                    setOnClickListener { drivingAndroidAuto = !drivingAndroidAuto; update() }
                })
                addView(TextView(context).apply {
                    setTextAppearance(R.style.TextSmall)
                    setText(R.string.cooldown_minutes_label)
                    setPadding(0, 12.dpToPx(), 0, 0)
                })
                cooldownInput = EditText(context).apply {
                    hint = "0"
                    setText(existing.cooldownMinutes.takeIf { it > 0 }?.toString().orEmpty())
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    maxLines = 1
                }.also(::addView)
            }
        }
        showDialog(editor)
    }

    private fun showRuleInfo() {
        requireContext().createDialog(
            title = R.string.info,
            action = R.string.close,
            message = R.string.rule_info_message,
        ).showRespectingStatusBar()
    }

    private fun parseTime(value: String): Int? {
        val parts = value.trim().split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun formatMinutes(value: Int?): String = value?.let { "%02d:%02d".format(Locale.US, it / 60, it % 60) }.orEmpty()

    private fun saveCurrentLocationAsHome() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            captureHomeLocation()
        } else {
            homeLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    private fun saveCurrentWifiAsHome() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            wifiPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        val network = DeviceContextSignals.currentWifi(requireContext().applicationContext)
        if (network == null) {
            requireContext().showToast(
                if (DeviceContextSignals.hasWifiConnection(requireContext().applicationContext))
                    R.string.wifi_details_unavailable else R.string.wifi_not_available,
                Toast.LENGTH_LONG,
            )
            return
        }
        prefs.addHomeWifi(network.name, network.bssid)
        binding.homeWifi.text = prefs.homeWifiNames.sorted().joinToString(", ")
    }

    private fun selectCarBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            afterBluetoothPermission = ::showCarBluetoothPicker
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        showCarBluetoothPicker()
    }

    private fun selectRuleBluetoothDevices(selected: MutableSet<String>, onChanged: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            afterBluetoothPermission = { showRuleBluetoothPicker(selected, onChanged) }
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        showRuleBluetoothPicker(selected, onChanged)
    }

    private fun showRuleBluetoothPicker(selected: MutableSet<String>, onChanged: () -> Unit) {
        val devices = DeviceContextSignals.pairedBluetoothDevices(requireContext())
        if (devices.isEmpty()) {
            requireContext().showToast(R.string.select_bluetooth_devices)
            return
        }
        requireContext().createDialog(R.string.bluetooth_devices_rule, R.string.close) { _ ->
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                devices.forEach { device ->
                    addView(TextView(context).apply {
                        setTextAppearance(R.style.TextSmall)
                        fun updateSelectedState() {
                            val isSelected = device.address in selected
                            text = "${if (isSelected) "✓ " else ""}${device.name}"
                            setTypeface(typeface, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                        }
                        updateSelectedState()
                        setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                        setOnClickListener {
                            if (!selected.add(device.address)) selected.remove(device.address)
                            updateSelectedState()
                            onChanged()
                        }
                    })
                }
            }
        }.showRespectingStatusBar()
    }

    private fun showCarBluetoothPicker() {
        val devices = DeviceContextSignals.pairedBluetoothDevices(requireContext())
        if (devices.isEmpty()) {
            requireContext().showToast(R.string.select_car_bluetooth)
            return
        }
        showDialog(requireContext().createDialog(R.string.car_bluetooth, R.string.close) { _ ->
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                devices.forEach { device ->
                    addView(TextView(context).apply {
                        setTextAppearance(R.style.TextSmall)
                        text = device.name
                        setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                        setOnClickListener {
                            prefs.carBluetoothAddress = device.address
                            prefs.carBluetoothName = device.name
                            binding.carBluetooth.text = prefs.carBluetoothName
                            dialog?.dismiss()
                        }
                    })
                }
            }
        })
    }

    private fun captureHomeLocation() {
        val context = requireContext().applicationContext
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> manager.getProviders(true).firstOrNull()
        }
        val fallback = runCatching {
            manager.getProviders(true).mapNotNull(manager::getLastKnownLocation).maxByOrNull(Location::getTime)
        }.getOrNull()
        fun save(location: Location?) {
            if (location == null) {
                context.showToast(R.string.location_waiting)
                @Suppress("DEPRECATION")
                runCatching {
                    manager.requestSingleUpdate(
                        provider ?: return@runCatching,
                        object : android.location.LocationListener {
                            override fun onLocationChanged(location: Location) = save(location)
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                            override fun onProviderEnabled(provider: String) = Unit
                            override fun onProviderDisabled(provider: String) = Unit
                        },
                        android.os.Looper.getMainLooper(),
                    )
                }
            }
            else {
                prefs.homeLatitude = location.latitude.toFloat()
                prefs.homeLongitude = location.longitude.toFloat()
                prefs.homeLocationSet = true
                if (_binding != null) binding.homeLocation.setText(R.string.on)
                context.showToast(R.string.location_saved)
            }
        }
        if (provider == null) {
            save(fallback)
            return
        }
        runCatching {
            LocationManagerCompat.getCurrentLocation(
                manager,
                provider,
                null as android.os.CancellationSignal?,
                ContextCompat.getMainExecutor(context),
                Consumer { save(it ?: fallback) },
            )
        }.onFailure { save(fallback) }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) {
            populateMindfulApps()
            populateContextualSuggestions()
        }
    }

    private fun populateKeyboardText() {
        if (prefs.autoShowKeyboard) binding.autoShowKeyboard.text = getString(R.string.on)
        else binding.autoShowKeyboard.text = getString(R.string.off)
    }

    private fun populateWallpaperText() {
        if (prefs.dailyWallpaper) binding.dailyWallpaper.text = getString(R.string.on)
        else binding.dailyWallpaper.text = getString(R.string.off)
    }

    private fun updateHomeBottomAlignment() {
        if (viewModel.isOlauncherDefault.value != true) {
            requireContext().showToast(getString(R.string.please_set_olauncher_as_default_first), Toast.LENGTH_LONG)
            return
        }
        prefs.homeBottomAlignment = !prefs.homeBottomAlignment
        populateAlignment()
        viewModel.updateHomeAlignment(prefs.homeAlignment)
    }

    private fun populateAlignment() {
        when (prefs.homeAlignment) {
            Gravity.START -> binding.alignment.text = getString(R.string.left)
            Gravity.CENTER -> binding.alignment.text = getString(R.string.center)
            Gravity.END -> binding.alignment.text = getString(R.string.right)
        }
    }

    // Home button for recents feature disabled
    // private fun toggleHomeButtonRecents() {
    //     if (!prefs.homeButtonShowRecents && !isAccessServiceEnabled(requireContext())) {
    //         showAccessibilityDialog()
    //         return
    //     }
    //     prefs.homeButtonShowRecents = !prefs.homeButtonShowRecents
    //     populateHomeButtonRecents()
    // }

    // private fun populateHomeButtonRecents() {
    //     binding.homeButtonRecents.text = getString(
    //         if (prefs.homeButtonShowRecents && isAccessServiceEnabled(requireContext())) R.string.on
    //         else R.string.off
    //     )
    // }

    private fun populateLockSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.toggleLock.text = getString(
                if (prefs.lockModeOn && isAccessServiceEnabled(requireContext())) R.string.on
                else R.string.off
            )
        } else {
            binding.toggleLock.text = getString(
                if (prefs.lockModeOn) R.string.on
                else R.string.off
            )
        }
    }

    private fun populateSwipeApps() {
        binding.swipeLeftApp.text = prefs.appNameSwipeLeft
        binding.swipeRightApp.text = prefs.appNameSwipeRight
        if (!prefs.swipeLeftEnabled)
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
        if (!prefs.swipeRightEnabled)
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
    }

//    private fun populateDigitalWellbeing() {
//        binding.digitalWellbeing.isVisible = requireContext().isPackageInstalled(Constants.DIGITAL_WELLBEING_PACKAGE_NAME).not()
//                && requireContext().isPackageInstalled(Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME).not()
//                && prefs.hideDigitalWellbeing.not()
//    }

    private fun showAppListIfEnabled(flag: Int) {
        if ((flag == Constants.FLAG_SET_SWIPE_LEFT_APP) and !prefs.swipeLeftEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        if ((flag == Constants.FLAG_SET_SWIPE_RIGHT_APP) and !prefs.swipeRightEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        viewModel.getAppList(true)
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to flag)
        )
    }

    override fun onDestroyView() {
        // Dismissing the text size dialog applies any pending scale via its dismiss listener
        dialog?.dismiss()
        dialog = null
        applyTextSizeScale()
        applyAppSpacingScale()
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        viewModel.checkForMessages.call()
        super.onDestroy()
    }
}
