package com.example.livelocationtracker.presentation.location

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.livelocationtracker.R
import com.example.livelocationtracker.presentation.components.InfoRow
import com.example.livelocationtracker.presentation.components.PermissionRationale
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocationScreen(viewModel: LocationViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    )

    // Drive the VM's stream from the screen's lifecycle: collection (and the FusedLocationProvider
    // callback under it) starts when the composition appears with permission and stops when the
    // composition leaves.
    LaunchedEffect(permissions.allPermissionsGranted) {
        if (permissions.allPermissionsGranted) {
            viewModel.start(permissionGranted = true)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is LocationViewModel.Event.ShowMessage -> snackbarHost.showSnackbar(e.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_live_location)) })
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        when {
            !permissions.allPermissionsGranted -> PermissionGate(
                permissions = permissions,
                modifier = Modifier.padding(padding),
            )

            ui.locationServicesDisabled -> PermissionRationale(
                title = stringResource(R.string.location_services_disabled_title),
                body = stringResource(R.string.location_services_disabled_body),
                ctaLabel = stringResource(R.string.enable_location),
                onCtaClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.padding(padding),
            )

            else -> LocationContent(
                state = ui,
                onRefresh = { viewModel.refresh(permissionGranted = true) },
                contentPadding = padding,
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionGate(
    permissions: MultiplePermissionsState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val showSettings = !permissions.shouldShowRationale && permissions.permissions.any { !it.status.isGranted }
    PermissionRationale(
        title = stringResource(R.string.permission_rationale_title),
        body = if (showSettings) stringResource(R.string.permission_denied_permanently)
        else stringResource(R.string.permission_rationale_body),
        ctaLabel = if (showSettings) stringResource(R.string.open_settings)
        else stringResource(R.string.permission_grant),
        onCtaClick = {
            if (showSettings) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                permissions.launchMultiplePermissionRequest()
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationContent(
    state: LocationUiState,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MapPinImage()
            Spacer(Modifier.height(16.dp))

            when (val phase = state.phase) {
                LocationUiState.Phase.Loading -> CenterLoading()
                is LocationUiState.Phase.Error -> ErrorBlock(phase.message)
                LocationUiState.Phase.Success -> {
                    val loc = state.location ?: return@Column
                    LocationCard(state = state, onRefresh = onRefresh)
                    Spacer(Modifier.height(16.dp))
                    AddressCard(state = state)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Streaming every 5s • ±%.0fm".format(loc.accuracyMeters),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPinImage() {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(R.drawable.ic_map_pin)
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.content_desc_pin),
        modifier = Modifier.size(120.dp),
    )
}

@Composable
private fun CenterLoading() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.loading_location))
        }
    }
}

@Composable
private fun ErrorBlock(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.error_generic),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationCard(state: LocationUiState, onRefresh: () -> Unit) {
    val loc = state.location ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(stringResource(R.string.label_latitude), "%.6f".format(loc.latitude))
            InfoRow(stringResource(R.string.label_longitude), "%.6f".format(loc.longitude))
            InfoRow(stringResource(R.string.label_accuracy), "±%.1f m".format(loc.accuracyMeters))
            InfoRow(
                stringResource(R.string.label_timestamp),
                DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(loc.timestampMillis)),
            )
        }
    }
}

@Composable
private fun AddressCard(state: LocationUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.label_address),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            when {
                state.addressLoading && state.address == null ->
                    Text(stringResource(R.string.address_loading))

                state.address != null -> {
                    val a = state.address
                    val primary = a.displayName.takeIf { it.isNotBlank() }
                        ?: listOfNotNull(a.road, a.city, a.country)
                            .joinToString(", ")
                            .ifBlank { stringResource(R.string.address_unavailable) }
                    Text(primary, style = MaterialTheme.typography.bodyLarge)
                }

                else -> Text(
                    text = stringResource(R.string.address_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}