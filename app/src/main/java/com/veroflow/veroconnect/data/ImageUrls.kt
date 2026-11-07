package com.veroflow.veroconnect.data

import kotlin.math.abs

/**
 * Deterministic remote image URLs, so the same user/post always shows the same picture.
 * Portraits: randomuser.me (free). Scenery / video frames: picsum.photos (free).
 * If an image cannot be loaded (offline), the UI falls back to a coloured box / initials.
 */
object ImageUrls {

    private val femaleNames = setOf(
        "Ava", "Noor", "Emma", "Sara", "Zara", "Maya",
        "Layla", "Nina", "Hana", "Leila", "Amber", "Sofia"
    )

    fun portrait(female: Boolean, n: Int): String =
        "https://randomuser.me/api/portraits/${if (female) "women" else "men"}/$n.jpg"

    fun avatar(userId: String, firstName: String): String {
        val female = firstName in femaleNames
        val idx = userId.removePrefix("u").toIntOrNull() ?: abs(userId.hashCode() % 90)
        return portrait(female, (idx * 7) % 90 + 1)
    }

    /** Photos the current user can pick as their profile picture. */
    val myAvatarOptions: List<String> = listOf(
        portrait(false, 32), portrait(true, 44), portrait(false, 75), portrait(true, 12),
        portrait(false, 5), portrait(true, 68), portrait(false, 51), portrait(true, 29)
    )

    fun cover(userId: String): String = "https://picsum.photos/seed/vc-cover-$userId/900/400"

    fun post(postId: String, video: Boolean): String =
        if (video) "https://picsum.photos/seed/vc-v-$postId/800/450"
        else "https://picsum.photos/seed/vc-$postId/800/600"

    fun videoDuration(id: String): String {
        val s = 12 + abs(id.hashCode() % 108)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    fun group(groupId: String): String = "https://picsum.photos/seed/vc-g-$groupId/200/200"

    fun media(mediaId: String): String = "https://picsum.photos/seed/vc-m-$mediaId/500/500"

    fun story(storyId: String): String = "https://picsum.photos/seed/vc-s-$storyId/540/960"

    /** A fresh random picture, used when the user "takes a photo" or picks one from the gallery. */
    fun draft(): String = "https://picsum.photos/seed/vc-d-${System.nanoTime()}/800/600"
}
