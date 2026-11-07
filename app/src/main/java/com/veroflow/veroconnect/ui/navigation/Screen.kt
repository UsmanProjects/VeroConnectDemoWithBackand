package com.veroflow.veroconnect.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Register : Screen("register")
    data object Login : Screen("login")
    data object ForgotPassword : Screen("forgot_password")

    // Main shell (bottom navigation host)
    data object Main : Screen("main")

    data object HomeFeed : Screen("home_feed")
    data object CreatePost : Screen("create_post")
    data object Comments : Screen("comments/{postId}") {
        fun build(postId: String) = "comments/$postId"
    }
    data object Profile : Screen("profile/{userId}") {
        fun build(userId: String) = "profile/$userId"
    }
    data object EditProfile : Screen("edit_profile")
    data object Friends : Screen("friends")
    data object ChatList : Screen("chat_list")
    data object Chat : Screen("chat/{conversationId}") {
        fun build(conversationId: String) = "chat/$conversationId"
    }
    data object Groups : Screen("groups")
    data object Notifications : Screen("notifications")
    data object Search : Screen("search")
    data object MediaGallery : Screen("media_gallery")
    data object Settings : Screen("settings")
    data object About : Screen("about")
}
