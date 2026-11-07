# VeroConnect Demo

A fully offline demo Android app built for **VeroFlow** AI-powered mobile test automation. No backend, no network calls, no auth server — everything runs on local mock data held in memory for the session.

## Getting started

1. Unzip the project and open the root folder in **Android Studio** (Giraffe / 2023.1+ or newer).
2. Let Gradle sync. On first sync Android Studio will fetch the Gradle distribution declared in
   `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.7) and download `gradle-wrapper.jar`
   automatically if it's missing — this repo ships the wrapper scripts (`gradlew`, `gradlew.bat`)
   but not the binary jar, since it can't be committed from this environment. If sync doesn't
   auto-fetch it, use **File → Sync Project with Gradle Files**, or **View → Tool Windows → Gradle →
   Refresh**, and Android Studio will regenerate it.
3. Run the `app` configuration on an emulator or device with **minSdk 26** or higher.

No API keys, accounts, or internet connection are required to run or test the app.

## What's implemented

All 19 screens from the spec, plus a Forgot Password screen and a Logout confirmation dialog:

Splash → Welcome → Register → Login → Home Feed → Create Post → Comments → User Profile →
Edit Profile → Friends → Chat List → Chat → Groups → Notifications → Search → Media Gallery →
Settings → About

Navigation styles: Bottom Navigation (Home / Chats / Alerts / Profile), a Navigation Drawer
(Friends, Groups, Media Gallery, Settings, About), Tabs (Posts/Media on Profile), dialog
navigation (Logout, Report post, Create group, permission prompts), and standard back-stack
navigation via Navigation Compose.

Mock data lives entirely in `data/MockRepository.kt`: 40 users, 120+ posts, stories, group,
chat, and notification data, all generated in memory — nothing is persisted or fetched.

## Project structure

```
app/src/main/java/com/veroflow/veroconnect/
  MainActivity.kt              # entry point
  data/
    Models.kt                  # data classes
    MockRepository.kt          # single in-memory source of truth
  ui/
    theme/                     # Material 3 color/typography/theme
    navigation/
      Screen.kt                # route definitions
      NavGraph.kt               # root NavHost
      MainShell.kt              # bottom nav + drawer shell
    components/                # shared composables (Avatar, MediaPlaceholder, EmptyState...)
    screens/                   # one file per screen
