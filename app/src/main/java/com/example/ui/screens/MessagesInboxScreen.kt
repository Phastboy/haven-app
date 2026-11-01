package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.InboxConversation
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.model.MessagesInboxUiState
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MessagesInboxScreen(
  state: MessagesInboxUiState,
  onRetry: () -> Unit,
  onLoadMore: () -> Unit,
  onSelectConversation: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier.fillMaxSize().background(HavenColors.Canvas).testTag("messages_inbox_screen")) {
    HavenTopBarContainer(backgroundColor = HavenColors.Canvas) {
      Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        Text("Messages", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp, color = HavenColors.TextPrimary)
      }
    }
    when {
      state.loading && state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = HavenColors.Primary, modifier = Modifier.testTag("messages_loading"))
      }
      state.error != null && state.items.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(state.error, color = HavenColors.TextSecondary, fontFamily = InterFontFamily,
          fontSize = 14.sp, modifier = Modifier.testTag("messages_error"))
        TextButton(onClick = onRetry) { Text("Try again") }
      }
      state.items.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No messages yet", color = HavenColors.TextPrimary, fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("When you message someone about an offer or someone reaches out to you, conversations will appear here.",
          color = HavenColors.TextSecondary, fontFamily = InterFontFamily, fontSize = 14.sp)
      }
      else -> LazyColumn(Modifier.fillMaxSize().testTag("messages_list")) {
        items(state.items, key = InboxConversation::id) { item ->
          InboxConversationRow(item, onClick = { onSelectConversation(item.id) })
          HorizontalDivider(color = HavenColors.BorderSubtle, modifier = Modifier.padding(start = 16.dp))
        }
        if (state.nextCursor != null) item {
          if (state.error != null) Text(state.error, color = HavenColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
          TextButton(onClick = onLoadMore, enabled = !state.loadingMore,
            modifier = Modifier.fillMaxWidth().testTag("messages_load_more")) {
            if (state.loadingMore) CircularProgressIndicator(Modifier.size(20.dp), color = HavenColors.Primary)
            else Text("Load more")
          }
        }
      }
    }
  }
}

@Composable
private fun InboxConversationRow(item: InboxConversation, onClick: () -> Unit) {
  Row(Modifier.fillMaxWidth().height(80.dp).clickable(onClick = onClick)
    .padding(horizontal = 16.dp).testTag("conversation_${item.id}"),
    verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(48.dp)) {
      Box(Modifier.fillMaxSize().clip(CircleShape).background(HavenColors.SurfaceContainer),
        contentAlignment = Alignment.Center) {
        val initials = item.displayName.trim().split(Regex("\\s+")).take(2)
          .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        if (item.avatarUrl.isNullOrBlank()) {
          Text(initials, fontFamily = InterFontFamily, fontSize = 16.sp,
            fontWeight = FontWeight.Medium, color = HavenColors.TextPrimary)
        } else {
          SubcomposeAsyncImage(model = item.avatarUrl, contentDescription = item.displayName,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            loading = { Text(initials, color = HavenColors.TextPrimary) },
            error = { Text(initials, color = HavenColors.TextPrimary) })
        }
      }
      if (item.isUnread) Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape)
        .background(HavenColors.TextPrimary).testTag("unread_${item.id}"))
    }
    Spacer(Modifier.size(12.dp))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(item.displayName, modifier = Modifier.weight(1f, fill = false),
          fontFamily = InterFontFamily, fontWeight = if (item.isUnread) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 16.sp, color = HavenColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.size(8.dp))
        Text(inboxTimeLabel(item.lastMessageAt), fontFamily = InterFontFamily,
          fontSize = 13.sp, color = HavenColors.TextSecondary)
      }
      Spacer(Modifier.height(4.dp))
      Text(item.lastMessageBody, fontFamily = InterFontFamily,
        fontWeight = if (item.isUnread) FontWeight.Medium else FontWeight.Normal,
        fontSize = 14.sp, color = if (item.isUnread) HavenColors.TextPrimary else HavenColors.TextSecondary,
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("conversation_preview_${item.id}"))
    }
  }
}

internal fun inboxTimeLabel(raw: String, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
  val messageTime = runCatching { Instant.parse(raw).atZone(zone) }.getOrNull() ?: return ""
  val today = now.atZone(zone).toLocalDate()
  val date = messageTime.toLocalDate()
  return when (date) {
    today -> messageTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    today.minusDays(1) -> "Yesterday"
    else -> messageTime.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "MMM d" else "MMM d, yyyy", Locale.getDefault()))
  }
}
