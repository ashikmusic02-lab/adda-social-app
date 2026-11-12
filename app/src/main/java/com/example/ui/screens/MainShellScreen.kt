package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfflineStateBanner
import com.example.ui.theme.SuccessGreen

enum class MainNavTab(
  val tag: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  HOME("nav_tab_home", Icons.Filled.Home, Icons.Outlined.Home),
  EXPLORE("nav_tab_explore", Icons.Filled.Explore, Icons.Outlined.Explore),
  CREATE("nav_tab_create", Icons.Filled.AddBox, Icons.Outlined.AddBox),
  NOTIFICATIONS("nav_tab_notifications", Icons.Filled.Notifications, Icons.Outlined.Notifications),
  PROFILE("nav_tab_profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
  profile: UserProfile,
  isOnline: Boolean,
  isEmailVerified: Boolean,
  currentLanguage: AppLanguage,
  currentThemeMode: AppThemeMode,
  statusBannerMessage: String?,
  onClearStatusBanner: () -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onThemeChange: (AppThemeMode) -> Unit,
  onUpdateProfile: (displayName: String, bio: String, onDone: () -> Unit) -> Unit,
  onRefreshEmailVerified: () -> Unit,
  onSendVerificationEmail: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val strings = LocalAppStrings.current
  var selectedTab by rememberSaveable { mutableStateOf(MainNavTab.PROFILE) }
  var showArchitectureSheet by rememberSaveable { mutableStateOf(false) }

  // BackHandler returns to HOME tab when on secondary tabs, or closes the guide sheet
  BackHandler(enabled = showArchitectureSheet || selectedTab != MainNavTab.HOME) {
    if (showArchitectureSheet) {
      showArchitectureSheet = false
    } else {
      selectedTab = MainNavTab.HOME
    }
  }

  if (showArchitectureSheet) {
    SetupAndArchitectureScreen(
      onBack = { showArchitectureSheet = false }
    )
    return
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = strings.appName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("main_bottom_navigation")
      ) {
        MainNavTab.entries.forEach { tab ->
          val label = when (tab) {
            MainNavTab.HOME -> strings.navHome
            MainNavTab.EXPLORE -> strings.navExplore
            MainNavTab.CREATE -> strings.navCreate
            MainNavTab.NOTIFICATIONS -> strings.navNotifications
            MainNavTab.PROFILE -> strings.navProfile
          }
          val isSelected = selectedTab == tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTab = tab },
            icon = {
              Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = label
              )
            },
            label = { Text(label) },
            modifier = Modifier.testTag(tab.tag)
          )
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      OfflineStateBanner(isOnline = isOnline)

      if (statusBannerMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = statusBannerMessage,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClearStatusBanner) {
              Text("OK")
            }
          }
        }
      }

      when (selectedTab) {
        MainNavTab.HOME -> ComingSoonTabContent(
          title = strings.homePhaseInfoTitle,
          description = strings.homePhaseInfoDesc,
          badge = strings.comingSoonBadge,
          onGoToProfile = { selectedTab = MainNavTab.PROFILE },
          onOpenGuide = { showArchitectureSheet = true }
        )

        MainNavTab.EXPLORE -> ComingSoonTabContent(
          title = strings.explorePhaseInfoTitle,
          description = strings.explorePhaseInfoDesc,
          badge = strings.comingSoonBadge,
          onGoToProfile = { selectedTab = MainNavTab.PROFILE },
          onOpenGuide = { showArchitectureSheet = true }
        )

        MainNavTab.CREATE -> ComingSoonTabContent(
          title = strings.createPhaseInfoTitle,
          description = strings.createPhaseInfoDesc,
          badge = strings.comingSoonBadge,
          onGoToProfile = { selectedTab = MainNavTab.PROFILE },
          onOpenGuide = { showArchitectureSheet = true }
        )

        MainNavTab.NOTIFICATIONS -> ComingSoonTabContent(
          title = strings.notificationsPhaseInfoTitle,
          description = strings.notificationsPhaseInfoDesc,
          badge = strings.comingSoonBadge,
          onGoToProfile = { selectedTab = MainNavTab.PROFILE },
          onOpenGuide = { showArchitectureSheet = true }
        )

        MainNavTab.PROFILE -> ProfileAndSettingsContent(
          profile = profile,
          isOnline = isOnline,
          isEmailVerified = isEmailVerified,
          currentLanguage = currentLanguage,
          currentThemeMode = currentThemeMode,
          onLanguageChange = onLanguageChange,
          onThemeChange = onThemeChange,
          onUpdateProfile = onUpdateProfile,
          onRefreshEmailVerified = onRefreshEmailVerified,
          onSendVerificationEmail = onSendVerificationEmail,
          onOpenArchitectureSheet = { showArchitectureSheet = true },
          onSignOut = onSignOut
        )
      }
    }
  }
}

@Composable
private fun ComingSoonTabContent(
  title: String,
  description: String,
  badge: String,
  onGoToProfile: () -> Unit,
  onOpenGuide: () -> Unit
) {
  val strings = LocalAppStrings.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 540.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      EmptyStateCard(
        title = title,
        subtitle = description,
        badgeText = badge
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = strings.comingSoonSubtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(20.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = onGoToProfile,
          modifier = Modifier.testTag("coming_soon_to_profile_button")
        ) {
          Text(strings.navProfile)
        }
        OutlinedButton(
          onClick = onOpenGuide,
          modifier = Modifier.testTag("coming_soon_open_guide_button")
        ) {
          Text(strings.architectureBlueprintBtn)
        }
      }
    }
  }
}

