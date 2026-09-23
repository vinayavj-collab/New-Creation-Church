package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.AdminUser
import com.example.ui.components.AdminInvitationAccessDialog
import com.example.data.model.AdminHierarchy
import com.example.data.model.UserActivityItem
import com.example.data.model.UserActivityType
import com.example.data.model.UserProfileData
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenAdminPanel: () -> Unit = {},
    onOpenBible: (bookId: Int, chapter: Int, verse: Int) -> Unit = { _, _, _ -> },
    onOpenPrayer: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val allAdmins by viewModel.allAdmins.collectAsState()
    val allDesignations by viewModel.allDesignations.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageUriForCrop by remember { mutableStateOf<Uri?>(null) }

    val handleAdminAccess = {
        val isMasterVinay = com.example.util.ProfileManager.isVinayProfile() ||
                currentAdmin?.rank == com.example.data.model.AdminHierarchy.RANK_VINAY_KUMAR ||
                currentAdmin?.designation?.contains("Vinay", ignoreCase = true) == true

        if (settings.masterAdminPasswordEnabled) {
            val fragmentActivity = context as? androidx.fragment.app.FragmentActivity
            if (settings.isBiometricEnabled && fragmentActivity != null && com.example.util.BiometricAuthManager.isBiometricAvailable(context)) {
                com.example.util.BiometricAuthManager.authenticate(
                    activity = fragmentActivity,
                    title = "एडमिन बायोमेट्रिक सत्यापन",
                    subtitle = "एडमिन प्रोफ़ाइल व अधिकारों में प्रवेश हेतु अंगूठा/फ़ेस स्कैन करें",
                    onSuccess = {
                        onOpenAdminPanel()
                    },
                    onError = {
                        showAdminLoginDialog = true
                    }
                )
            } else {
                showAdminLoginDialog = true
            }
        } else {
            if (isMasterVinay && currentAdmin == null) {
                viewModel.loginVinayKumarAutomatic { _, _ ->
                    onOpenAdminPanel()
                }
            } else {
                onOpenAdminPanel()
            }
        }
    }

    // Auto-redirect ONLY if masterAdminPasswordEnabled is false (Lock is OFF)
    LaunchedEffect(currentAdmin, com.example.util.ProfileManager.isVinayProfile(), settings.masterAdminPasswordEnabled) {
        if (!settings.masterAdminPasswordEnabled) {
            if (com.example.util.ProfileManager.isVinayProfile()) {
                if (currentAdmin == null) {
                    viewModel.loginVinayKumarAutomatic { _, _ ->
                        onOpenAdminPanel()
                    }
                } else {
                    onOpenAdminPanel()
                }
            } else if (currentAdmin != null) {
                onOpenAdminPanel()
            }
        }
    }

    // Editable form state initialized from current profile
    var nameInput by remember(userProfile.displayName) { mutableStateOf(userProfile.displayName) }
    var roleInput by remember(userProfile.role) { mutableStateOf(userProfile.role) }
    var phoneInput by remember(userProfile.phoneNumber) { mutableStateOf(userProfile.phoneNumber) }
    var cityInput by remember(userProfile.city) { mutableStateOf(userProfile.city) }
    var bioInput by remember(userProfile.bio) { mutableStateOf(userProfile.bio) }
    var favVerseInput by remember(userProfile.favoriteVerse) { mutableStateOf(userProfile.favoriteVerse) }
    var storageModeInput by remember(userProfile.storagePermissionMode) {
        mutableStateOf(if (userProfile.storagePermissionMode.isNotBlank()) userProfile.storagePermissionMode else "AUTOMATIC")
    }
    var churchNameInput by remember(userProfile.churchName) { mutableStateOf(userProfile.churchName) }
    var orgNameInput by remember(userProfile.organizationName) { mutableStateOf(userProfile.organizationName) }
    var yearsInFaithInput by remember(userProfile.yearsInFaith) { mutableStateOf(if (userProfile.yearsInFaith > 0) userProfile.yearsInFaith.toString() else "") }
    var distanceInput by remember(userProfile.distanceToChurchKm) { mutableStateOf(if (userProfile.distanceToChurchKm > 0.0) userProfile.distanceToChurchKm.toString() else "") }
    var baptismStatusInput by remember(userProfile.baptismStatus) { mutableStateOf(userProfile.baptismStatus) }
    var ministryInterestInput by remember(userProfile.ministryInterest) { mutableStateOf(userProfile.ministryInterest) }
    var dobInput by remember(userProfile.dateOfBirth) { mutableStateOf(userProfile.dateOfBirth) }
    var genderInput by remember(userProfile.gender) { mutableStateOf(userProfile.gender) }
    var maritalStatusInput by remember(userProfile.maritalStatus) { mutableStateOf(userProfile.maritalStatus) }
    var cityPincodeInput by remember(userProfile.cityPincode) { mutableStateOf(userProfile.cityPincode) }

    var isSaving by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showStoragePermissionDialog by remember { mutableStateOf(false) }

    val requiredStoragePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Activity history state
    var selectedActivityFilter by remember { mutableStateOf(UserActivityType.ALL) }
    var activitySearchQuery by remember { mutableStateOf("") }
    val activityList by viewModel.getActivityHistory(selectedActivityFilter).collectAsState(initial = emptyList())

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedImageUriForCrop = tempCameraUri
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = File(context.cacheDir, "camera_avatar_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "कैमरा शुरू करने में समस्या आई", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "फोटो खींचने के लिए कैमरा अनुमति आवश्यक है", Toast.LENGTH_LONG).show()
        }
    }

    // Automatic Gallery Photo Picker launcher (Zero Permission)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUriForCrop = uri
        }
    }

    // Custom Storage File Picker Launcher
    val customFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUriForCrop = uri
        }
    }

    // Storage Permission launcher for Custom Mode
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "स्टोरेज अनुमति स्वीकृत ✅ फ़ाइल चुनें", Toast.LENGTH_SHORT).show()
            customFilePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "कस्टम फ़ाइल चयन हेतु स्टोरेज अनुमति आवश्यक है", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "उपयोगकर्ता प्रोफ़ाइल (Profile)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (userProfile.displayName.isNotBlank()) userProfile.displayName else "व्यक्तिगत विवरण व इतिहास",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("user_profile_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "वापस जाएं")
                    }
                },
                actions = {
                    IconButton(
                        onClick = handleAdminAccess,
                        modifier = Modifier.testTag("admin_toggle_top_button")
                    ) {
                        Icon(
                            imageVector = if (currentAdmin != null) Icons.Default.VerifiedUser else Icons.Default.AdminPanelSettings,
                            contentDescription = "एडमिन कंट्रोल",
                            tint = if (currentAdmin != null) GoldWarm else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (pagerState.currentPage == 0) {
                        IconButton(
                            onClick = {
                                isSaving = true
                                val updated = userProfile.copy(
                                    displayName = nameInput.trim(),
                                    phoneNumber = phoneInput.trim(),
                                    city = cityInput.trim(),
                                    bio = bioInput.trim(),
                                    favoriteVerse = favVerseInput.trim(),
                                    storagePermissionMode = storageModeInput,
                                    churchName = churchNameInput.trim(),
                                    organizationName = orgNameInput.trim(),
                                    yearsInFaith = yearsInFaithInput.toIntOrNull() ?: 0,
                                    distanceToChurchKm = distanceInput.toDoubleOrNull() ?: 0.0,
                                    baptismStatus = baptismStatusInput,
                                    ministryInterest = ministryInterestInput.trim(),
                                    dateOfBirth = dobInput.trim(),
                                    gender = genderInput.trim(),
                                    maritalStatus = maritalStatusInput.trim(),
                                    cityPincode = cityPincodeInput.trim()
                                )
                                viewModel.updateUserProfile(updated)
                                isSaving = false
                                saveSuccessMessage = "प्रोफ़ाइल सुरक्षित हो गई!"
                                Toast.makeText(context, "प्रोफ़ाइल सुरक्षित हो गई! ✅", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("save_profile_top_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "सुरक्षित करें", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Compact Row-Based Header
            ProfileCompactHeader(
                userProfile = userProfile,
                currentAdmin = currentAdmin,
                onAvatarClick = { showPhotoPickerSheet = true },
                onOpenAdminPanel = onOpenAdminPanel,
                onLogoutAdmin = {
                    viewModel.logoutAdmin()
                    Toast.makeText(context, "एडमिन से लॉगआउट किया गया", Toast.LENGTH_SHORT).show()
                }
            )

            // Swipeable Tab navigation
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("विवरण व संपादन", fontSize = 12.sp, fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("इतिहास (${activityList.size})", fontSize = 12.sp, fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // Profile Edit Form Tab
                        ProfileEditForm(
                            nameInput = nameInput,
                            onNameChange = { nameInput = it },
                            phoneInput = phoneInput,
                            onPhoneChange = { phoneInput = it },
                            cityInput = cityInput,
                            onCityChange = { cityInput = it },
                            bioInput = bioInput,
                            onBioChange = { bioInput = it },
                            favVerseInput = favVerseInput,
                            onFavVerseChange = { favVerseInput = it },
                            storageModeInput = storageModeInput,
                            onStorageModeChange = { storageModeInput = it },
                            churchNameInput = churchNameInput,
                            onChurchNameChange = { churchNameInput = it },
                            orgNameInput = orgNameInput,
                            onOrgNameChange = { orgNameInput = it },
                            yearsInFaithInput = yearsInFaithInput,
                            onYearsInFaithChange = { yearsInFaithInput = it },
                            distanceInput = distanceInput,
                            onDistanceChange = { distanceInput = it },
                            baptismStatusInput = baptismStatusInput,
                            onBaptismStatusChange = { baptismStatusInput = it },
                            ministryInterestInput = ministryInterestInput,
                            onMinistryInterestChange = { ministryInterestInput = it },
                            dobInput = dobInput,
                            onDobChange = { dobInput = it },
                            genderInput = genderInput,
                            onGenderChange = { genderInput = it },
                            maritalStatusInput = maritalStatusInput,
                            onMaritalStatusChange = { maritalStatusInput = it },
                            cityPincodeInput = cityPincodeInput,
                            onCityPincodeChange = { cityPincodeInput = it },
                            onOpenPhotoPicker = { showPhotoPickerSheet = true },
                            isSaving = isSaving,
                            onSave = {
                                val updated = userProfile.copy(
                                    displayName = nameInput.trim(),
                                    phoneNumber = phoneInput.trim(),
                                    city = cityInput.trim(),
                                    bio = bioInput.trim(),
                                    favoriteVerse = favVerseInput.trim(),
                                    storagePermissionMode = storageModeInput,
                                    churchName = churchNameInput.trim(),
                                    organizationName = orgNameInput.trim(),
                                    yearsInFaith = yearsInFaithInput.toIntOrNull() ?: 0,
                                    distanceToChurchKm = distanceInput.toDoubleOrNull() ?: 0.0,
                                    baptismStatus = baptismStatusInput,
                                    ministryInterest = ministryInterestInput.trim(),
                                    dateOfBirth = dobInput.trim(),
                                    gender = genderInput.trim(),
                                    maritalStatus = maritalStatusInput.trim(),
                                    cityPincode = cityPincodeInput.trim()
                                )
                                viewModel.updateUserProfile(updated)
                                Toast.makeText(context, "प्रोफ़ाइल सफलतापूर्वक सुरक्षित हो गई! 🎉", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    1 -> {
                        // Activity History Tab
                        ActivityHistoryContent(
                            activityList = activityList,
                            selectedFilter = selectedActivityFilter,
                            onFilterSelected = { selectedActivityFilter = it },
                            searchQuery = activitySearchQuery,
                            onSearchQueryChange = { activitySearchQuery = it },
                            onClearHistory = {
                                coroutineScope.launch {
                                    viewModel.userProfileRepository.clearHistory()
                                    Toast.makeText(context, "हालिया इतिहास साफ़ कर दिया गया", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onItemClick = { item ->
                                when (item.type) {
                                    UserActivityType.BIBLE_READ -> {
                                        // Open Bible Home
                                    }
                                    UserActivityType.PRAYER_POSTED, UserActivityType.TESTIMONY_SHARED -> {
                                        onOpenPrayer()
                                    }
                                    UserActivityType.STUDY_NOTE -> {
                                        onOpenNotes()
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet for selecting Camera or Gallery
    if (showPhotoPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoPickerSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "प्रोफ़ाइल फोटो बदलें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "कैमरा से नई फोटो खींचें या गैलरी से चुनें",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Storage Mode Switcher (Automatic by default vs Custom)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (storageModeInput == "AUTOMATIC") Icons.Default.AutoAwesome else Icons.Default.FolderShared,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "स्टोरेज परमिशन मोड:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = storageModeInput == "AUTOMATIC",
                                onClick = {
                                    storageModeInput = "AUTOMATIC"
                                    viewModel.updateUserProfile(userProfile.copy(storagePermissionMode = "AUTOMATIC"))
                                },
                                label = {
                                    Text(
                                        "⚡ Automatic (स्वचालित)",
                                        fontSize = 11.sp,
                                        fontWeight = if (storageModeInput == "AUTOMATIC") FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (storageModeInput == "AUTOMATIC") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = storageModeInput == "CUSTOM",
                                onClick = {
                                    storageModeInput = "CUSTOM"
                                    viewModel.updateUserProfile(userProfile.copy(storagePermissionMode = "CUSTOM"))
                                },
                                label = {
                                    Text(
                                        "📁 Custom (कस्टम)",
                                        fontSize = 11.sp,
                                        fontWeight = if (storageModeInput == "CUSTOM") FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (storageModeInput == "CUSTOM") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = if (storageModeInput == "AUTOMATIC")
                                "✨ स्वचालित (Default): ज़ीरो-परमिशन सुरक्षित फ़ोटो पिकर। कोई स्टोरेज परमिशन नहीं चाहिए।"
                            else
                                "📂 कस्टम: आपकी डिवाइस स्टोरेज फ़ाइलों से चुनने हेतु स्टोरेज अनुमति की आवश्यकता होगी।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Option 1: Take Photo with Camera
                Surface(
                    onClick = {
                        showPhotoPickerSheet = false
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            try {
                                val file = File(context.cacheDir, "camera_avatar_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "कैमरा लोड करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showCameraPermissionDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("कैमरा से फोटो लें (Take Photo)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("अपने फोन के कैमरे से तुरंत फोटो खींचें", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Option 2: Choose from Gallery
                Surface(
                    onClick = {
                        showPhotoPickerSheet = false
                        if (storageModeInput == "AUTOMATIC") {
                            // Automatic mode: Zero-permission Photo Picker
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            // Custom mode: Check storage permission and use custom file picker
                            val hasStoragePerm = ContextCompat.checkSelfPermission(
                                context,
                                requiredStoragePermission
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasStoragePerm) {
                                customFilePickerLauncher.launch("image/*")
                            } else {
                                showStoragePermissionDialog = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                if (storageModeInput == "AUTOMATIC") "गैलरी से चुनें (Automatic Photo Picker)" else "कस्टम स्टोरेज से चुनें (Custom File / Storage)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                if (storageModeInput == "AUTOMATIC") "स्वचालित व सुरक्षित फ़ोटो चयन" else "डिवाइस स्टोरेज अनुमति के साथ फ़ाइल चुनें",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 3: Remove photo if exists
                if (userProfile.photoUriOrPath.isNotBlank()) {
                    Surface(
                        onClick = {
                            showPhotoPickerSheet = false
                            viewModel.updateUserProfile(userProfile.copy(photoUriOrPath = ""))
                            Toast.makeText(context, "प्रोफ़ाइल फोटो हटा दी गई", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(14.dp))
                            Text("फोटो हटाएं (Remove Photo)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Camera Permission Dialog
    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            icon = {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("कैमरा अनुमति (Camera Permission)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "प्रोफ़ाइल तस्वीर खींचने के लिए आपके डिवाइस के कैमरे की अनुमति आवश्यक है। क्या आप अनुमति देना चाहते हैं?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraPermissionDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("अनुमति दें (Allow)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Admin PIN Login Modal Dialog
    if (showAdminLoginDialog) {
        AdminInvitationAccessDialog(
            viewModel = viewModel,
            onDismiss = { showAdminLoginDialog = false },
            onOpenNormalProfile = { showAdminLoginDialog = false },
            onOpenAdminPanel = {
                showAdminLoginDialog = false
                onOpenAdminPanel()
            },
            initialStage = com.example.ui.components.InvitationStage.SERIAL
        )
    }

    // Image Crop Dialog
    if (selectedImageUriForCrop != null) {
        ImageCropDialog(
            imageUri = selectedImageUriForCrop!!,
            onDismiss = { selectedImageUriForCrop = null },
            onCropped = { croppedBitmap ->
                coroutineScope.launch {
                    val savedPath = viewModel.saveCroppedAvatarBitmap(croppedBitmap)
                    if (savedPath.isNotBlank()) {
                        Toast.makeText(context, "कस्टम प्रोफ़ाइल फोटो क्रॉप व सेव की गई! 🖼️", Toast.LENGTH_SHORT).show()
                    }
                    selectedImageUriForCrop = null
                }
            }
        )
    }

    // Storage Permission Dialog (For Custom Mode)
    if (showStoragePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showStoragePermissionDialog = false },
            icon = {
                Icon(Icons.Default.FolderShared, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("स्टोरेज अनुमति (Storage Permission)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "कस्टम मोड में आपकी डिवाइस फ़ाइलों व मीडिया से फोटो चुनने के लिए स्टोरेज (Media/Storage) अनुमति की आवश्यकता है।\n\n(नोट: यदि आप अनुमति नहीं देना चाहते, तो 'Automatic' मोड चुन सकते हैं जो बिना किसी परमिशन के काम करता है।)",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStoragePermissionDialog = false
                        storagePermissionLauncher.launch(requiredStoragePermission)
                    }
                ) {
                    Text("अनुमति दें (Grant Permission)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStoragePermissionDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun ProfileCompactHeader(
    userProfile: UserProfileData,
    currentAdmin: AdminUser? = null,
    onAvatarClick: () -> Unit,
    onOpenAdminPanel: () -> Unit = {},
    onLogoutAdmin: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (userProfile.photoUriOrPath.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(File(userProfile.photoUriOrPath)),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, GoldWarm, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = if (currentAdmin != null) currentAdmin.name.trim().take(1).uppercase() else userProfile.displayName.trim().take(1).uppercase()
                        if (initial.isNotBlank()) {
                            Text(
                                text = initial,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayName = when {
                        userProfile.displayName.isNotBlank() -> userProfile.displayName
                        currentAdmin != null && currentAdmin.name.isNotBlank() -> currentAdmin.name
                        else -> "अतिथि विश्वासी (Guest Member)"
                    }

                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(Modifier.width(6.dp))

                    val badgeText = when {
                        currentAdmin != null -> if (currentAdmin.rank >= AdminHierarchy.RANK_VINAY_KUMAR || currentAdmin.isMasterAdmin()) "👑 Master Admin" else "🛡️ ${currentAdmin.designation.ifBlank { "Admin" }}"
                        userProfile.isVerifiedVishwasi -> "✔️ Verified"
                        userProfile.role.isNotBlank() -> userProfile.role
                        else -> "विश्वासी"
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentAdmin != null) GoldWarm.copy(alpha = 0.2f) else if (userProfile.isVerifiedVishwasi) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer,
                        border = if (currentAdmin != null) androidx.compose.foundation.BorderStroke(0.5.dp, GoldWarm) else null
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentAdmin != null) GoldWarm else if (userProfile.isVerifiedVishwasi) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                if (userProfile.city.isNotBlank() && currentAdmin == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = userProfile.city,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (currentAdmin != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenAdminPanel,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("कंसोल", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onLogoutAdmin,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("लॉगआउट", fontSize = 11.sp)
                        }
                    }
                } else if (userProfile.bio.isNotBlank()) {
                    Text(
                        text = "“${userProfile.bio}”",
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileEditForm(
    nameInput: String,
    onNameChange: (String) -> Unit,
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    cityInput: String,
    onCityChange: (String) -> Unit,
    bioInput: String,
    onBioChange: (String) -> Unit,
    favVerseInput: String,
    onFavVerseChange: (String) -> Unit,
    storageModeInput: String,
    onStorageModeChange: (String) -> Unit,
    churchNameInput: String,
    onChurchNameChange: (String) -> Unit,
    orgNameInput: String,
    onOrgNameChange: (String) -> Unit,
    yearsInFaithInput: String,
    onYearsInFaithChange: (String) -> Unit,
    distanceInput: String,
    onDistanceChange: (String) -> Unit,
    baptismStatusInput: Boolean,
    onBaptismStatusChange: (Boolean) -> Unit,
    ministryInterestInput: String,
    onMinistryInterestChange: (String) -> Unit,
    dobInput: String,
    onDobChange: (String) -> Unit,
    genderInput: String,
    onGenderChange: (String) -> Unit,
    maritalStatusInput: String,
    onMaritalStatusChange: (String) -> Unit,
    cityPincodeInput: String,
    onCityPincodeChange: (String) -> Unit,
    onOpenPhotoPicker: () -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Smart Profile Reminder Banner
        val isIncomplete = churchNameInput.isBlank() || yearsInFaithInput.isBlank() || genderInput.isBlank()
        if (isIncomplete) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💡 स्मार्ट प्रोफ़ाइल रिमाइंडर",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "अपनी प्रोफ़ाइल पूरी करें (चर्च का नाम, विश्वास के वर्ष, आदि) ताकि आपको बेहतर आध्यात्मिक अनुभव और अपडेट मिल सकें।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "व्यक्तिगत जानकारी (Personal Info)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Display Name Field
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    label = { Text("पूरा नाम (Display Name)") },
                    placeholder = { Text("उदा. विनय कुमार") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("profile_name_input")
                )

                // City / Location
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = onCityChange,
                    label = { Text("शहर / राज्य (City / State)") },
                    placeholder = { Text("उदा. नई दिल्ली, भारत") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // City / Pincode
                OutlinedTextField(
                    value = cityPincodeInput,
                    onValueChange = onCityPincodeChange,
                    label = { Text("शहर / पिन कोड (City / Pincode)") },
                    placeholder = { Text("उदा. 110001") },
                    leadingIcon = { Icon(Icons.Default.PinDrop, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Phone Number (Optional)
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = onPhoneChange,
                    label = { Text("फ़ोन नंबर (वैकल्पिक)") },
                    placeholder = { Text("+91 9876543210") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Church Name (Optional)
                OutlinedTextField(
                    value = churchNameInput,
                    onValueChange = onChurchNameChange,
                    label = { Text("चर्च का नाम (Church Name - वैकल्पिक)") },
                    placeholder = { Text("उदा. ग्रेस बैपटिस्ट चर्च") },
                    leadingIcon = { Icon(Icons.Default.Church, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Organization Name (Optional)
                OutlinedTextField(
                    value = orgNameInput,
                    onValueChange = onOrgNameChange,
                    label = { Text("संस्था का नाम (Organization Name - वैकल्पिक)") },
                    placeholder = { Text("उदा. क्रिश्चियन फेलोशिप") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Years in Faith (Numeric Input)
                OutlinedTextField(
                    value = yearsInFaithInput,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) onYearsInFaithChange(it) },
                    label = { Text("कितने साल से विश्वास में हैं (Years in Faith)") },
                    placeholder = { Text("उदा. 5") },
                    leadingIcon = { Icon(Icons.Default.Timeline, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Distance to Church (Numeric Input)
                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = { onDistanceChange(it) },
                    label = { Text("चर्च की दूरी किमी में (Distance to Church in KM)") },
                    placeholder = { Text("उदा. 3.5") },
                    leadingIcon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Baptism Status (Switch)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "बपतिस्मा की स्थिति (Baptism Status):", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text(text = if (baptismStatusInput) "✅ बपतिस्मा लिया हुआ है" else "❌ अभी नहीं / विचारधीन", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = baptismStatusInput,
                        onCheckedChange = onBaptismStatusChange
                    )
                }

                // Ministry Interest
                OutlinedTextField(
                    value = ministryInterestInput,
                    onValueChange = onMinistryInterestChange,
                    label = { Text("सेवा में रुचि (Ministry Interest)") },
                    placeholder = { Text("उदा. संगीत सेवा, युवा सेवा, प्रार्थना") },
                    leadingIcon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Date of Birth
                OutlinedTextField(
                    value = dobInput,
                    onValueChange = onDobChange,
                    label = { Text("जन्म तिथि (Date of Birth)") },
                    placeholder = { Text("उदा. 15/08/1995") },
                    leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Gender (Chips)
                Text(text = "लिंग (Gender):", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("पुरुष (Male)", "महिला (Female)", "अन्य (Other)").forEach { g ->
                        FilterChip(
                            selected = genderInput == g,
                            onClick = { onGenderChange(g) },
                            label = { Text(g, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Marital Status (Chips)
                Text(text = "वैवाहिक स्थिति (Marital Status):", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("अविवाहित (Single)", "विवाहित (Married)").forEach { m ->
                        FilterChip(
                            selected = maritalStatusInput == m,
                            onClick = { onMaritalStatusChange(m) },
                            label = { Text(m, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Favorite Scripture Verse
                OutlinedTextField(
                    value = favVerseInput,
                    onValueChange = onFavVerseChange,
                    label = { Text("पसंदीदा बाइबिल वचन (Favorite Verse)") },
                    placeholder = { Text("उदा. यूहन्ना 3:16 या भजन संहिता 23:1") },
                    leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Bio / Spiritual thought
                OutlinedTextField(
                    value = bioInput,
                    onValueChange = onBioChange,
                    label = { Text("आत्मिक विचार / गवाही सारांश (Bio)") },
                    placeholder = { Text("परमेश्वर का अनुग्रह मेरे लिए काफी है।") },
                    leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = null) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Save Button
                val view = LocalView.current
                Button(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        onSave()
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSaving) "सुरक्षित हो रहा है..." else "प्रोफ़ाइल सुरक्षित करें (Save Profile)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}



@Composable
fun StoragePointItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActivityHistoryContent(
    activityList: List<UserActivityItem>,
    selectedFilter: UserActivityType,
    onFilterSelected: (UserActivityType) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onItemClick: (UserActivityItem) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredList = remember(activityList, searchQuery) {
        if (searchQuery.isBlank()) activityList
        else activityList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips Row
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("गतिविधि खोजें (Search history)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // Filter row
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(UserActivityType.entries) { type ->
                            FilterChip(
                                selected = selectedFilter == type,
                                onClick = { onFilterSelected(type) },
                                label = { Text(type.titleHindi, fontSize = 12.sp) },
                                leadingIcon = if (selectedFilter == type) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    // Count and Clear action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredList.size} गतिविधियां पाई गईं",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (activityList.isNotEmpty()) {
                            TextButton(
                                onClick = { showClearDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("इतिहास साफ़ करें", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.HistoryEdu,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "कोई परिणाम नहीं मिला" else "अभी कोई गतिविधि दर्ज नहीं है",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "बाइबिल पढ़ने, प्रार्थना निवेदन करने या संदेश देखने पर आपकी गतिविधि यहाँ दिखेगी।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { activity ->
                    ActivityItemRow(
                        item = activity,
                        onClick = { onItemClick(activity) }
                    )
                }
                item {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("गतिविधि इतिहास साफ़ करें?") },
            text = { Text("क्या आप वास्तव में हालिया गतिविधि इतिहास हटाना चाहते हैं? यह क्रिया पूर्ववत नहीं की जा सकती।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    }
                ) {
                    Text("हाँ, साफ़ करें", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun ActivityItemRow(
    item: UserActivityItem,
    onClick: () -> Unit
) {
    val (icon, tintColor, typeLabel) = when (item.type) {
        UserActivityType.BIBLE_READ -> Triple(Icons.Default.MenuBook, MaterialTheme.colorScheme.primary, "बाइबिल पठन")
        UserActivityType.VIDEO_WATCH -> Triple(Icons.Default.PlayCircle, Color(0xFFEF4444), "वीडियो")
        UserActivityType.BLOG_READ -> Triple(Icons.Default.Article, MaterialTheme.colorScheme.secondary, "आलेख / नोटिस")
        UserActivityType.PRAYER_POSTED -> Triple(Icons.Default.VolunteerActivism, GoldWarm, "प्रार्थना निवेदन")
        UserActivityType.TESTIMONY_SHARED -> Triple(Icons.Default.AutoAwesome, Color(0xFF10B981), "गवाही")
        UserActivityType.STUDY_NOTE -> Triple(Icons.Default.EditNote, MaterialTheme.colorScheme.tertiary, "स्टडी नोट्स")
        UserActivityType.SAVED_BOOKMARK -> Triple(Icons.Default.Bookmark, Color(0xFF3B82F6), "बुकमार्क")
        UserActivityType.ALL -> Triple(Icons.Default.Check, MaterialTheme.colorScheme.primary, "गतिविधि")
    }

    val timeFormatted = remember(item.timestamp) {
        val now = System.currentTimeMillis()
        val diff = now - item.timestamp
        when {
            diff < 60_000 -> "अभी-अभी"
            diff < 3600_000 -> "${diff / 60_000} मिनट पहले"
            diff < 86400_000 -> "${diff / 3600_000} घंटे पहले"
            diff < 172800_000 -> "कल"
            else -> {
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                sdf.format(Date(item.timestamp))
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tintColor.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tintColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tintColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}



