package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.example.data.theme.HavenThemePreferencesRepository
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.FeedOfferCard
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.model.ProfileUiState
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode

/** Native profile from authenticated backend data, with persisted appearance settings. */
@Composable
fun ProfileScreen(
  state: ProfileUiState,
  onRetry: () -> Unit,
  onOpenWeb: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenMyOffers: (() -> Unit)? = null,
  onOpenSavedOffers: (() -> Unit)? = null,
  onOpenFollowing: (() -> Unit)? = null,
  onOpenOffer: ((String) -> Unit)? = null,
  onOpenPublicProfile: ((String) -> Unit)? = null,
  onOpenEditProfile: (() -> Unit)? = null
) {
  val context = LocalContext.current
  var appearance by remember { mutableStateOf(false) }
  var confirmSignOut by remember { mutableStateOf(false) }
  val profile = state.profile
  Column(modifier.fillMaxSize().background(HavenColors.Canvas).testTag("profile_screen")) {
    HavenTopBarContainer {
      Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("My profile", fontFamily = InterFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        TextButton(enabled = profile != null, onClick = {
          profile?.let { onOpenPublicProfile?.invoke(it.handle) ?: onOpenWeb("/profiles/${it.handle}") }
        }) {
          Text("Public view", color = HavenColors.TextSecondary)
        }
      }
    }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
      if (state.loading) CircularProgressIndicator(Modifier.padding(24.dp).testTag("profile_loading"), color = HavenColors.Primary)
      state.error?.let { error ->
        Text(error, modifier = Modifier.testTag("profile_error"))
        TextButton(onClick = onRetry) { Text("Try again") }
      }
      if (profile != null) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(96.dp).clip(CircleShape).background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, CircleShape), contentAlignment = Alignment.Center) {
          val initial: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(profile.displayName.take(1).uppercase(), fontSize = 32.sp, color = HavenColors.TextPrimary)
            }
          }
          SubcomposeAsyncImage(model = profile.avatarUrl, contentDescription = profile.displayName,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, loading = { initial() }, error = { initial() })
        }
        Spacer(Modifier.height(16.dp))
        Text(profile.displayName, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
          fontSize = 22.sp, textAlign = TextAlign.Center, color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("profile_display_name"))
        profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
          Text(bio, fontFamily = InterFontFamily, fontSize = 16.sp, lineHeight = 24.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp).testTag("profile_bio"))
        }
        TextButton(onClick = { onOpenEditProfile?.invoke() ?: onOpenWeb("/profile/edit") }, modifier = Modifier.padding(top = 16.dp)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(24.dp)).testTag("profile_edit_button")) {
          Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
          Text("Edit profile", modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))) {
          ProfileRow("My offers", "${profile.offerCount} ${if (profile.offerCount == 1L) "offer" else "offers"}", Icons.Outlined.GridView) {
            onOpenMyOffers?.invoke() ?: onOpenWeb("/profile/offers")
          }
          HorizontalDivider(color = HavenColors.BorderSubtle)
          ProfileRow("Saved offers", "${profile.savedCount} saved", Icons.Outlined.FavoriteBorder) {
            onOpenSavedOffers?.invoke() ?: onOpenWeb("/profile/saved")
          }
          HorizontalDivider(color = HavenColors.BorderSubtle)
          ProfileRow("Following", "${profile.followingCount} ${if (profile.followingCount == 1L) "person" else "people"}", Icons.Outlined.Group) {
            onOpenFollowing?.invoke() ?: onOpenWeb("/profile/following")
          }
          HorizontalDivider(color = HavenColors.BorderSubtle)
          ProfileRow("Appearance", ThemeManager.themeMode.title, Icons.Outlined.Palette,
            tag = "profile_link_appearance") { appearance = true }
        }
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
          Text("Your offers", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
          TextButton(onClick = { onOpenMyOffers?.invoke() ?: onOpenWeb("/profile/offers") }) { Text("Manage") }
        }
        if (profile.offers.isEmpty()) Text("No public offers yet.", color = HavenColors.TextSecondary,
          modifier = Modifier.padding(vertical = 16.dp))
        profile.offers.chunked(2).forEach { row ->
          Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { offer ->
              FeedOfferCard(offer, Modifier.weight(1f)) {
                onOpenOffer?.invoke(offer.slug) ?: onOpenWeb("/offers/${offer.slug}")
              }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
          }
        }
      }
      Spacer(Modifier.height(24.dp))
      TextButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()
        .padding(top = 12.dp).testTag("profile_link_sign_out")) {
        Text("Sign out")
      }
    }
  }
  val themeRepo = remember(context) { HavenThemePreferencesRepository.getInstance(context) }
  if (appearance) AlertDialog(
    onDismissRequest = { appearance = false },
    title = {
      Text(
        "Appearance",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        color = HavenColors.TextPrimary
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ThemeMode.entries.forEach { mode ->
          val isSelected = mode == ThemeManager.themeMode
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                themeRepo.setThemeMode(mode)
                appearance = false
              }
              .padding(vertical = 10.dp, horizontal = 4.dp)
              .testTag("appearance_option_${mode.name.lowercase()}"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = isSelected,
              onClick = {
                themeRepo.setThemeMode(mode)
                appearance = false
              },
              colors = RadioButtonDefaults.colors(
                selectedColor = HavenColors.Primary,
                unselectedColor = HavenColors.TextSecondary
              ),
              modifier = Modifier.testTag("appearance_radio_${mode.name.lowercase()}")
            )
            Spacer(Modifier.width(12.dp))
            Column {
              Text(
                text = mode.title,
                fontFamily = InterFontFamily,
                color = HavenColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
              )
              if (mode.subtitle.isNotBlank()) {
                Text(
                  text = mode.subtitle,
                  fontFamily = InterFontFamily,
                  color = HavenColors.TextSecondary,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { appearance = false }) {
        Text("Close", color = HavenColors.Primary)
      }
    }
  )
  if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false },
    title = { Text("Sign out?") }, text = { Text("Sign out of Haven on this device?") },
    confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sign out") } },
    dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } })
}

@Composable
private fun ProfileRow(label: String, value: String, icon: ImageVector, tag: String = "profile_row", onClick: () -> Unit) {
  Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp).testTag(tag),
    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Icon(icon, null, tint = HavenColors.TextPrimary, modifier = Modifier.size(20.dp))
    Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, color = HavenColors.TextPrimary)
    Text(value, fontSize = 12.sp, color = HavenColors.TextSecondary)
    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = HavenColors.TextSecondary, modifier = Modifier.size(16.dp))
  }
}