@Composable
private fun ProfileAndSettingsContent(
  profile: UserProfile,
  isOnline: Boolean,
  isEmailVerified: Boolean,
  currentLanguage: AppLanguage,
  currentThemeMode: AppThemeMode,
  onLanguageChange: (AppLanguage) -> Unit,
  onThemeChange: (AppThemeMode) -> Unit,
  onUpdateProfile: (displayName: String, bio: String, onDone: () -> Unit) -> Unit,
  onRefreshEmailVerified: () -> Unit,
  onSendVerificationEmail: () -> Unit,
  onOpenArchitectureSheet: () -> Unit,
  onSignOut: () -> Unit
) {
  val strings = LocalAppStrings.current
  var showEditDialog by rememberSaveable { mutableStateOf(false) }
  var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

  if (showEditDialog) {
    EditProfileDialog(
      initialDisplayName = profile.displayName,
      initialBio = profile.bio.orEmpty(),
      onDismiss = { showEditDialog = false },
      onSave = { newName, newBio ->
        onUpdateProfile(newName, newBio) {
          showEditDialog = false
        }
      }
    )
  }

  if (showSignOutDialog) {
    AlertDialog(
      onDismissRequest = { showSignOutDialog = false },
      title = { Text(strings.signOutConfirmTitle) },
      text = { Text(strings.signOutConfirmBody) },
      confirmButton = {
        Button(
          onClick = {
            showSignOutDialog = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
          ),
          modifier = Modifier.testTag("confirm_sign_out_button")
        ) {
          Text(strings.signOutBtn)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutDialog = false }) {
          Text(strings.cancelBtn)
        }
      }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp)
      .testTag("profile_settings_screen"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // User Identity Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = profile.displayName.take(1).uppercase().ifBlank { "A" },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = profile.displayName,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold
                )
                if (profile.isVerified) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
              )
              if (profile.email.isNotBlank()) {
                Text(
                  text = profile.email,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            OutlinedButton(
              onClick = { showEditDialog = true },
              enabled = isOnline,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.testTag("edit_profile_button")
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = strings.editProfileBtn,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.editProfileBtn)
            }
          }

          if (!profile.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = profile.bio,
              style = MaterialTheme.typography.bodyLarge
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(14.dp))

          // Read-Only Denormalized Counters (Protected by firestore.rules)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            CounterColumn(count = profile.postsCount, label = strings.postsLabel)
            CounterColumn(count = profile.followersCount, label = strings.followersLabel)
            CounterColumn(count = profile.followingCount, label = strings.followingLabel)
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Email Verification Status Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(50),
              color = if (isEmailVerified) {
                SuccessGreen.copy(alpha = 0.16f)
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              }
            ) {
              Text(
                text = if (isEmailVerified) strings.verifiedEmailBadge else strings.unverifiedEmailBadge,
                style = MaterialTheme.typography.labelLarge,
                color = if (isEmailVerified) SuccessGreen else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }

            if (!isEmailVerified) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                  onClick = onSendVerificationEmail,
                  enabled = isOnline,
                  modifier = Modifier.testTag("send_verification_email_button")
                ) {
                  Text(strings.verifyEmailBtn)
                }
                TextButton(
                  onClick = onRefreshEmailVerified,
                  enabled = isOnline,
                  modifier = Modifier.testTag("refresh_email_verified_button")
                ) {
                  Text(strings.refreshStatusBtn)
                }
              }
            }
          }
        }
      }

      // Phase 1 Settings Card (Strictly Language, Theme & Logout per specification)
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = strings.sessionActiveLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Language Selector
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.languageSettingLabel,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.entries.forEach { lang ->
              FilterChip(
                selected = currentLanguage == lang,
                onClick = { onLanguageChange(lang) },
                label = { Text(lang.nativeLabel) },
                modifier = Modifier.testTag("settings_lang_${lang.code}")
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Theme Selector
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DarkMode,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.themeSettingLabel,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = currentThemeMode == AppThemeMode.SYSTEM,
              onClick = { onThemeChange(AppThemeMode.SYSTEM) },
              label = { Text(strings.themeSystem) },
              modifier = Modifier.testTag("settings_theme_system")
            )
            FilterChip(
              selected = currentThemeMode == AppThemeMode.LIGHT,
              onClick = { onThemeChange(AppThemeMode.LIGHT) },
              label = { Text(strings.themeLight) },
              modifier = Modifier.testTag("settings_theme_light")
            )
            FilterChip(
              selected = currentThemeMode == AppThemeMode.DARK,
              onClick = { onThemeChange(AppThemeMode.DARK) },
              label = { Text(strings.themeDark) },
              modifier = Modifier.testTag("settings_theme_dark")
            )
          }

          Spacer(modifier = Modifier.height(20.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(16.dp))

          OutlinedButton(
            onClick = onOpenArchitectureSheet,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("profile_open_guide_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.MenuBook,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.architectureBlueprintBtn)
          }

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { showSignOutDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("settings_sign_out_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.signOutBtn,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CounterColumn(
  count: Long,
  label: String
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun EditProfileDialog(
  initialDisplayName: String,
  initialBio: String,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit
) {
  val strings = LocalAppStrings.current
  var name by rememberSaveable { mutableStateOf(initialDisplayName) }
  var bio by rememberSaveable { mutableStateOf(initialBio) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.editProfileBtn) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { if (it.length <= 60) name = it },
          label = { Text(strings.displayNameLabel) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_name_input")
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { if (it.length <= 160) bio = it },
          label = { Text(strings.bioLabel) },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_bio_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, bio) },
        enabled = name.trim().isNotEmpty(),
        modifier = Modifier.testTag("save_profile_changes_button")
      ) {
        Text(strings.saveChangesBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancelBtn)
      }
    }
  )
}
