package com.codelv.inventory

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.trimmedLength
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.codelv.inventory.ui.theme.AppTheme
import com.codelv.inventory.ui.theme.Colors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.journeyapps.barcodescanner.ScanContract
import kotlinx.coroutines.launch


val TAG = "MainActivity";

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var state = AppViewModel(database = (application as App).db)
        state.viewModelScope.launch {
            state.load();
            state.loadSettings(context = applicationContext);
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main(state)
                }
            }
        }
    }
}

@Composable
fun Main(state: AppViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "parts") {
        composable("parts") {
            Log.i(TAG, "Navigate to parts list")
            PartsScreen(nav, state)
        }
        composable("scans") {
            Log.i(TAG, "Navigate to scans list")
            ScansScreen(nav, state)
        }
        composable("settings") {
            Log.i(TAG, "Navigate to settings")
            SettingsScreen(nav, state)
        }
        composable(
            "edit-part?id={id}&import={import}",
            arguments = listOf(
                navArgument("id") { defaultValue = 0 },
                navArgument("import") { defaultValue = 0 }
            )
        ) {
            val partId = it.arguments?.getInt("id");
            val autoImport = it.arguments?.getInt("import");
            Log.i(TAG, "Navigate to edit-part?id=${partId}&import=${autoImport}")
            val savedPart = state.parts.find { it.id == partId };
            var part = if (savedPart != null) savedPart else Part(id = 0)
            PartEditorScreen(nav, state, part,  autoImport == 1)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScansScreen(nav: NavHostController, state: AppViewModel) {
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    var selectedScan by remember { mutableStateOf(Scan(id = 0)) }
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(snackbarState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(20.dp),
                title = { Text("Scanned Barcodes") },
                navigationIcon =
                    {
                        IconButton(onClick = { nav.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
            ) {
                ScanList(state.scans, { scan ->
                    scope.launch {
                        selectedScan = scan;
                        scaffoldState.bottomSheetState.expand()
                    }
                })
            }
        },
        sheetPeekHeight = 0.dp,
        sheetShadowElevation = 20.dp,
        sheetContent = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Text("Barcode content", fontWeight = FontWeight.Bold)
                SelectionContainer {
                    Text(selectedScan.value, modifier = Modifier.padding(0.dp, 8.dp))
                }
                var part = selectedScan.part;
                if (part != null) {
                    Button(
                        onClick = {
                            scope.launch {
                                val savedPart = state.parts.find { it.mpn == part.mpn };
                                if (savedPart == null) {
                                    state.addPart(part)
                                    nav.navigate("edit-part?id=${part.id}&import=1")
                                } else {
                                    nav.navigate("edit-part?id=${savedPart.id}")
                                }
                            }
                        }
                    ) {
                        Text("Open part")
                    }
                } else {
                    Text("Unknown barcode format")
                    if (selectedScan.value.trimmedLength() > 0) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val part = Part(id = 0, mpn = selectedScan.value)
                                    state.addPart(part)
                                    nav.navigate("edit-part?id=${part.id}")
                                }
                            }
                        ) {
                            Text("Import as MPN of new part")
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun ScanList(scans: List<Scan>, onScanClicked: (scan: Scan) -> Unit) {
    LazyColumn(
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(scans) { scan ->
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable {
                        onScanClicked(scan)
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {
                var valid = scan.isValid();

                Icon(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .align(Alignment.CenterVertically),
                    imageVector = if (valid) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = "Scan valid",
                    tint = if (valid) Color.Green else Color.Red
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        scan.created.toString(),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp
                    )
                    Text(scan.value, maxLines = 1)
                }

            }
        }
    }

}

// Search parts in memory. Can use " and " to separate multiple filters
// and
fun search(parts: List<Part>, query: String, ignoreCase: Boolean = true): List<Part> {
    if (query.trimmedLength() == 0) {
        return parts // No query
    }
    val splitQuery = query.lowercase().replace(" and ", ",").split(",")
    return parts.filter { part ->
        val desc = part.description.replace(
            "µ", "u"
        ).replace("Ω", "Ohm").replace("ꭥ", "Ohm")
        splitQuery.all {
            var q = it.trim()
            if (q.length == 0)
                true// Ignore empty strings
            else if (":" in q) {
                val (k, v) = q.split(":", limit = 2)
                if (k.trimmedLength() == 0 || v.trimmedLength() == 0) {
                    true // Ignore empty key or value
                } else when (k.trim().lowercase()) {
                    "mpn" -> part.mpn.contains(v.trim(), ignoreCase)
                    "mfg" -> part.manufacturer.contains(v.trim(), ignoreCase)
                    "sku" -> part.sku.contains(v.trim(), ignoreCase)
                    "desc" -> desc.contains(v.trim(), ignoreCase)
                    else -> false
                }
            } else {
                part.mpn.contains(q, ignoreCase)
                        || part.manufacturer.contains(q, ignoreCase)
                        || desc.contains(q, ignoreCase)
            }
        }
    }
//    return parts.filter{ part ->
//        part.mpn.contains(q, ignoreCase)
//                || part.manufacturer.contains(q, ignoreCase)
//                || part.description.contains(q, ignoreCase)
//    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PartsScreen(nav: NavHostController, state: AppViewModel) {
    val context = LocalContext.current;
    val snackbarState = remember { SnackbarHostState() }
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(state.settings.defaultSort) }
    var showSearch by remember { mutableStateOf(false) }

    var filteredParts = search(state.parts, searchText)
    if (sortOrder.isNotBlank()) {
        // These must match values in the sortOptions list
        filteredParts = when (sortOrder) {
            "mpn" -> filteredParts.sortedBy { part -> part.mpn }
            "-mpn" -> filteredParts.sortedByDescending { part -> part.mpn }
            "created" -> filteredParts.sortedBy { part -> part.created }
            "-created" -> filteredParts.sortedByDescending { part -> part.created }
            "updated" -> filteredParts.sortedBy { part -> part.updated }
            "-updated" -> filteredParts.sortedByDescending { part -> part.updated }
            else -> filteredParts
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            Log.i(TAG, "scanned code: ${result.contents}")
            scope.launch {
                var scan = Scan(id = state.scans.size + 1, value = result.contents)
                state.addScan(scan);
                var part = scan.part;
                if (part != null && part.mpn.length > 0) {
                    val existingPart = state.parts.find { it.mpn == part.mpn };
                    if (existingPart == null) {
                        state.addPart(part);
                        snackbarState.showSnackbar(message = "Scanned new part ${part.mpn}")
                        nav.navigate("edit-part?id=${part.id}&import=1")
                    } else {
                        snackbarState.showSnackbar(message = "Opening part ${part.mpn}")
                        nav.navigate("edit-part?id=${existingPart.id}")
                    }
                } else {
                    snackbarState.showSnackbar(message = "Unknown barcode format")
                }
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.sqlite3")
    ) { mediaPath ->
        Log.d("Export", "Media path is ${mediaPath}");
        if (mediaPath != null) {
            context.contentResolver.openOutputStream(mediaPath, "wt")?.use { stream ->
                if (state.exportDb(stream) > 0) {
                    Toast.makeText(context, "Export complete!", 3000).show()
                } else {
                    Toast.makeText(context, "Export failed!", 3000).show()
                }

            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { mediaPath ->
        Log.d("Import", "Media path is ${mediaPath}");
        if (mediaPath != null) {
            scope.launch {
                context.contentResolver.openInputStream(mediaPath)?.use { stream ->

                    if (state.importDb(context, stream) > 0) {
                        state.reload()
                        Toast.makeText(context, "Import complete!", 3000).show()
                    } else {
                        Toast.makeText(context, "Import failed!", 3000).show()
                    }

                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Parts")
                        Text(
                            "${state.parts.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add part") },
                            onClick = { nav.navigate("edit-part") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Add part",
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { nav.navigate("settings") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export database") },
                            onClick = {
                                expanded = false
                                exportLauncher.launch("inventory.db")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.FileUpload,
                                    contentDescription = "Export",
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import database") },
                            onClick = {
                                expanded = false
                                importLauncher.launch(
                                    arrayOf(
                                        "application/x-sqlite3",
                                        "application/vnd.sqlite3",
                                        "application/octet-stream"
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = "Import",
                                )
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    onClick = {
                        nav.navigate("scans")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "Go to scans"
                    )
                }
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    onClick = {
                        showSearch = !showSearch
                        if (!showSearch) {
                            searchText = ""
                        }
                    }) {
                    Icon(
                        imageVector = if (!showSearch) Icons.Filled.Search else Icons.Filled.SearchOff,
                        contentDescription = "Search",
                    )
                }
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape,
                    onClick = {
                        if (!cameraPermissionState.status.isGranted) {
                            val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                                // If the user has denied the permission but the rationale can be shown,
                                // then gently explain why the app requires this permission
                                "The camera is important for this app. Please grant the permission."
                            } else {
                                // If it's the first time the user lands on this feature, or the user
                                // doesn't want to be asked again for this permission, explain that the
                                // permission is required
                                "Camera permission required for this feature to be available. " +
                                        "Please grant the permission"
                            }
                            scope.launch {
                                var result = snackbarState.showSnackbar(
                                    message = textToShow,
                                    actionLabel = "OK",

                                    )
                                when (result) {
                                    SnackbarResult.Dismissed -> {
                                    }

                                    SnackbarResult.ActionPerformed -> {
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                }
                            }
                        } else {
                            scanLauncher.launch(state.scanOptions)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "Scan barcode"
                    )
                }
            }

        },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                PartsList(parts = filteredParts) { part ->
                    Log.d(TAG, "Clicked part ${part}")
                    nav.navigate("edit-part?id=${part.id}")
                }
            }

        },
        bottomBar = {
            if (showSearch) {
                BottomAppBar(
                    modifier = Modifier.shadow(20.dp),
                    containerColor = MaterialTheme.colorScheme.background,
                    actions = {
                        OutlinedTextField(
                            label = { Text("Search") },
                            placeholder = {
                                Text(
                                    "Part number, manufacturer, or description. Use , or and to separate",
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                )
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .align(Alignment.CenterVertically),
                            trailingIcon = {
                                if (searchText.length > 0) {
                                    Icon(
                                        imageVector = Icons.Filled.Cancel,
                                        contentDescription = "Clear",
                                        modifier = Modifier.clickable {
                                            searchText = ""
                                        }
                                    )
                                }
                            },
                            value = searchText,
                            onValueChange = {
                                searchText = it
                            }
                        )
                    }
                )
            }
        }
    )
}

@Composable
fun PartsList(parts: List<Part>, onPartClicked: (part: Part) -> Unit) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(parts, key = { p -> p.id }) { part ->
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable {
                        onPartClicked(part)
                    }
            ) {
                if (part.pictureUrl.length > 0) {
                    val req =
                        ImageRequest.Builder(LocalContext.current).data(part.pictureUrl)
                            .diskCachePolicy(
                                CachePolicy.ENABLED
                            ).httpHeaders(
                                NetworkHeaders.Builder().add("User-Agent", userAgent).build()
                            );
                    AsyncImage(
                        model = req.build(),
                        contentDescription = null,
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .padding(8.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PictureInPicture,
                        contentDescription = null,
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .padding(8.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(5f)
                ) {

                    Text(
                        part.mpn,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Text(
                        part.manufacturer,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Text(
                        part.description,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                Text(
                    "${part.num_in_stock}",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(64.dp)
                )
            }

        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmRemoveDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            modifier = Modifier.padding(8.dp),
            elevation = CardDefaults.cardElevation(20.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Are you sure you want to delete?",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TextButton(
                        onClick = { onDismiss() }
                    ) {
                        Text("No")
                    }
                    TextButton(
                        onClick = {
                            onConfirm()
                        }
                    ) {
                        Text("Yes", color = Colors.Danger)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartEditorScreen(nav: NavHostController, state: AppViewModel, originalPart: Part,  autoImport: Boolean) {
    Log.i(TAG, "Editing part ${originalPart}")
    val context = LocalContext.current;
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

    var partId by remember { mutableStateOf(originalPart.id) };
    var partDesc by remember { mutableStateOf(originalPart.description) };
    var partManufacturer by remember { mutableStateOf(originalPart.manufacturer) };
    var partLocation by remember { mutableStateOf(originalPart.location) };

    var partSku by remember { mutableStateOf(originalPart.sku) };
    var partMpn by remember { mutableStateOf(originalPart.mpn) };
    var partSupplier by remember { mutableStateOf(originalPart.supplier) };
    var partSupplierUrl by remember { mutableStateOf(originalPart.supplierUrl()) }
    var partNumOrdered by remember { mutableStateOf(originalPart.num_ordered) };
    var partNumInStock by remember { mutableStateOf(originalPart.num_in_stock) };
    var partUnitPrice by remember { mutableStateOf(originalPart.unit_price) };

    var partDatasheet by remember { mutableStateOf(originalPart.datasheetUrl) };
    var partImage by remember { mutableStateOf(originalPart.pictureUrl) };
    var partUpdated by remember { mutableStateOf(originalPart.updated) };
    var editMode by remember { mutableStateOf(false) }
    var editing = partId == 0 || editMode;

    val importPartData: () -> Unit = {
        scope.launch {
            if (originalPart.supplier.isBlank()) {
                originalPart.supplier = state.settings.defaultSupplier;
                partSupplier = state.settings.defaultSupplier;
            }
            when (originalPart.importFromSupplier()) {
                ImportResult.Success -> {
                    // Force update
                    partImage = originalPart.pictureUrl
                    partDatasheet = originalPart.datasheetUrl
                    partDesc = originalPart.description
                    partManufacturer = originalPart.manufacturer
                    partUpdated = originalPart.updated

                    // If import is pressed before save
                    var msg = "Imported!"
                    if (partId == 0) {
                        if (state.addPart(originalPart)) {
                            partId = originalPart.id
                        } else {
                            msg = "A part with this MPN already exists!"
                        }
                    } else {
                        state.savePart(originalPart);
                    }
                    snackbarState.showSnackbar(msg)
                }

                ImportResult.NoData -> {
                    snackbarState.showSnackbar("No data was imported.")
                }

                ImportResult.MultipleResults -> {
                    val r = snackbarState.showSnackbar(
                        "No exact part match found. Try adding an SKU",
                        actionLabel = "Search supplier website"
                    )
                    when (r) {
                        SnackbarResult.ActionPerformed -> {
                            try {
                                val browserIntent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(originalPart.supplierUrl())
                                    )
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarState.showSnackbar("Search url is invalid")
                                }
                            }
                        }

                        SnackbarResult.Dismissed -> {}
                    }
                }

                else -> {
                    snackbarState.showSnackbar("Failed to import.")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(20.dp),
                title = {
                    if (partId == 0) {
                        Text("Add Part")
                    } else {
                        SelectionContainer {
                            Text(partMpn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon =
                    {
                        IconButton(onClick = { nav.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    var showRemoveDialog by remember { mutableStateOf(false) };
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (partMpn.isNotBlank() || partSku.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Import from supplier") },
                                onClick = { importPartData() },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.SystemUpdateAlt,
                                        contentDescription = "Import",
                                    )
                                })
                        }
                        if (partId != 0) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = Colors.Danger) },
                                onClick = {
                                    showRemoveDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        //color = Colors.Danger,
                                    )
                                })
                        }
                    }
                    if (showRemoveDialog) {
                        ConfirmRemoveDialog(
                            onDismiss = { showRemoveDialog = false },
                            onConfirm = {
                                scope.launch {
                                    nav.navigateUp()
                                    state.removePart(originalPart)
                                }
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    onClick = { editMode = !editMode }
                ) {
                    Icon(
                        imageVector = if (!editMode) Icons.Filled.Edit else Icons.Filled.EditOff,
                        contentDescription = "Edit"
                    )
                }
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape,
                    onClick = {
                        scope.launch {
                            var msg = "Part saved!"
                            // Set default supplier if none was set
                            if (originalPart.supplier.isBlank()) {
                                originalPart.supplier = state.settings.defaultSupplier;
                                partSupplier = state.settings.defaultSupplier;
                            }
                            if (partId == 0) {
                                if (state.addPart(originalPart)) {
                                    partId = originalPart.id
                                    Log.d(TAG, "Added new part ${originalPart.id}")
                                } else {
                                    msg = "Cannot save. A part with this MPN already exists!"
                                }

                            } else {
                                state.savePart(originalPart);
                            }
                            snackbarState.showSnackbar(msg)
                        }

                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Save"
                    )
                }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                if (partImage.length > 0) {
                    AsyncImage(
                        model = partImage,
                        contentDescription = null,
                        modifier = Modifier
                            .height(200.dp)
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                }

                Row {
                    if (partDatasheet.length > 0) {
                        Button(
                            modifier = Modifier.padding(8.dp),
                            onClick = {
                                try {
                                    val browserIntent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(partDatasheet))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarState.showSnackbar("Datasheet url is invalid")
                                    }
                                }
                            }
                        ) {
                            Text("Datasheet")
                        }
                    }
                    if (partSupplierUrl.isNotBlank()) {
                        Button(
                            modifier = Modifier.padding(8.dp),
                            onClick = {
                                try {
                                    val browserIntent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(partSupplierUrl))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarState.showSnackbar("Supplier url is invalid")
                                    }
                                }
                            }
                        ) {
                            Text("Open supplier website")
                        }
                    }
                }

                if (editing) {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        singleLine = true,
                        value = partMpn,
                        onValueChange = { partMpn = it; originalPart.mpn = it },
                        label = { Text("MPN") }
                    )
                    var mfgMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        mfgMenuExpanded,
                        onExpandedChange = { mfgMenuExpanded = it }) {
                        OutlinedTextField(
                            modifier = Modifier
                                .padding(8.dp)
                                .menuAnchor() // Needed or dropdown will not show
                                .fillMaxWidth(),
                            value = partManufacturer,
                            onValueChange = {
                                partManufacturer = it; originalPart.manufacturer = it
                            },
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = mfgMenuExpanded)
                            },
                            label = { Text("Manufacturer") }
                        )
                        // filter options based on text field value (i.e. crude autocomplete)
                        val filterOpts = state.manufacturerOptions.filter {
                            it.contains(
                                partManufacturer,
                                ignoreCase = true
                            )
                        }
                        if (filterOpts.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = mfgMenuExpanded,
                                onDismissRequest = { mfgMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                filterOpts.forEach { option ->
                                    DropdownMenuItem(
                                        onClick = {
                                            partManufacturer = option
                                            originalPart.manufacturer = option
                                            mfgMenuExpanded = false
                                        }
                                    ) {
                                        Text(text = option)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        value = partDesc,
                        onValueChange = { partDesc = it; originalPart.description = it },
                        singleLine = true,
                        label = { Text("Description") }
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        singleLine = true,
                        value = partSku,
                        onValueChange = { partSku = it; originalPart.sku = it },
                        label = { Text("Supplier SKU") }
                    )

                    var supplierMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        supplierMenuExpanded,
                        onExpandedChange = { supplierMenuExpanded = it }) {
                        OutlinedTextField(
                            modifier = Modifier
                                .padding(8.dp)
                                .menuAnchor() // Needed or dropdown will not show
                                .fillMaxWidth(),
                            singleLine = true,
                            // colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            value = partSupplier,
                            onValueChange = { partSupplier = it; originalPart.supplier = it },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierMenuExpanded)
                            },
                            label = { Text("Supplier") }
                        )
                        // filter options based on text field value (i.e. crude autocomplete)
                        val filterOpts = state.supplierOptions.filter {
                            it.contains(
                                partSupplier,
                                ignoreCase = true
                            )
                        }
                        if (filterOpts.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = supplierMenuExpanded,
                                onDismissRequest = { supplierMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                filterOpts.forEach { option ->
                                    DropdownMenuItem(
                                        onClick = {
                                            partSupplier = option
                                            originalPart.supplier = option
                                            supplierMenuExpanded = false
                                        }
                                    ) {
                                        Text(text = option)
                                    }
                                }
                            }
                        }
                    }

                } else {
                    Text(
                        "Last updated on ${partUpdated.toString()}",
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                    Text(
                        "MPN",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
                    )
                    SelectionContainer {
                        Text(partMpn, modifier = Modifier.padding(8.dp))
                    }
                    Text(
                        "Manufacturer",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
                    )
                    SelectionContainer {
                        Text(
                            if (partManufacturer.trimmedLength() > 0) partManufacturer else "N/A",
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Text(
                        "Description",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
                    )
                    SelectionContainer {
                        Text(
                            if (partDesc.trimmedLength() > 0) partDesc else "N/A",
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Text(
                        "Unit Price",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
                    )
                    SelectionContainer {
                        Text(
                            "$${"%.5f".format(partUnitPrice)}",
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Text(
                        "Total Price",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
                    )
                    SelectionContainer {
                        Text(
                            "$${"%.5f".format(partUnitPrice * partNumOrdered)}",
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Text(
                        "Supplier",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
                    )
                    SelectionContainer {
                        Text(
                            if (partSupplier.trimmedLength() > 0) partSupplier else "N/A",
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Text(
                        "Supplier SKU",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
                    )
                    SelectionContainer {
                        Text(
                            if (partSku.trimmedLength() > 0) partSku else "N/A",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    value = partNumOrdered.toString(),
                    onValueChange = {
                        try {
                            val v = Integer.parseUnsignedInt(it);
                            partNumOrdered = v;
                            originalPart.num_ordered = v
                        } catch (e: Exception) {
                            Log.d(TAG, e.toString());
                            partNumOrdered = 0;
                            originalPart.num_ordered = 0
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    label = { Text("Qty ordered") }
                )
                OutlinedTextField(

                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    value = partNumInStock.toString(),
                    onValueChange = {
                        try {
                            val v = Integer.parseUnsignedInt(it);
                            partNumInStock = v;
                            originalPart.num_in_stock = v
                        } catch (e: Exception) {
                            Log.d(TAG, e.toString());
                            partNumInStock = 0;
                            originalPart.num_in_stock = 0
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    label = { Text("Qty in stock") }
                )

                OutlinedTextField(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    singleLine = true,
                    value = partLocation,
                    onValueChange = { partLocation = it; originalPart.location = it },
                    label = { Text("Location") }
                )
                if (editing) {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        value = partUnitPrice.toString(),
                        onValueChange = {
                            try {
                                val v = it.toDouble();
                                partUnitPrice = v;
                                originalPart.unit_price = v
                            } catch (e: Exception) {
                                Log.d(TAG, e.toString());
                                partUnitPrice = 0.0
                                originalPart.unit_price = 0.0
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        label = { Text("Unit price") }
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        value = partDatasheet,
                        onValueChange = { partDatasheet = it; originalPart.datasheetUrl = it },
                        singleLine = true,
                        label = { Text("Datasheet Url") }
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        value = partImage,
                        onValueChange = { partImage = it; originalPart.pictureUrl = it },
                        singleLine = true,
                        label = { Text("Image Url") }
                    )
                }
                Text(
                    "Created on ${originalPart.created.toString()}",
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp, 4.dp)
                )
            }
        }
    )

    // Auto import data if a new part is scanned
    if (autoImport) {
        LaunchedEffect(autoImport) {
            snackbarState.showSnackbar("Importing part data...")
            importPartData();
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavHostController, state: AppViewModel) {
    Log.i(TAG, "Settings screen")
    val context = LocalContext.current;
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    var defaultSupplier by remember { mutableStateOf(state.settings.defaultSupplier) };
    var defaultSort by remember { mutableStateOf(state.settings.defaultSort) };
    val sortOptions = mapOf(
        "" to "Default",
        "mpn" to "MPN asc",
        "-mpn" to "MPN desc",
        "created" to "Created asc",
        "-created" to "Created desc",
        "updated" to "Updated asc",
        "-updated" to "Updated desc",
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(20.dp),
                title = {
                    Text("Settings")
                },
                navigationIcon =
                    {
                        IconButton(onClick = {
                            scope.launch {
                                state.settings.defaultSupplier = defaultSupplier;
                                state.settings.defaultSort = defaultSort;
                                state.saveSettings(context.applicationContext);
                                nav.navigateUp();
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    value = defaultSupplier,
                    onValueChange = { defaultSupplier = it; },
                    singleLine = true,
                    label = { Text("Default Supplier") }
                )

                var sortMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    sortMenuExpanded,
                    onExpandedChange = { sortMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(8.dp)
                            .menuAnchor() // Needed or dropdown will not show
                            .fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        value = sortOptions.getOrDefault(defaultSort, "Default"),
                        onValueChange = {},
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortMenuExpanded)
                        },
                        label = { Text("Default sort order") }
                    )
                    ExposedDropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        sortOptions.keys.forEach { k ->
                            DropdownMenuItem(
                                onClick = {
                                    defaultSort = k
                                    sortMenuExpanded = false
                                }
                            ) {
                                Text(text = sortOptions[k]!!)
                            }
                        }
                    }
                }
            }
        },
    )

}
//
//
//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    var state = AppViewModel(database = (application as App).db)
//    AppTheme {
//        Main(state)
//    }
//}