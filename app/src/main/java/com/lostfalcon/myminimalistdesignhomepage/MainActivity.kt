package com.lostfalcon.myminimalistdesignhomepage

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lostfalcon.myminimalistdesignhomepage.ui.theme.MyMinimalistDesignHomePageTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class LauncherApp(
    val label: String,
    val packageName: String,
    val componentName: ComponentName? = null,
    val user: UserHandle? = null,
    val id: String = packageName,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyMinimalistDesignHomePageTheme {
                MinimalLauncher()
            }
        }
    }
}

@Composable
private fun MinimalLauncher() {
    val context = LocalContext.current
    val preferences = remember(context) {
        LauncherPreferences(context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE))
    }
    var allApps by remember(context) { mutableStateOf(loadLaunchableApps(context)) }
    var favoriteAppIds by remember(preferences) {
        mutableStateOf(preferences.favoriteAppIds(allApps))
    }
    var hiddenAppIds by remember(preferences) { mutableStateOf(preferences.hiddenAppIds()) }
    var appLabels by remember(preferences) { mutableStateOf(preferences.appLabels(allApps)) }
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    var renameAppId by remember { mutableStateOf<String?>(null) }
    val displayedApps = remember(allApps, appLabels) { applyAppLabels(allApps, appLabels) }
    val favoriteApps = displayedApps.filter { it.id in favoriteAppIds && it.id !in hiddenAppIds }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allApps = loadLaunchableApps(context)
                favoriteAppIds = preferences.favoriteAppIds(allApps)
                hiddenAppIds = preferences.hiddenAppIds()
                appLabels = preferences.appLabels(allApps)
                selectedAppId = selectedAppId?.takeIf { id -> allApps.any { it.id == id } }
                renameAppId = renameAppId?.takeIf { id -> allApps.any { it.id == id } }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                favoriteApps = favoriteApps,
                onAppClick = { launchApp(context, it) },
                onAppLongClick = { selectedAppId = it.id },
                onSearchRequested = { launchWebSearch(context) },
                onPhoneClick = {
                    launchIntent(context, Intent(Intent.ACTION_DIAL))
                },
                onCameraClick = {
                    launchIntent(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                },
            )

            else -> AppsScreen(
                apps = displayedApps,
                isActive = pagerState.settledPage == page,
                favoriteAppIds = favoriteAppIds,
                hiddenAppIds = hiddenAppIds,
                onAppClick = { launchApp(context, it) },
                onAppLongClick = { selectedAppId = it.id },
            )
        }
    }

    displayedApps.firstOrNull { it.id == selectedAppId }?.let { app ->
        AppActionsDialog(
            app = app,
            isFavorite = app.id in favoriteAppIds,
            isHidden = app.id in hiddenAppIds,
            onDismiss = { selectedAppId = null },
            onFavoriteToggle = {
                favoriteAppIds = preferences.toggleFavorite(favoriteAppIds, app.id)
                selectedAppId = null
            },
            onRename = {
                renameAppId = app.id
                selectedAppId = null
            },
            onHiddenToggle = {
                hiddenAppIds = preferences.toggleHidden(hiddenAppIds, app.id)
                selectedAppId = null
            },
            onAppInfo = {
                selectedAppId = null
                openAppInfo(context, app)
            },
            onUninstall = {
                selectedAppId = null
                if (!launchIntent(context, uninstallIntent(app))) {
                    showActionUnavailable(context)
                }
            },
        )
    }
    allApps.firstOrNull { it.id == renameAppId }?.let { originalApp ->
        RenameAppDialog(
            originalLabel = originalApp.label,
            currentLabel = appLabels[originalApp.id] ?: originalApp.label,
            onDismiss = { renameAppId = null },
            onSave = { label ->
                preferences.renameApp(originalApp.id, label)
                appLabels = preferences.appLabels(allApps)
                renameAppId = null
            },
        )
    }
}

