package com.cayboard.ui.settings

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cayboard.ui.common.menuSwipeBack

data class SettingsNavigationRequest(val route: String, val id: Int)

@Composable
fun SettingsScreen(
    initialRoute: String? = null,
    onThemeChanged: () -> Unit = {},
    onWizardBack: () -> Unit = {},
    navigationRequest: SettingsNavigationRequest? = null,
) {
    val navController = rememberNavController()
    val startDestination = if (initialRoute == "manage_dict") SettingsRoutes.Main
    else if (initialRoute == "schema") SettingsRoutes.Schema
    else SettingsRoutes.Main

    // Shortcuts enter the existing settings graph with Main underneath, so Back
    // works exactly as it does when the page is opened from the settings home.
    LaunchedEffect(initialRoute) {
        // 键盘提示「缺手写模型」发的是 model_management，冷启动时也要落到模型页（以前只在设置已打开时才认）。
        val route = if (initialRoute == "model_management") SettingsRoutes.ModelLocal else initialRoute
        // 起始页已处理的几个之外，图里有的页面都可直接进（键盘菜单长按跳到对应设置）；不认识的留在首页。
        if (route != null && route !in setOf(SettingsRoutes.Main, "manage_dict", SettingsRoutes.Schema)) {
            runCatching { navController.navigate(route) { launchSingleTop = true } }
        }
    }

    LaunchedEffect(navigationRequest) {
        val request = navigationRequest ?: return@LaunchedEffect
        val destination = when (request.route) {
            "manage_dict", SettingsRoutes.Dictionary, SettingsRoutes.CellDictionaries, SettingsRoutes.DictContribute -> SettingsRoutes.Main
            "model_management" -> SettingsRoutes.ModelLocal
            else -> request.route
        }
        if (destination == SettingsRoutes.Main) {
            // 冷启动直接进了方案、词库时起始页不是首页：换成首页，不停在那一页。
            if (navController.graph.startDestinationRoute == SettingsRoutes.Main) {
                navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
            } else runCatching {
                navController.navigate(SettingsRoutes.Main) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (navController.currentDestination?.route != destination) {
            runCatching {
                navController.navigate(destination) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }
    
    val motion = rememberSettingsMotionEnabled()
    val entry by navController.currentBackStackEntryAsState()
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val context = androidx.compose.ui.platform.LocalContext.current
    // 说明里的「」指路：亮哪一项和搜索点开一样；已经在那一页就只滚过去高亮，不再压一层。
    val jump: (SettingsSearchEntry) -> Unit = { target ->
        SettingsSearchFocus.open(target)
        val current = navController.currentBackStackEntry
        if (target.target == SEARCH_TARGET_CUSTOM_LAYOUT) {
            context.startActivity(android.content.Intent(context, com.cayboard.CustomLayoutActivity::class.java))
        } else if (!settingsPageMatches(current?.destination?.route, { current?.arguments?.getString(it) }, target.target)) {
            runCatching { navController.navigate(target.target) }
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalSettingsJump provides jump) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.menuSwipeBack(entry, if (back != null && entry != null && entry?.destination?.route != SettingsRoutes.Main) {
            { if (back.hasEnabledCallbacks()) back.onBackPressed() }
        } else null),
        enterTransition = { if (motion) SettingsPageMotion.enter else EnterTransition.None },
        exitTransition = { if (motion) SettingsPageMotion.exit else ExitTransition.None },
        popEnterTransition = { if (motion) SettingsPageMotion.popEnter else EnterTransition.None },
        popExitTransition = { if (motion) SettingsPageMotion.popExit else ExitTransition.None },
    ) {
        composable(SettingsRoutes.Main) {
            androidx.compose.runtime.CompositionLocalProvider(LocalSettingsSearchAnchors provides false) {
                androidx.compose.runtime.CompositionLocalProvider(LocalSettingsShowIcons provides true) {
                SettingsMainContent(
                    onNavigateToHardwareKeyboard = { navController.navigate(SettingsRoutes.HardwareKeyboard) },
                    onNavigateToLanguages = { navController.navigate("languages") },
                    onNavigateToSchema = { navController.navigate(SettingsRoutes.Schema) },
                    onNavigateToLayoutDisplay = { navController.navigate(SettingsRoutes.LayoutDisplay) },
                    onNavigateToModelLocal = { navController.navigate(SettingsRoutes.ModelLocal) },
                    onNavigateToSmartPrediction = { navController.navigate(SettingsRoutes.SmartPrediction) },
                    onNavigateToSpeechToText = { navController.navigate(SettingsRoutes.SpeechToText) },
                    onNavigateToAbout = { navController.navigate(SettingsRoutes.About) },
                    onNavigateToClipboard = { navController.navigate(SettingsRoutes.Clipboard) },
                    onNavigateToBackup = { navController.navigate(SettingsRoutes.Backup) },
                    onNavigateToRoute = { navController.navigate(it) },
                )
                }
            }
        }
        composable(SettingsRoutes.HardwareKeyboard) { CompositionLocalProvider(LocalSettingsShowIcons provides false) { HardwareKeyboardSettingsContent { navController.popBackStack() } } }
        composable("languages") {
            LanguageSettingsContent(onBack = { navController.popBackStack() },
                onOpenDoublePinyin = { navController.navigate(SettingsRoutes.DoublePinyin) },
                onOpenChineseInput = { navController.navigate(SettingsRoutes.ChineseInput) },
                onOpenEnglishInput = { navController.navigate(SettingsRoutes.EnglishInput) })
        }
        composable(SettingsRoutes.ChineseInput) { ChineseInputSettingsContent { navController.popBackStack() } }
        composable(SettingsRoutes.EnglishInput) { EnglishInputSettingsContent { navController.popBackStack() } }
        composable(SettingsRoutes.DoublePinyin) { DoublePinyinSettingsContent { navController.popBackStack() } }
        composable(SettingsRoutes.Schema) {
            val leaveSchema = {
                if (initialRoute == "schema") {
                    navController.navigate(SettingsRoutes.Main) {
                        popUpTo(SettingsRoutes.Main) { inclusive = true }
                    }
                } else navController.popBackStack()
                onWizardBack()
            }
            BackHandler(enabled = initialRoute == "schema", onBack = leaveSchema)
            SchemaSettingsContent(
                onBack = leaveSchema,
                onNavigateToEngineFileBrowser = { navController.navigate(SettingsRoutes.EngineFileBrowser) },
            )
        }
        composable(SettingsRoutes.ModelLocal) {
            ModelLocalContent(
                onBack = { navController.popBackStack() },
            )
        }
        composable("${SettingsRoutes.LayoutDisplay}?page={page}",
            arguments = listOf(navArgument("page") { type = NavType.StringType; nullable = true; defaultValue = null })) { entry ->
            LayoutDisplaySettingsContent(
                onBack = { navController.popBackStack() },
                onOpenLayout = { navController.navigate(it) },
                initialPage = entry.arguments?.getString("page"),
            )
        }
        LayoutInputCategory.entries.forEach { category ->
            composable(category.route) {
                LayoutInputSettingsContent(category, onBack = { navController.popBackStack() },
                    onNavigate = { navController.navigate(it) })
            }
        }
        composable(SettingsRoutes.Translation) {
            TranslationSettingsContent(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.SmartPrediction) {
            SmartPredictionSettingsContent(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.SpeechToText) {
            SpeechToTextSettingsContent(
                onBack = { navController.popBackStack() },

            )
        }
        // Retain only route identities so an upgrade can restore an old navigation stack safely.
        listOf(SettingsRoutes.Dictionary, SettingsRoutes.CellDictionaries, SettingsRoutes.DictContribute).forEach { retired ->
            composable(retired) {
                LaunchedEffect(Unit) {
                    navController.navigate(SettingsRoutes.Main) {
                        popUpTo(retired) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
        composable(SettingsRoutes.EngineFileBrowser) {
            EngineFileBrowserContent(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.Clipboard) {
            ClipboardSettingsContent(
                onBack = { navController.popBackStack() },
            )
        }
        composable(SettingsRoutes.KeyboardResize) {
            KeyboardPanelSettingsContent(com.cayboard.keyboard.KeyboardPanelLaunch.Panel.RESIZE) { navController.popBackStack() }
        }
        composable(SettingsRoutes.ToolbarArrange) {
            KeyboardPanelSettingsContent(com.cayboard.keyboard.KeyboardPanelLaunch.Panel.ARRANGE) { navController.popBackStack() }
        }
        composable(SettingsRoutes.Backup) {
            BackupSettingsContent(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.About) {
            AboutContent(
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(SettingsRoutes.Privacy) },
                onNavigateToLicenses = { navController.navigate(SettingsRoutes.Licenses) },
                onNavigateToLogViewer = { navController.navigate(SettingsRoutes.LogViewer) },
                onNavigateToStorageSpace = { navController.navigate(SettingsRoutes.StorageSpace) }
            )
        }
        composable(SettingsRoutes.StorageSpace) {
            StorageSpaceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToModels = { navController.navigate(SettingsRoutes.ModelLocal) },
            )
        }
        composable(SettingsRoutes.LogViewer) {
            LogViewerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.Handwriting) {
            HandwritingSettingsContent(onBack = { navController.popBackStack() },
                onOpenModels = { navController.navigate(SettingsRoutes.ModelLocal) })
        }
        composable(SettingsRoutes.Privacy) {
            PrivacyPolicyContent(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.Licenses) {
            LicensesContent(
                onBack = { navController.popBackStack() }
            )
        }
    }
    }
}


