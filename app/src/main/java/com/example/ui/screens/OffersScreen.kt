package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AttachOfferBottomSheet
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.HavenBottomBarContainer
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenPrimaryButton
import com.example.ui.components.HavenSecondaryButton
import com.example.ui.components.OfferCard
import com.example.ui.model.Offer
import com.example.ui.model.OffersTab
import com.example.ui.model.OffersViewModel
import com.example.ui.model.MessagesViewModel
import com.example.ui.model.SessionManager
import com.example.ui.model.UserSession
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Haven Offers Screen & App Shell:
 * - Clean top-level pages: NO back button, NO refresh button, NO page title bar.
 * - Skeletons are strictly offercard-based.
 * - Full conversation list page and thread page with attachable offers in messages.
 * - Thoughtful unauthenticated UX for Messages and Profile tabs that guides users to sign in.
 */
@Composable
fun OffersScreen(
  viewModel: OffersViewModel,
  messagesViewModel: MessagesViewModel = viewModel(),
  onNavigateBack: () -> Unit,
  onNavigateToSignIn: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val messagesState by messagesViewModel.uiState.collectAsState()
  val session by SessionManager.session.collectAsState()
  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  val detailOffer = uiState.selectedOffer ?: messagesState.inspectedOfferDetail
  val isDetailOpen = detailOffer != null
  val savedOfferIds = uiState.allOffers.filter { it.isFavorite }.map { it.id }.toSet()

  BackHandler {
    when {
      isDetailOpen -> {
        viewModel.selectOffer(null)
        messagesViewModel.inspectOffer(null)
      }
      messagesState.selectedConversationId != null -> {
        messagesViewModel.closeConversationThread()
      }
      else -> onNavigateBack()
    }
  }

  val isInsideConversation = uiState.activeTab == OffersTab.MESSAGES && messagesState.selectedConversation != null

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        focusManager.clearFocus()
      }
      .testTag("offers_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      if (!isInsideConversation && !isDetailOpen) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(HavenInsets.statusBarsAndCutouts))
      }

      // Main Content Area
      when (uiState.activeTab) {
        OffersTab.OFFERS -> {
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(scrollState)
              .padding(horizontal = 16.dp)
              .padding(top = 8.dp, bottom = 12.dp)
          ) {
            // Search & Filter Row (44dp height — always active and interactive)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Search Input Box
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(HavenColors.Canvas)
                  .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
                  .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = HavenColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Box(modifier = Modifier.weight(1f)) {
                    if (uiState.searchQuery.isEmpty()) {
                      Text(
                        text = "Search offers by name...",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = HavenColors.TextMuted
                      )
                    }
                    BasicTextField(
                      value = uiState.searchQuery,
                      onValueChange = { viewModel.onSearchQueryChange(it) },
                      singleLine = true,
                      textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = HavenColors.TextPrimary
                      ),
                      cursorBrush = SolidColor(HavenColors.Primary),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offers_search_input")
                    )
                  }
                  if (uiState.searchQuery.isNotEmpty()) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Clear search",
                      tint = HavenColors.TextMuted,
                      modifier = Modifier
                        .size(18.dp)
                        .clickable { viewModel.onSearchQueryChange("") }
                    )
                  }
                }
              }

              // Filters Button (44dp height)
              Box(
                modifier = Modifier
                  .height(44.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(HavenColors.Canvas)
                  .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
                  .clickable { viewModel.openFilterSheet() }
                  .padding(horizontal = 14.dp)
                  .testTag("open_filters_button"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Open filters",
                    tint = HavenColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Filters",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = HavenColors.TextPrimary
                  )
                  if (uiState.activeFilterCount > 0) {
                    Box(
                      modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(HavenColors.Primary),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = uiState.activeFilterCount.toString(),
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HavenColors.TextOnDark
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Offercard-Based Grid Rendering
            if (uiState.isLoading) {
              // Individual card-based skeleton placeholders in the 2-column grid
              Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                for (rowIndex in 0 until 3) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Box(modifier = Modifier.weight(1f)) {
                      OfferCard(
                        offer = null,
                        isLoading = true
                      )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                      OfferCard(
                        offer = null,
                        isLoading = true
                      )
                    }
                  }
                }
              }
            } else if (uiState.filteredOffers.isEmpty()) {
              // Empty search/filter state
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "No offers match your criteria",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 16.sp,
                  color = HavenColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Try clearing your filters or search keywords.",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.Normal,
                  fontSize = 13.sp,
                  color = HavenColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(HavenColors.SurfaceContainer)
                    .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(6.dp))
                    .clickable { viewModel.clearFilters() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                  Text(
                    text = "Clear all filters",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = HavenColors.TextPrimary
                  )
                }
              }
            } else {
              // 2-Column Grid of Loaded Cards (with per-card image skeleton fallback)
              Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                val offers = uiState.filteredOffers
                val rowCount = (offers.size + 1) / 2
                for (rowIndex in 0 until rowCount) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    val firstIndex = rowIndex * 2
                    val secondIndex = firstIndex + 1

                    Box(modifier = Modifier.weight(1f)) {
                      OfferCard(
                        offer = offers[firstIndex],
                        onClick = { viewModel.selectOffer(offers[firstIndex]) },
                        onToggleFavorite = { viewModel.onToggleFavorite(offers[firstIndex].id) }
                      )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                      if (secondIndex < offers.size) {
                        OfferCard(
                          offer = offers[secondIndex],
                          onClick = { viewModel.selectOffer(offers[secondIndex]) },
                          onToggleFavorite = { viewModel.onToggleFavorite(offers[secondIndex].id) }
                        )
                      } else {
                        Spacer(modifier = Modifier.fillMaxWidth())
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(24.dp))
          }
        }
        OffersTab.MESSAGES -> {
          if (!session.isAuthenticated) {
            AuthGateView(
              icon = Icons.Outlined.ChatBubbleOutline,
              title = "Direct trade messaging",
              description = "Sign in to negotiate direct trades, propose barter items, and chat privately with artisans.",
              benefits = listOf(
                "Real-time barter exchange proposals",
                "Direct member-to-member messaging",
                "Attach offers to any message bubble",
                "Instant sign-in via magic link (no passwords)"
              ),
              onDemoSignIn = { SessionManager.signIn("adaeze.obi@haven.ng") },
              onSignIn = onNavigateToSignIn,
              onBrowseOffers = { viewModel.selectTab(OffersTab.OFFERS) },
              modifier = Modifier.weight(1f)
            )
          } else {
            // Authenticated Messages: Conversation List vs Thread View
            val selectedConv = messagesState.selectedConversation
            if (selectedConv != null) {
              ConversationThreadView(
                conversation = selectedConv,
                messageInputText = messagesState.messageInputText,
                pendingAttachedOffer = messagesState.pendingAttachedOffer,
                onBack = { messagesViewModel.closeConversationThread() },
                onMessageInputChange = { messagesViewModel.onMessageInputChange(it) },
                onOpenAttachSheet = { messagesViewModel.openAttachSheet() },
                onRemoveAttachment = { messagesViewModel.removeAttachedOffer() },
                onSendMessage = { messagesViewModel.sendMessage() },
                onInspectOffer = { messagesViewModel.inspectOffer(it) },
                modifier = Modifier.fillMaxSize()
              )
            } else {
              ConversationListView(
                conversations = messagesState.conversations,
                onSelectConversation = { messagesViewModel.selectConversation(it) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
        OffersTab.PROFILE -> {
          if (!session.isAuthenticated) {
            AuthGateView(
              icon = Icons.Outlined.PersonOutline,
              title = "Your Haven account",
              description = "Sign in to manage your listed items, view saved offers, and keep track of your exchanges.",
              benefits = listOf(
                "Saved offers bookmark sync",
                "Verified Haven member badge",
                "Fast passwordless authentication"
              ),
              onDemoSignIn = { SessionManager.signIn("adaeze.obi@haven.ng") },
              onSignIn = onNavigateToSignIn,
              onBrowseOffers = { viewModel.selectTab(OffersTab.OFFERS) },
              modifier = Modifier.weight(1f)
            )
          } else {
            ProfileScreen(
              session = session,
              savedOffersCount = uiState.allOffers.count { it.isFavorite },
              onSignOut = { SessionManager.signOut() },
              onNavigateToSavedOffers = {
                viewModel.selectTab(OffersTab.OFFERS)
              },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // Android Bottom Navigation Bar (80px height) matching Haven spec
      if (!isInsideConversation && !isDetailOpen) {
        HavenBottomBarContainer(
          backgroundColor = HavenColors.Canvas,
          showTopBorder = true,
          borderColor = HavenColors.BorderSubtle
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TabItem(
              label = "Offers",
              icon = ProfileIcons.Storefront,
              isActive = uiState.activeTab == OffersTab.OFFERS,
              onClick = {
                if (uiState.activeTab == OffersTab.MESSAGES) {
                  messagesViewModel.closeConversationThread()
                }
                viewModel.selectTab(OffersTab.OFFERS)
              },
              modifier = Modifier.weight(1f)
            )

            // Show badge if authenticated and has unread messages
            TabItem(
              label = "Messages",
              icon = Icons.Outlined.ChatBubbleOutline,
              isActive = uiState.activeTab == OffersTab.MESSAGES,
              hasBadge = session.isAuthenticated && messagesState.unreadCount > 0,
              onClick = { viewModel.selectTab(OffersTab.MESSAGES) },
              modifier = Modifier.weight(1f)
            )

            TabItem(
              label = "Profile",
              icon = Icons.Outlined.Person,
              isActive = uiState.activeTab == OffersTab.PROFILE,
              onClick = {
                if (uiState.activeTab == OffersTab.MESSAGES) {
                  messagesViewModel.closeConversationThread()
                }
                viewModel.selectTab(OffersTab.PROFILE)
              },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // Filter Bottom Sheet Modal
    if (uiState.isFilterSheetVisible) {
      FilterBottomSheet(
        currency = uiState.draftCurrency,
        minPriceText = uiState.draftMinPriceText,
        maxPriceText = uiState.draftMaxPriceText,
        onCurrencyChange = { viewModel.onDraftCurrencyChange(it) },
        onMinPriceChange = { viewModel.onDraftMinPriceChange(it) },
        onMaxPriceChange = { viewModel.onDraftMaxPriceChange(it) },
        onApply = { viewModel.applyFilters() },
        onClear = { viewModel.clearFilters() },
        onDismiss = { viewModel.closeFilterSheet() }
      )
    }

    // Full-screen Offer Detail (opened from the feed or a message attachment)
    detailOffer?.let { offer ->
      OfferDetailScreen(
        offer = offer,
        isSaved = offer.id in savedOfferIds,
        isAuthenticated = session.isAuthenticated,
        onBack = {
          viewModel.selectOffer(null)
          messagesViewModel.inspectOffer(null)
        },
        onToggleSave = { viewModel.onToggleFavorite(offer.id) },
        onMessageProvider = {
          viewModel.selectOffer(null)
          messagesViewModel.inspectOffer(null)
          viewModel.selectTab(OffersTab.MESSAGES)
          messagesViewModel.startConversationForOffer(offer)
        },
        onSignInRequested = onNavigateToSignIn,
        modifier = Modifier.fillMaxSize()
      )
    }

    // Attach Offer Bottom Sheet (triggered from message composer)
    if (messagesState.isAttachSheetVisible) {
      AttachOfferBottomSheet(
        onSelectOffer = { messagesViewModel.selectOfferToAttach(it) },
        onDismiss = { messagesViewModel.closeAttachSheet() }
      )
    }
  }
}

/**
 * Thoughtful, high-converting monastic Auth Gate shown when visiting Messages or Profile while unauthenticated
 */
@Composable
private fun AuthGateView(
  icon: ImageVector,
  title: String,
  description: String,
  benefits: List<String>,
  onDemoSignIn: () -> Unit,
  onSignIn: () -> Unit,
  onBrowseOffers: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Spacer(modifier = Modifier.height(28.dp))

    // Monastic Icon in 56dp circle
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(HavenColors.SurfaceContainer)
        .border(1.dp, HavenColors.BorderDefault, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = HavenColors.Primary,
        modifier = Modifier.size(26.dp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = title,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 22.sp,
      lineHeight = 28.sp,
      color = HavenColors.TextPrimary,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = description,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 22.sp,
      color = HavenColors.TextSecondary,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Feature highlights container
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(HavenColors.SurfaceContainer)
        .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      benefits.forEach { benefit ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = HavenColors.Primary,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = benefit,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = HavenColors.TextPrimary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Call to action buttons
    HavenPrimaryButton(
      text = "Sign in as Demo Member (1-Click)",
      onClick = onDemoSignIn,
      modifier = Modifier.fillMaxWidth(),
      testTag = "auth_gate_demo_sign_in_button"
    )

    Spacer(modifier = Modifier.height(10.dp))

    HavenSecondaryButton(
      text = "Sign in with Magic Link",
      onClick = onSignIn,
      modifier = Modifier.fillMaxWidth(),
      testTag = "auth_gate_sign_in_button"
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "Browse offers instead",
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 13.sp,
      color = HavenColors.TextPrimary,
      modifier = Modifier
        .clickable { onBrowseOffers() }
        .padding(6.dp)
        .testTag("auth_gate_browse_button")
    )

    Spacer(modifier = Modifier.height(24.dp))
  }
}



@Composable
private fun TabItem(
  label: String,
  icon: ImageVector,
  isActive: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  hasBadge: Boolean = false
) {
  val textColor = if (isActive) HavenColors.TextPrimary else HavenColors.TextSecondary
  val iconColor = if (isActive) HavenColors.TextPrimary else HavenColors.TextSecondary

  Column(
    modifier = modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onClick() }
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // 56px x 32px pill container for icon
    Box(
      modifier = Modifier
        .width(56.dp)
        .height(32.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(if (isActive) HavenColors.SurfaceContainer else Color.Transparent),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = iconColor,
        modifier = Modifier.size(24.dp)
      )

      if (hasBadge) {
        // Unread dot (7px x 7px, HavenColors.TextPrimary with 1.5px HavenColors.Canvas border)
        Box(
          modifier = Modifier
            .size(8.dp)
            .align(Alignment.TopEnd)
            .padding(end = 4.dp, top = 2.dp)
            .clip(CircleShape)
            .background(HavenColors.TextPrimary)
            .border(1.5.dp, HavenColors.Canvas, CircleShape)
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = label,
      fontFamily = InterFontFamily,
      fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
      fontSize = 12.sp,
      letterSpacing = 0.12.sp,
      color = textColor
    )
  }
}
