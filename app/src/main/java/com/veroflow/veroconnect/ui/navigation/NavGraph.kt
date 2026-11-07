package com.veroflow.veroconnect.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.screens.*

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { it / 8 } }
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                val dest = if (MockRepository.isLoggedIn.value || MockRepository.isGuest.value) Screen.Main.route else Screen.Welcome.route
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLogin = { navController.navigate(Screen.Login.route) },
                onRegister = { navController.navigate(Screen.Register.route) },
                onGuest = {
                    MockRepository.startGuest()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(Screen.Login.route) {
                        // Do not leave the Register screen behind Login.
                        popUpTo(Screen.Welcome.route)
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    MockRepository.isLoggedIn.value = true
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Screen.Register.route) },
                onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Main.route) {
            MainShell(
                onOpenComments = { postId -> navController.navigate(Screen.Comments.build(postId)) },
                onOpenProfile = { userId -> navController.navigate(Screen.Profile.build(userId)) },
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                onOpenChat = { conversationId -> navController.navigate(Screen.Chat.build(conversationId)) },
                onOpenFriends = { navController.navigate(Screen.Friends.route) },
                onOpenGroups = { navController.navigate(Screen.Groups.route) },
                onOpenMediaGallery = { navController.navigate(Screen.MediaGallery.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenAbout = { navController.navigate(Screen.About.route) },
                onOpenEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onCancel = { navController.popBackStack() },
                onPublished = { navController.popBackStack() }
            )
        }

        composable(Screen.Comments.route) { backStackEntry ->
            CommentsScreen(
                postId = backStackEntry.arguments?.getString("postId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: "me"
            ProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onMessage = { targetUserId ->
                    // Opens the existing chat, or starts a new one.
                    val convo = MockRepository.getOrCreateConversation(targetUserId)
                    navController.navigate(Screen.Chat.build(convo.id))
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.Friends.route) {
            FriendsScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId -> navController.navigate(Screen.Profile.build(userId)) }
            )
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(conversationId = conversationId, onBack = { navController.popBackStack() })
        }

        composable(Screen.Groups.route) {
            GroupsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId -> navController.navigate(Screen.Profile.build(userId)) },
                onOpenComments = { postId -> navController.navigate(Screen.Comments.build(postId)) }
            )
        }

        composable(Screen.MediaGallery.route) {
            MediaGalleryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Screen.About.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