@Composable
internal fun HomeScreen(
    favoriteApps: List<LauncherApp>,
    onAppClick: (LauncherApp) -> Unit,
    onAppLongClick: (LauncherApp) -> Unit,
    onSearchRequested: () -> Unit,
    onPhoneClick: () -> Unit,
    onCameraClick: () -> Unit,
) {
    val currentSearchRequested by rememberUpdatedState(onSearchRequested)
    val searchActionLabel = stringResource(R.string.open_web_search)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("home-screen")
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(searchActionLabel) {
                        currentSearchRequested()
                        true
                    },
                )
            }
            .pointerInput(Unit) {
                val threshold = 80.dp.toPx()
                var dragDistance = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onDragCancel = { dragDistance = 0f },
                    onDragEnd = {
                        if (dragDistance <= -threshold) currentSearchRequested()
                        dragDistance = 0f
                    },
                    onVerticalDrag = { _, distance -> dragDistance += distance },
                )
            }
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Clock(
            modifier = Modifier.align(Alignment.TopStart),
        )

        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            favoriteApps.forEach { app ->
                AppWord(
                    app = app,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) },
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ActionWord(text = "PHONE", onClick = onPhoneClick)
            ActionWord(text = "CAMERA", onClick = onCameraClick)
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000)
        }
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    }

    Column(modifier = modifier) {
        Text(
            text = now.format(timeFormatter),
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
        )
        Text(
            text = now.format(dateFormatter).uppercase(Locale.getDefault()),
            color = Color(0xFF777777),
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppWord(
    app: LauncherApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Text(
        text = app.label,
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.2.sp,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

@Composable
private fun ActionWord(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.8.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppsScreen(
    apps: List<LauncherApp>,
    isActive: Boolean,
    favoriteAppIds: Set<String>,
    hiddenAppIds: Set<String>,
    onAppClick: (LauncherApp) -> Unit,
    onAppLongClick: (LauncherApp) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showHiddenApps by remember { mutableStateOf(false) }
    val visibleApps = remember(apps, query, hiddenAppIds, showHiddenApps) {
        filterApps(apps.filter { (it.id in hiddenAppIds) == showHiddenApps }, query)
    }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isActive) {
        if (isActive) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .imePadding()
            .padding(top = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(if (showHiddenApps) R.string.hidden_apps else R.string.all_apps),
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                showHiddenApps = !showHiddenApps
                query = ""
            }) {
                Text(stringResource(if (showHiddenApps) R.string.show_all_apps else R.string.show_hidden_apps))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "SEARCH APPS",
                    color = Color(0xFF666666),
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp,
                )
            }
            val searchDescription = stringResource(R.string.search_apps)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    letterSpacing = 1.2.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = searchDescription }
                    .focusRequester(searchFocusRequester),
            )
        }
        HorizontalDivider(
            color = Color(0xFF282828),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            text = stringResource(R.string.long_press_app_hint),
            color = Color(0xFF555555),
            fontSize = 9.sp,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        if (visibleApps.isEmpty()) {
            Text(
                text = stringResource(if (showHiddenApps) R.string.no_hidden_apps else R.string.no_matching_apps),
                color = Color(0xFF777777),
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = visibleApps,
                key = { it.id },
            ) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onAppClick(app) },
                            onLongClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onAppLongClick(app)
                            },
                        )
                        .padding(horizontal = 24.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.weight(1f),
                    )
                    if (app.id in favoriteAppIds) {
                        Text(
                            text = "HOME",
                            color = Color(0xFF707070),
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppActionsDialog(
    app: LauncherApp,
    isFavorite: Boolean,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onRename: () -> Unit,
    onHiddenToggle: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column {
                AppMenuAction(
                    text = stringResource(if (isFavorite) R.string.remove_from_home else R.string.add_to_home),
                    onClick = onFavoriteToggle,
                )
                AppMenuAction(stringResource(R.string.rename_app), onRename)
                AppMenuAction(
                    text = stringResource(if (isHidden) R.string.unhide_app else R.string.hide_app),
                    onClick = onHiddenToggle,
                )
                AppMenuAction(stringResource(R.string.app_info), onAppInfo)
                AppMenuAction(stringResource(R.string.uninstall_app), onUninstall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun AppMenuAction(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun RenameAppDialog(
    originalLabel: String,
    currentLabel: String,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var label by remember(currentLabel) { mutableStateOf(currentLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_app)) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.app_name_label)) },
                    singleLine = true,
                )
                Text(
                    text = stringResource(R.string.original_app_name, originalLabel),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TextButton(onClick = { onSave(null) }) {
                    Text(stringResource(R.string.restore_original_name))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label.trim()) },
                enabled = label.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

internal fun filterApps(
    apps: List<LauncherApp>,
    query: String,
): List<LauncherApp> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return apps
    return apps.filter {
        it.label.contains(normalizedQuery, ignoreCase = true)
    }
}

private fun loadLaunchableApps(context: Context): List<LauncherApp> {
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    val userManager = context.getSystemService(UserManager::class.java)
    val personalProfile = Process.myUserHandle()

    return launcherApps.profiles
        .flatMap { user ->
            val profileSerial = userManager.getSerialNumberForUser(user)
            val isWorkProfile = user != personalProfile
            launcherApps.getActivityList(null, user).map { activity ->
                val baseLabel = activity.label.toString()
                LauncherApp(
                    label = if (isWorkProfile) "$baseLabel (WORK)" else baseLabel,
                    packageName = activity.applicationInfo.packageName,
                    componentName = activity.componentName,
                    user = user,
                    id = "$profileSerial:${activity.applicationInfo.packageName}",
                )
            }
        }
        .asSequence()
        .filter { it.packageName != context.packageName }
        .distinctBy { it.id }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
        .toList()
}

private fun launchApp(context: Context, app: LauncherApp) {
    try {
        if (app.componentName != null && app.user != null) {
            context.getSystemService(LauncherApps::class.java).startMainActivity(
                app.componentName,
                app.user,
                null,
                null,
            )
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent == null || !launchIntent(context, intent)) showActionUnavailable(context)
    } catch (_: ActivityNotFoundException) {
        showActionUnavailable(context)
    } catch (_: SecurityException) {
        showActionUnavailable(context)
    }
}

private fun openAppInfo(context: Context, app: LauncherApp) {
    if (app.componentName != null && app.user != null) {
        try {
            context.getSystemService(LauncherApps::class.java).startAppDetailsActivity(
                app.componentName,
                app.user,
                null,
                null,
            )
            return
        } catch (_: ActivityNotFoundException) {
            // Only fall back to package settings for this profile.
        } catch (_: SecurityException) {
            // A work-profile policy may deny the request.
        }
    }
    if (app.user != null && app.user != Process.myUserHandle()) {
        showActionUnavailable(context)
        return
    }
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", app.packageName, null),
    )
    if (!launchIntent(context, intent)) showActionUnavailable(context)
}

internal fun uninstallIntent(app: LauncherApp): Intent =
    Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)).apply {
        app.user?.let { putExtra("android.intent.extra.USER", it) }
    }

internal fun launchWebSearch(
    context: Context,
    launch: (Intent) -> Boolean = { launchIntent(context, it) },
) {
    val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, "")
    if (!launch(search) && !launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))) {
        showActionUnavailable(context)
    }
}

private fun launchIntent(context: Context, intent: Intent): Boolean =
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

private fun showActionUnavailable(context: Context) {
    Toast.makeText(context, R.string.app_action_unavailable, Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenPreview() {
    MyMinimalistDesignHomePageTheme {
        HomeScreen(
            favoriteApps = listOf(
                LauncherApp("Calendar", "calendar"),
                LauncherApp("Notion", "notion"),
                LauncherApp("GPay", "gpay"),
                LauncherApp("Maps", "maps"),
            ),
            onAppClick = {},
            onAppLongClick = {},
            onSearchRequested = {},
            onPhoneClick = {},
            onCameraClick = {},
        )
    }
}