```

## Testing hooks for VeroFlow

Every interactive element (buttons, fields, checkboxes, switches, list items, cards) carries a
stable `Modifier.testTag("...")` so an automated agent can target elements reliably without
relying on visible text or layout position. Examples:

- Auth: `field_email`, `field_password`, `btn_login_submit`, `checkbox_remember_me`, `link_register`
- Feed: `post_{id}`, `btn_like_{id}`, `btn_comment_{id}`, `btn_share_{id}`, `btn_save_{id}`,
  `btn_pull_to_refresh`, `fab_create_post`
- Create Post: `field_post_text`, `btn_camera`, `btn_gallery`, `btn_attach_file`, `btn_publish_post`
- Comments: `field_comment`, `btn_send_comment`, `btn_like_comment_{id}`, `menu_edit_{id}`, `menu_delete_{id}`
- Profile: `btn_follow_toggle`, `btn_edit_profile`, `tab_posts`, `tab_media`
- Friends: `btn_accept_{id}`, `btn_reject_{id}`, `btn_add_friend_{id}`
- Chats: `chat_{id}` (supports long-press), `field_message`, `btn_send_message`, `btn_voice_note`
- Groups: `btn_join_{id}`, `btn_leave_{id}`, `fab_create_group`
- Notifications: `notification_{id}`, `btn_clear_all`
- Search: `field_search`, `filter_ALL` / `filter_USERS` / `filter_POSTS` / `filter_GROUPS`
- Media Gallery: `media_{id}`, `pager_media_viewer`, `btn_delete_media`
- Settings: `switch_dark_mode`, `switch_notifications`, `switch_location`, `switch_autoplay`,
  `row_language`, `row_logout`, `btn_logout_yes`
- Navigation: `bottom_navigation`, `tab_home` / `tab_chats` / `tab_notifications` / `tab_profile`,
  `navigation_drawer`, `drawer_friends`, `drawer_groups`, `drawer_settings`

## Notes

- This project was assembled and reviewed outside of Android Studio (no Android SDK / Google
  Maven access in that environment), so a first build inside Android Studio is the real
  correctness check. If you hit a build error, it's most likely a dependency version mismatch —
  the Compose BOM, Kotlin, and AGP versions in `build.gradle.kts` / `app/build.gradle.kts` can be
  bumped to whatever your installed Android Studio's template defaults to.
- `minSdk` is 26 (Android 8.0) — adjust in `app/build.gradle.kts` if your target devices need
  wider coverage.

---

## v1.1 changes (UI / UX update)

**Layout fixes**
- Bottom content no longer hides behind the system navigation bar or the keyboard
  (Welcome → "Continue as Guest", Chat message box, Comment box, Create Post, Edit Profile).
- Login / Register / Edit Profile / Settings scroll on small screens and with the keyboard open.
- Profile is one scrolling page (header + tabs + pictures) instead of a cramped fixed layout.
- No more double top/bottom padding inside the bottom-navigation tabs.

**Real pictures**
- Profile photos (randomuser.me) and post / video / group / story / gallery images (picsum.photos),
  fixed per item so the same picture always shows for the same user or post.
- Needs internet (permission added). While loading: shimmer; if offline: the old colour box / initials.
- Videos show a play button and duration. Coil is used for loading + caching.

**Fixes**
- Likes, saves, comment likes, unread badges, join/leave group, friend accept/remove and notification
  read state now update on screen immediately (models are now Compose-state backed).
- "Message" on a profile opens the chat (creates one if needed). Edit Profile now really saves.
- Groups screen has a back button. Search results open (posts → comments, users → profile).
- Selected tab is kept when you come back; Back from a tab goes to Home first.
- Publishing a post no longer waits ~4 s for a snackbar; messages appear on the next screen.
- Settings, profile and "Remember me" login are remembered after the app is closed.

**New**
- Chat: bubbles with timestamps, avatars, "typing…" indicator + simulated reply, auto-scroll, new chats move to top, mute.
- Stories: gradient ring, "Your story" bubble, full-screen story viewer.
- Pull-to-refresh on the feed (the refresh icon still works), double-tap a photo to like.
- Edit Profile: choose a profile picture. Screen transitions, animated splash, new welcome screen.

**For VeroFlow tests:** all test tags are kept. Because settings and "Remember me" are now saved, clear the
app data between runs (`adb shell pm clear com.veroflow.veroconnect`) if you want a fresh start.

---

## v1.2 - built-in backend (nothing to install or run)

The app now has a real backend **inside the APK**: a SQLite database file (`veroconnect.db`) that Android creates
automatically on first launch. Code is in `app/src/main/java/com/veroflow/veroconnect/backend/`.

| File | Job |
|---|---|
| `VeroDatabase.kt` | Creates the database and its tables (accounts, posts, comments, messages, groups, flags) |
| `AuthService.kt` | Register, login, update profile, change password. Passwords are salted PBKDF2 hashes, never plain text |
| `DataService.kt` | Saves everything an account does and restores it at the next login |
| `Backend.kt` | Entry point: `Backend.auth` and `Backend.data` (screens never touch SQL) |

**Accounts are real now**
- Register saves the account. Login only works for a registered email with the right password.
- Errors: "No account found...", "Incorrect password...", "An account with this email already exists", "This username is already taken".
- Each account keeps its own posts, comments, likes, saves, messages, friends, follows, groups, notifications and profile.
- "Remember me" keeps you signed in after the app is closed. Logout signs you out. Guests are not saved.
- Settings: change password (real), Clear all data (deletes the database), account shown at the top.

**Demo account (created automatically):** `alex.morgan@example.com` / `Password123`

**For VeroFlow tests**
- Test flow "register, then login": register a new user, then log in with the same email + password.
- Reset between runs: Settings > Clear all data, or `adb shell pm clear com.veroflow.veroconnect`.
- Not saved (on purpose): other people's demo data is regenerated identically on every launch.
