package com.example.jetpackmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.jetpackmap.api.RetrofitClient
import com.example.jetpackmap.model.ChipItem
import com.example.jetpackmap.model.NearbySpot
import com.example.jetpackmap.ui.theme.JetpackMapTheme
import com.example.jetpackmap.utils.Constants
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    //--------
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    var latLng by remember { mutableStateOf<LatLng?>(null) }

                    RequestLocationPermission(onPermissionGranted = {
                        getLastUserLocation(onGetLastLocationSuccess = {
                            latLng = LatLng(it.first, it.second)
                        }, onGetLastLocationFailed = {}, onGetLastLocationIsNull = {
                            getCurrentLocation(
                                onGetCurrentLocationSuccess = {},
                                onGetCurrentLocationFailed = {})
                        })
                    }, onPermissionDenied = {}, onPermissionsRevoked = {})

                    latLng?.let {
                        MapScreen(it)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun RequestLocationPermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit,
        onPermissionsRevoked: () -> Unit
    ) {
        val permissionState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )

        LaunchedEffect(key1 = permissionState) {
            val allPermissionsRevoked =
                permissionState.permissions.size == permissionState.revokedPermissions.size
            val permissionsToRequest = permissionState.permissions.filter { !it.status.isGranted }

            if (permissionsToRequest.isNotEmpty()) permissionState.launchMultiplePermissionRequest()

            if (allPermissionsRevoked) {
                onPermissionsRevoked()
            } else {
                if (permissionState.allPermissionsGranted) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(
        onGetCurrentLocationSuccess: (Pair<Double, Double>) -> Unit,
        onGetCurrentLocationFailed: (Exception) -> Unit,
        priority: Boolean = true
    ) {
        val accuracy =
            if (priority) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        if (areLocationPermissionsGranted()) {
            fusedLocationProviderClient.getCurrentLocation(
                accuracy, CancellationTokenSource().token,
            ).addOnSuccessListener { location ->
                location?.let {
                    onGetCurrentLocationSuccess(Pair(it.latitude, it.longitude))
                }?.run {}
            }.addOnFailureListener { exception ->
                onGetCurrentLocationFailed(exception)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastUserLocation(
        onGetLastLocationSuccess: (Pair<Double, Double>) -> Unit,
        onGetLastLocationFailed: (Exception) -> Unit,
        onGetLastLocationIsNull: () -> Unit
    ) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (areLocationPermissionsGranted()) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onGetLastLocationSuccess(Pair(it.latitude, it.longitude))
                }?.run {
                    onGetLastLocationIsNull()
                }
            }.addOnFailureListener { exception ->
                onGetLastLocationFailed(exception)
            }
        }
    }

    private fun areLocationPermissionsGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }
}


@Composable
fun CustomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeHolder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier) {
        BasicTextField(value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            singleLine = true,
            modifier = Modifier
                .height(56.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onActiveChange(it.isFocused) }
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(30.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(30.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    leadingIcon?.invoke()
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) placeHolder?.invoke()
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            })
        LaunchedEffect(active) {
            if (!active) focusManager.clearFocus()
        }
    }
}

@Composable
fun MapScreen(latLng: LatLng) {
    var query by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var nearbySpots by remember { mutableStateOf(listOf<NearbySpot>()) }
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("") }
    var markers by remember { mutableStateOf<List<MarkerOptions>>(emptyList()) }
    var isSatelliteView by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val cameraPositionState =
        rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(latLng, 10f) }
    val chipItems = listOf(
        ChipItem("Restaurants", "restaurant"),
        ChipItem("Gym", "gym"),
        ChipItem("Hospitals", "hospital"),
        ChipItem("ATMs", "atm")
    )

    fun animateToLocation(location: LatLng) {
        scope.launch {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(location, 15f),
                durationMs = 500 // Duration of the animation
            )
        }
    }

    fun toggleMapView() {
        isSatelliteView = !isSatelliteView
    }

    fun onSearch(searchQuery: String) {
        val location = "${latLng.latitude},${latLng.longitude}"
        scope.launch {
            markers = RetrofitClient.instance.getAutocomplete(
                location, searchQuery, Constants.OLA_API_KEY
            ).predictions.map {
                MarkerOptions().position(
                    LatLng(
                        it.geometry.location.lat, it.geometry.location.lng
                    )
                ).title(it.structured_formatting.main_text)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
            properties = MapProperties(mapType = if (isSatelliteView) MapType.SATELLITE else MapType.NORMAL)
        ) {

            MarkerInfoWindow(
                state = MarkerState(position = latLng),
                title = "Current Location",
                snippet = "This is where you are!",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )

            markers.forEach { markerOptions ->
                Marker(
                    state = MarkerState(position = markerOptions.position),
                    title = markerOptions.title
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            CustomSearchBar(query = query,
                onQueryChange = { query = it },
                onSearch = ::onSearch,
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                modifier = Modifier.fillMaxWidth(),
                placeHolder = { Text(stringResource(R.string.search_here), color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray
                    )
                }
            ) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Icon",
                            tint = Color.Gray
                        )
                    }
                }
            }

            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(chipItems) { chip ->
                    ChipItemView(chip, chip.type == selectedType) {
                        selectedType = chip.type
                        val location = "${latLng.latitude},${latLng.longitude}"
                        scope.launch {
                            val nearbySpotResponse = RetrofitClient.instance.fetchNearbySpots(
                                "venue",
                                chip.type,
                                location,
                                Constants.OLA_API_KEY
                            )

                            // Map response to NearbySpot objects
                            nearbySpots = nearbySpotResponse.predictions.map { prediction2 ->
                                NearbySpot(
                                    placeId = prediction2.place_id,
                                    mainText = prediction2.structured_formatting.main_text,
                                    secondaryText = prediction2.structured_formatting.secondary_text,
                                    distanceMeters = prediction2.distance_meters
                                )
                            }
                            isBottomSheetVisible = true
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Column {
                    IconButton(onClick = { toggleMapView() }) {
                        Icon(
                            imageVector = if (isSatelliteView) Icons.Outlined.CheckCircle else Icons.Filled.CheckCircle,
                            contentDescription = if (isSatelliteView) "Normal View" else "Satellite View"
                        )
                    }
                }
            }
        }

        //----
        FloatingActionButtonView { animateToLocation(latLng) }

        if (isBottomSheetVisible) {
            NearbySpotsBottomSheet(nearbySpots) { isBottomSheetVisible = false }
        }
    }
}

@Composable
fun FloatingActionButtonView(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = "Current Location")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipItemView(chip: ChipItem, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(chip.label) },
        modifier = Modifier.padding(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbySpotsBottomSheet(spots: List<NearbySpot>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Nearby Spots", style = MaterialTheme.typography.headlineSmall)
            spots.forEach { spot ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(spot.mainText, fontWeight = FontWeight.Bold)
                    Text(spot.secondaryText)
                    Text("${spot.distanceMeters} meters away")
                }
            }
        }
    }
}

