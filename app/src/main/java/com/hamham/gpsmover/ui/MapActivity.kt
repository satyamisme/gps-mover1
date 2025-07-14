package com.hamham.gpsmover.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hamham.gpsmover.BuildConfig
import com.hamham.gpsmover.R
import com.hamham.gpsmover.adapter.FavListAdapter
import com.hamham.gpsmover.databinding.ActivityMapBinding
import com.hamham.gpsmover.utils.FavoritesImportExport
import com.hamham.gpsmover.utils.NotificationsChannel
import com.hamham.gpsmover.utils.ext.*
import com.hamham.gpsmover.viewmodel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.properties.Delegates

@AndroidEntryPoint
class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private val binding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private lateinit var mMap: GoogleMap
    val viewModel by viewModels<MainViewModel>()
    private val notificationsChannel by lazy { NotificationsChannel() }
    private var favListAdapter: FavListAdapter = FavListAdapter()
    private var mMarker: Marker? = null
    private var mLatLng: LatLng? = null
    private var lat by Delegates.notNull<Double>()
    private var lon by Delegates.notNull<Double>()
    private var xposedDialog: MaterialAlertDialogBuilder? = null
    private lateinit var alertDialog: MaterialAlertDialogBuilder
    private lateinit var dialog: androidx.appcompat.app.AlertDialog
    private val favoritesImportExport by lazy { FavoritesImportExport(this) }
    private val IMPORT_REQUEST_CODE = 1001
    private val EXPORT_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        initializeMap()
        setFloatActionButton()
        isModuleEnable()
        setupSearchBar()

        // Add click listener for add favorite FAB
        val mapContainer = findViewById<View>(R.id.map_container)
        mapContainer.findViewById<View>(R.id.add_fav_fab).setOnClickListener {
            it.performHapticClick()
            addFavouriteDialog()
        }

        // Add click listener for my location FAB
        mapContainer.findViewById<View>(R.id.my_location_fab).setOnClickListener {
            it.performHapticClick()
            moveToMyRealLocation()
        }

        // Setup bottom navigation
        setupBottomNavigation()
        
        // Ensure FABs are visible on startup
        mapContainer.findViewById<View>(R.id.start).visibility = View.VISIBLE
        mapContainer.findViewById<View>(R.id.stop).visibility = if (viewModel.isStarted) View.VISIBLE else View.GONE
        mapContainer.findViewById<View>(R.id.add_fav_fab).visibility = View.VISIBLE
        mapContainer.findViewById<View>(R.id.my_location_fab).visibility = View.VISIBLE
        
        // Initialize favorites page setup immediately
        setupFavoritesPage()
        
        // Ensure the correct page is shown based on currentPage
        when (currentPage) {
            "map" -> showMapPage()
            "favorites" -> showFavoritesPage()
            "settings" -> showSettingsPage()
            else -> showMapPage()
        }
    }

    override fun onPause() {
        super.onPause()
        dismissAllDialogs()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAllDialogs()
    }

    private fun dismissAllDialogs() {
        try {
            // Dismiss xposed dialog
            xposedDialog?.create()?.dismiss()
            xposedDialog = null
            
            // Dismiss favorite dialog
            if (::dialog.isInitialized && dialog.isShowing) {
                dialog.dismiss()
            }
            
            // Dismiss any other dialogs
            if (::alertDialog.isInitialized) {
                try {
                    alertDialog.create().dismiss()
                } catch (e: Exception) {
                    // Ignore errors
                }
            }
        } catch (e: Exception) {
            // Ignore any errors when trying to dismiss dialogs
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            // Haptic feedback
            bottomNavigation.performHapticClick()
            when (menuItem.itemId) {
                R.id.navigation_map -> {
                    showMapPage()
                    true
                }
                R.id.navigation_favorites -> {
                    showFavoritesPage()
                    true
                }
                R.id.navigation_settings -> {
                    showSettingsPage()
                    true
                }
                else -> false
            }
        }
        
        // Set the correct tab selection based on current page
        when (currentPage) {
            "map" -> bottomNavigation.selectedItemId = R.id.navigation_map
            "favorites" -> bottomNavigation.selectedItemId = R.id.navigation_favorites
            "settings" -> bottomNavigation.selectedItemId = R.id.navigation_settings
            else -> bottomNavigation.selectedItemId = R.id.navigation_map
        }
    }

    private fun navigateToMapPage() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if (bottomNavigation.selectedItemId != R.id.navigation_map) {
            bottomNavigation.selectedItemId = R.id.navigation_map
        }
        showMapPage()
    }


    


    private fun showMapPage() {
        currentPage = "map"
        val mapContainer = findViewById<View>(R.id.map_container)
        val favoritesPage = findViewById<View>(R.id.favorites_page)
        val settingsPage = findViewById<View>(R.id.settings_page)
        
        // Fade out other pages
        favoritesPage.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                favoritesPage.visibility = View.GONE
            }
            .start()
            
        settingsPage.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                settingsPage.visibility = View.GONE
            }
            .start()
        
        // Fade in map page
        mapContainer.visibility = View.VISIBLE
        mapContainer.alpha = 0f
        mapContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
        
        // Ensure FABs are visible on map page
        mapContainer.findViewById<View>(R.id.start).visibility = View.VISIBLE
        mapContainer.findViewById<View>(R.id.stop).visibility = if (viewModel.isStarted) View.VISIBLE else View.GONE
        mapContainer.findViewById<View>(R.id.add_fav_fab).visibility = View.VISIBLE
        mapContainer.findViewById<View>(R.id.my_location_fab).visibility = View.VISIBLE
        
        setFloatActionButton()
    }

    private fun showFavoritesPage() {
        currentPage = "favorites"
        val mapContainer = findViewById<View>(R.id.map_container)
        val favoritesPage = findViewById<View>(R.id.favorites_page)
        val settingsPage = findViewById<View>(R.id.settings_page)
        
        // Fade out other pages
        mapContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                mapContainer.visibility = View.GONE
            }
            .start()
            
        settingsPage.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                settingsPage.visibility = View.GONE
            }
            .start()
        
        // Fade in favorites page
        favoritesPage.visibility = View.VISIBLE
        favoritesPage.alpha = 0f
        favoritesPage.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
        
        // Hide FABs on favorites page
        mapContainer.findViewById<View>(R.id.start).visibility = View.GONE
        mapContainer.findViewById<View>(R.id.stop).visibility = View.GONE
        mapContainer.findViewById<View>(R.id.add_fav_fab).visibility = View.GONE
        mapContainer.findViewById<View>(R.id.my_location_fab).visibility = View.GONE
        
        // No need to call setupFavoritesPage() again as it's already initialized in onCreate
    }

    private fun showSettingsPage() {
        currentPage = "settings"
        val mapContainer = findViewById<View>(R.id.map_container)
        val favoritesPage = findViewById<View>(R.id.favorites_page)
        val settingsPage = findViewById<View>(R.id.settings_page)
        
        // Fade out other pages
        mapContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                mapContainer.visibility = View.GONE
            }
            .start()
            
        favoritesPage.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                favoritesPage.visibility = View.GONE
            }
            .start()
        
        // Fade in settings page
        settingsPage.visibility = View.VISIBLE
        settingsPage.alpha = 0f
        settingsPage.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
        
        // Hide FABs on settings page
        mapContainer.findViewById<View>(R.id.start).visibility = View.GONE
        mapContainer.findViewById<View>(R.id.stop).visibility = View.GONE
        mapContainer.findViewById<View>(R.id.add_fav_fab).visibility = View.GONE
        mapContainer.findViewById<View>(R.id.my_location_fab).visibility = View.GONE
        
        setupSettingsPage()
    }

    private var isFavoritesPageInitialized = false
    private var currentPage = "map" // Track current page: "map", "favorites", "settings"

    private fun setupFavoritesPage() {
        if (isFavoritesPageInitialized) {
            Log.d("MapActivity", "Favorites page already initialized, skipping setup")
            return
        }
        
        Log.d("MapActivity", "Initializing favorites page setup")
        
        val favoritesPage = findViewById<View>(R.id.favorites_page)
        val recyclerView = favoritesPage.findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        
        recyclerView.adapter = favListAdapter

        // Setup drag and drop for MapActivity
        val itemTouchHelper = FavListAdapter.createItemTouchHelper(favListAdapter)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        favListAdapter.setItemTouchHelper(itemTouchHelper)
        
        Log.d("MapActivity", "ItemTouchHelper attached to RecyclerView")

        favListAdapter.onItemClick = { favourite ->
            
            // Add a very short delay to show the clicked item, then process and navigate
            lifecycleScope.launch {
                delay(100) // 100ms delay to show the clicked item
                
                // Process the favorite selection
                favourite.let {
                    lat = it.lat!!
                    lon = it.lng!!
                    val selectedLatLng = LatLng(lat, lon)
                    mLatLng = selectedLatLng
                    // Activate fake location immediately
                    viewModel.update(true, lat, lon)
                    // Update marker
                    mMarker?.position = selectedLatLng
                    mMarker?.isVisible = true
                    // Move camera to location
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 12.0f))
                    // Update FAB buttons state
                    val mapContainer = findViewById<View>(R.id.map_container)
                    mapContainer.findViewById<View>(R.id.start).visibility = View.GONE
                    mapContainer.findViewById<View>(R.id.stop).visibility = View.VISIBLE
                }
                
                // Switch back to map page
                navigateToMapPage()
                
                // Show notification and update FAB visibility after navigation
                delay(50) // Small delay to ensure navigation is complete
                val mapContainer = findViewById<View>(R.id.map_container)
                mapContainer.findViewById<View>(R.id.start).visibility = View.GONE
                mapContainer.findViewById<View>(R.id.stop).visibility = View.VISIBLE
                
                // Show notification
                mLatLng?.getAddress(this@MapActivity)?.let { address ->
                    address.collect { value ->
                        showStartNotification(value)
                    }
                }
            }
        }

        favListAdapter.onItemDelete = { favourite ->
            viewModel.deleteFavourite(favourite)
        }

        favListAdapter.onItemMove = { fromPosition, toPosition ->
            // Save the new order to database
            val updatedFavorites = favListAdapter.getItems().mapIndexed { index, favourite ->
                favourite.copy(order = index)
            }
            viewModel.updateFavouritesOrder(updatedFavorites)
        }

        // Setup back arrow functionality
        val backArrow = favoritesPage.findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            it.performHapticClick()
            // Switch back to map page
            navigateToMapPage()
        }

        // Setup menu button functionality
        val menuButton = favoritesPage.findViewById<ImageView>(R.id.menu_button)
        menuButton.setOnClickListener { view ->
            view.performHapticClick()
            showFavoritesMenu(view)
        }

        // Load favorites
        getAllUpdatedFavList()
        
        isFavoritesPageInitialized = true
        Log.d("MapActivity", "Favorites page setup completed")
    }

    private fun setupSettingsPage() {
        val settingsPage = findViewById<SettingsPage>(R.id.settings_page)
        settingsPage.setViewModel(viewModel)
        settingsPage.setOnSettingsChangedListener {
            // Update map when settings change
            if (::mMap.isInitialized) {
                mMap.mapType = viewModel.mapType
            }
        }
        
        // Setup back arrow functionality for settings page
        settingsPage.setOnBackClick {
            // Switch back to map page
            navigateToMapPage()
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun isModuleEnable() {
        viewModel.isXposed.observe(this) { isXposed ->
            xposedDialog?.create()?.dismiss()
            xposedDialog = null
            if (!isXposed) {
                xposedDialog = MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.error_xposed_module_missing)
                    .setMessage(R.string.error_xposed_module_missing_desc)
                    .setCancelable(BuildConfig.DEBUG)
                xposedDialog?.create()?.show()
            }
        }
    }

    private fun setFloatActionButton() {
        val mapContainer = findViewById<View>(R.id.map_container)
        val startButton = mapContainer.findViewById<View>(R.id.start)
        val stopButton = mapContainer.findViewById<View>(R.id.stop)

        // Explicitly reset the state
        if (viewModel.isStarted) {
            startButton.visibility = View.GONE
            stopButton.visibility = View.VISIBLE
            Log.d("FAB", "onCreate: show stop, hide start")
        } else {
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
            Log.d("FAB", "onCreate: show start, hide stop")
        }

        startButton.setOnClickListener {
            it.performHapticClick()
            viewModel.update(true, lat, lon)
            mLatLng.let {
                mMarker?.position = it!!
            }
            mMarker?.isVisible = true
            startButton.visibility = View.GONE
            stopButton.visibility = View.VISIBLE
            Log.d("FAB", "Clicked start: hide start, show stop")
            lifecycleScope.launch {
                mLatLng?.getAddress(this@MapActivity)?.let { address ->
                    address.collect { value ->
                        showStartNotification(value)
                    }
                }
            }
        }
        stopButton.setOnClickListener {
            it.performHapticClick()
            mLatLng.let {
                viewModel.update(false, it!!.latitude, it.longitude)
            }
            mMarker?.isVisible = false
            stopButton.visibility = View.GONE
            startButton.visibility = View.VISIBLE
            Log.d("FAB", "Clicked stop: hide stop, show start")
            cancelNotification()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        with(mMap) {
            mapType = viewModel.mapType
            val zoom = 12.0f
            lat = viewModel.getLat
            lon = viewModel.getLng
            mLatLng = LatLng(lat, lon)
            mLatLng.let {
                mMarker = addMarker(
                    MarkerOptions().position(it!!).draggable(false)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).visible(false)
                )
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, zoom))
            }
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
            if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                mMap.isMyLocationEnabled = true
            } else {
                val permList = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                ActivityCompat.requestPermissions(
                    this@MapActivity,
                    permList,
                    99
                )
            }
            setPadding(0, 0, 0, 170)
            setOnMapClickListener(this@MapActivity)
            if (viewModel.isStarted) {
                mMarker?.let {
                    it.isVisible = true
                    it.showInfoWindow()
                }
            }
        }
    }

    override fun onMapClick(latLng: LatLng) {
        mLatLng = latLng
        mMarker?.let { marker ->
            mLatLng.let {
                marker.position = it!!
                marker.isVisible = true
                mMap.animateCamera(CameraUpdateFactory.newLatLng(it))
                lat = it.latitude
                lon = it.longitude
            }
        }
    }

    private fun moveMapToNewLocation() {
        mLatLng = LatLng(lat, lon)
        mLatLng?.let { latLng ->
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0f))
            mMarker?.apply {
                position = latLng
                isVisible = true
                showInfoWindow()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateXposedState()
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_fav -> addFavouriteDialog()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showFavoritesMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.favorites_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    exportFavorites()
                    true
                }
                R.id.action_import -> {
                    importFavorites()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun showFavoritesMessageBar(message: String, type: SnackbarType = SnackbarType.INFO, duration: Long = 2500L) {
        val favoritesPage = findViewById<View>(R.id.favorites_page)
        val messageBar = favoritesPage.findViewById<TextView>(R.id.favorites_message_bar)
        messageBar.text = message
        messageBar.visibility = View.VISIBLE
        messageBar.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        // Get themed colors
        fun getThemeColor(attr: Int): Int {
            val typedValue = android.util.TypedValue()
            val theme = this.theme
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }
        val (bgColor, textColor) = when (type) {
            SnackbarType.SUCCESS -> getThemeColor(com.google.android.material.R.attr.colorPrimary) to getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
            SnackbarType.ERROR -> getThemeColor(com.google.android.material.R.attr.colorError) to getThemeColor(com.google.android.material.R.attr.colorOnError)
            SnackbarType.INFO -> getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant) to getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        }
        val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimensionPixelSize(R.dimen.message_bar_radius).toFloat()
            setColor(bgColor)
        }
        messageBar.background = bgDrawable
        messageBar.setTextColor(textColor)
        messageBar.removeCallbacks(null)
        messageBar.postDelayed({
            messageBar.visibility = View.GONE
        }, duration)
    }
    


    private fun exportFavorites() {
        lifecycleScope.launch {
            val favorites = viewModel.allFavList.first()
            if (favorites.isEmpty()) {
                showFavoritesMessageBar("No favorites to export", SnackbarType.INFO)
                return@launch
            }
            // Prepare JSON string for export
            val exportJsonString = com.google.gson.Gson().toJson(favorites)
            
            // Create intent to choose file save location
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "gps_mover_favorites.json")
            }
            startActivityForResult(intent, EXPORT_REQUEST_CODE)
        }
    }

    private fun importFavorites() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, IMPORT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    val importedFavorites = favoritesImportExport.importFavorites(uri)
                    if (importedFavorites != null) {
                        viewModel.replaceAllFavourites(importedFavorites)
                        showFavoritesMessageBar("Imported ${importedFavorites.size} favorites", SnackbarType.SUCCESS)
                        // Navigate to favorites page to show the imported items
                        showFavoritesPage()
                    } else {
                        showFavoritesMessageBar("Failed to import favorites", SnackbarType.ERROR)
                    }
                }
            }
        } else if (requestCode == EXPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    val favorites = viewModel.allFavList.first()
                    val json = com.google.gson.Gson().toJson(favorites)
                    try {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray())
                        }
                        showFavoritesMessageBar("Favorites exported successfully", SnackbarType.SUCCESS)
                    } catch (e: Exception) {
                        showFavoritesMessageBar("Failed to export favorites", SnackbarType.ERROR)
                    }
                }
            }
        }
    }



    private fun addFavouriteDialog() {
        // Dismiss any existing dialogs first
        try {
            if (::dialog.isInitialized && dialog.isShowing) {
                dialog.dismiss()
            }
        } catch (e: Exception) {
            // Ignore any errors when trying to dismiss dialogs
        }
        
        val view = layoutInflater.inflate(R.layout.dialog_layout, null)
        val editText = view.findViewById<TextInputEditText>(R.id.search_edittxt)
        val actionButton = view.findViewById<MaterialButton>(R.id.dialog_action_button)
        val textInputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.text_input_layout)
        
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT
        actionButton.text = getString(R.string.dialog_button_add)
        
        // Set Material 3 colors for favorite dialog
        actionButton.setOnClickListener {
            val s = editText.text.toString()
            if (!mMarker?.isVisible!!) {
                showCustomSnackbar("Location not select", SnackbarType.ERROR)
            } else {
                viewModel.storeFavorite(s, lat, lon)
                viewModel.response.observe(this@MapActivity) {
                    if (it == (-1).toLong()) showCustomSnackbar("Can't save", SnackbarType.ERROR) // else showToast("Save") 
                }
                try {
                    if (::dialog.isInitialized && dialog.isShowing) {
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    // Ignore any errors when trying to dismiss dialogs
                }
            }
        }
        
        alertDialog = MaterialAlertDialogBuilder(this)
            .setView(view)
        dialog = alertDialog.create()
        
        // Ensure dialog is shown safely
        try {
            if (!isFinishing && !isDestroyed) {
                dialog.show()
            }
        } catch (e: Exception) {
            // If showing dialog fails, show a simple snackbar instead
            showCustomSnackbar("Failed to show dialog", SnackbarType.ERROR)
        }
    }



    private fun getAllUpdatedFavList() {
        val favoritesPage = findViewById<View>(R.id.favorites_page)
        val recyclerView = favoritesPage.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyCard = favoritesPage.findViewById<View>(R.id.emptyCard)
        val emptyTitle = favoritesPage.findViewById<TextView>(R.id.emptyTitle)
        val emptyDescription = favoritesPage.findViewById<TextView>(R.id.emptyDescription)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.doGetUserDetails()
                viewModel.allFavList.collect { favorites ->
                    favListAdapter.setItems(favorites)
                    if (favorites.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyCard.visibility = View.VISIBLE
                        emptyTitle.text = getString(R.string.empty_favorites_title)
                        emptyDescription.text = getString(R.string.empty_favorites_description)
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyCard.visibility = View.GONE
                    }
                }
            }
        }
    }







    private suspend fun getSearchAddress(address: String) = callbackFlow {
        withContext(Dispatchers.IO) {
            trySend(SearchProgress.Progress)
            val matcher: Matcher =
                Pattern.compile("[-+]?\\d{1,3}([.]\\d+)?, *[-+]?\\d{1,3}([.]\\d+)?").matcher(address)

            if (matcher.matches()) {
                delay(3000)
                trySend(SearchProgress.Complete(matcher.group().split(",")[0].toDouble(), matcher.group().split(",")[1].toDouble()))
            } else {
                try {
                    val list: List<Address>? = Geocoder(this@MapActivity).getFromLocationName(address, 5)
                    list?.let {
                        if (it.size == 1) {
                            trySend(SearchProgress.Complete(list[0].latitude, list[1].longitude))
                        } else {
                            trySend(SearchProgress.Fail(getString(R.string.address_not_found)))
                        }
                    }
                } catch (io: IOException) {
                    trySend(SearchProgress.Fail(getString(R.string.no_internet)))
                }
            }
        }
        awaitClose { this.cancel() }
    }

    private fun showStartNotification(address: String) {
        notificationsChannel.showNotification(this) {
            it.setSmallIcon(R.drawable.ic_baseline_stop_24)
            it.setContentTitle(getString(R.string.location_set))
            it.setContentText(address)
            it.setAutoCancel(true)
            it.setCategory(Notification.CATEGORY_EVENT)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    private fun cancelNotification() {
        notificationsChannel.cancelAllNotifications(this)
    }

    private fun setupSearchBar() {
        val mapContainer = findViewById<View>(R.id.map_container)
        val searchEditText = mapContainer.findViewById<EditText>(R.id.search_edit_text)
        val searchSendButton = mapContainer.findViewById<ImageButton>(R.id.search_send_button)

        // Set up send button click listener
        searchSendButton.setOnClickListener {
            performSearch(searchEditText.text.toString())
        }

        // Set up search on keyboard action
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                performSearch(searchEditText.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(searchQuery: String) {
        if (searchQuery.isNotEmpty()) {
            try {
                // Try to parse as coordinates first
                val parts = searchQuery.split(",").map { it.trim() }
                if (parts.size == 2) {
                    val lat = parts[0].toDouble()
                    val lng = parts[1].toDouble()
                    
                    // Validate coordinates
                    if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                        this.lat = lat
                        this.lon = lng
                        moveMapToNewLocation()
                        // Clear search text after successful search
                        val mapContainer = findViewById<View>(R.id.map_container)
                        mapContainer.findViewById<EditText>(R.id.search_edit_text).text?.clear()
                        return
                    } else {
                        // Show error as toast
                        showCustomSnackbar("Invalid coordinates range", SnackbarType.ERROR)
                        return
                    }
                }
                
                // If not coordinates, try address search
                if (isNetworkConnected()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        getSearchAddress(searchQuery).let {
                            it.collect { result ->
                                when (result) {
                                    is SearchProgress.Progress -> {}
                                    is SearchProgress.Complete -> {
                                        lat = result.lat
                                        lon = result.lon
                                        moveMapToNewLocation()
                                        val mapContainer = findViewById<View>(R.id.map_container)
                                        mapContainer.findViewById<EditText>(R.id.search_edit_text).text?.clear()
                                    }
                                    is SearchProgress.Fail -> {
                                        showCustomSnackbar(result.error ?: "Search failed", SnackbarType.ERROR)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    showCustomSnackbar(getString(R.string.no_internet), SnackbarType.ERROR)
                }
            } catch (e: NumberFormatException) {
                // If coordinates parsing fails, try address search
                if (isNetworkConnected()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        getSearchAddress(searchQuery).let {
                            it.collect { result ->
                                when (result) {
                                    is SearchProgress.Progress -> {}
                                    is SearchProgress.Complete -> {
                                        lat = result.lat
                                        lon = result.lon
                                        moveMapToNewLocation()
                                        val mapContainer = findViewById<View>(R.id.map_container)
                                        mapContainer.findViewById<EditText>(R.id.search_edit_text).text?.clear()
                                    }
                                    is SearchProgress.Fail -> {
                                        showCustomSnackbar(result.error ?: "Search failed", SnackbarType.ERROR)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    showCustomSnackbar(getString(R.string.no_internet), SnackbarType.ERROR)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyRealLocation() {
        if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (::mMap.isInitialized) {
                // Enable my location if not already enabled
                if (!mMap.isMyLocationEnabled) {
                    mMap.isMyLocationEnabled = true
                }
                
                // Get the last known location
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val realLocation = LatLng(it.latitude, it.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(realLocation, 15.0f))
                    } ?: run {
                        showCustomSnackbar("Cannot get your current location", SnackbarType.ERROR)
                    }
                }.addOnFailureListener {
                    showCustomSnackbar("Failed to get your current location", SnackbarType.ERROR)
                }
            }
        } else {
            // Request location permission
            val permList = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(this, permList, 99)
            showCustomSnackbar("Location permission required", SnackbarType.INFO)
        }
    }
}

sealed class SearchProgress {
    object Progress : SearchProgress()
    data class Complete(val lat: Double, val lon: Double) : SearchProgress()
    data class Fail(val error: String?) : SearchProgress()
}