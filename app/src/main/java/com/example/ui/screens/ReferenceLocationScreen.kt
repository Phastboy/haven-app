package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.HavenAuthClient
import com.example.data.HavenPlacesRepository
import com.example.data.PlaceCandidate
import com.example.data.PlacesRepository
import com.example.ui.components.HavenBackButton
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.havenBottomSafePadding
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReferenceLocationScreen(
  currentReference: PlaceCandidate?,
  onBack: () -> Unit,
  onApplyReference: (PlaceCandidate) -> Unit,
  onTurnOffProximity: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val placesRepo = remember(context) { HavenPlacesRepository(HavenAuthClient.shared(context)) }
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  var searchQuery by remember { mutableStateOf("") }
  var isSearching by remember { mutableStateOf(false) }
  var searchError by remember { mutableStateOf<String?>(null) }
  var candidates by remember { mutableStateOf<List<PlaceCandidate>>(emptyList()) }
  var selectedCandidate by remember { mutableStateOf(currentReference) }
  var searchJob by remember { mutableStateOf<Job?>(null) }

  fun performSearch(query: String) {
    searchJob?.cancel()
    if (query.trim().length < 2) {
      candidates = emptyList()
      isSearching = false
      return
    }
    isSearching = true
    searchError = null
    searchJob = scope.launch {
      delay(300) // Debounce typing
      placesRepo.searchPlaces(query)
        .onSuccess { results ->
          candidates = results
          isSearching = false
        }
        .onFailure {
          searchError = "Couldn’t search places. Please try again."
          isSearching = false
        }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("reference_location_screen")
  ) {
    // Header
    HavenTopBarContainer {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HavenBackButton(
          onBack = onBack,
          testTag = "reference_location_back"
        )
        Text(
          text = "Reference location",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Spacer(Modifier.size(44.dp))
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Explainer Card
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "PROXIMITY DISCOVERY",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            color = HavenColors.TextSecondary
          )
          Text(
            text = "Choose reference location",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            color = HavenColors.TextPrimary
          )
          Text(
            text = "Filter offers by distance from a city, town, or neighbourhood. Distances are approximate.",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = HavenColors.TextSecondary
          )

          // Privacy Callout
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(HavenColors.SurfaceContainer)
              .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              Icons.Outlined.Shield,
              contentDescription = null,
              tint = HavenColors.TextSecondary,
              modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Text(
              text = "Your device’s exact GPS is never requested or shared. Reference locations are approximate places you choose.",
              fontFamily = InterFontFamily,
              fontSize = 13.sp,
              lineHeight = 19.sp,
              color = HavenColors.TextPrimary
            )
          }
        }
      }

      // Search Box
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Find a place",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = HavenColors.TextPrimary
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(HavenColors.SurfaceContainer)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Outlined.Search,
              contentDescription = null,
              tint = HavenColors.TextSecondary,
              modifier = Modifier.size(20.dp)
            )
            Box(Modifier.weight(1f)) {
              if (searchQuery.isEmpty()) {
                Text(
                  text = "Search city or neighbourhood…",
                  fontFamily = InterFontFamily,
                  fontSize = 15.sp,
                  color = HavenColors.TextSecondary
                )
              }
              BasicTextField(
                value = searchQuery,
                onValueChange = {
                  searchQuery = it
                  performSearch(it)
                },
                textStyle = TextStyle(
                  fontFamily = InterFontFamily,
                  fontSize = 15.sp,
                  color = HavenColors.TextPrimary
                ),
                cursorBrush = SolidColor(HavenColors.TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().testTag("reference_location_input")
              )
            }
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = {
                  searchQuery = ""
                  performSearch("")
                },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  Icons.Outlined.Close,
                  contentDescription = "Clear search",
                  tint = HavenColors.TextSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }

      // Selected Reference Badge
      selectedCandidate?.let { selected ->
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(HavenColors.SurfaceCard)
              .border(1.dp, HavenColors.TextPrimary, RoundedCornerShape(12.dp))
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(HavenColors.TextPrimary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Outlined.LocationOn,
                  contentDescription = null,
                  tint = HavenColors.TextOnDark,
                  modifier = Modifier.size(18.dp)
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = selected.label,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp,
                  color = HavenColors.TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Active reference point",
                  fontFamily = InterFontFamily,
                  fontSize = 12.sp,
                  color = HavenColors.TextSecondary
                )
              }
            }

            TextButton(
              onClick = { selectedCandidate = null },
              modifier = Modifier.padding(start = 8.dp)
            ) {
              Text("Clear", color = HavenColors.TextSecondary, fontSize = 13.sp)
            }
          }
        }
      }

      // Results Section
      if (isSearching) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = HavenColors.Primary)
          }
        }
      } else if (candidates.isNotEmpty()) {
        item {
          Text(
            text = "SEARCH RESULTS",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary,
            letterSpacing = 0.5.sp
          )
        }
        items(candidates) { candidate ->
          val isCandidateSelected = selectedCandidate?.near == candidate.near
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isCandidateSelected) HavenColors.TextPrimary else HavenColors.SurfaceContainer)
              .clickable { selectedCandidate = candidate }
              .padding(14.dp)
              .testTag("place_candidate_${candidate.near}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(if (isCandidateSelected) HavenColors.TextSecondary else HavenColors.BorderSubtle),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Outlined.LocationOn,
                  contentDescription = null,
                  tint = if (isCandidateSelected) HavenColors.TextOnDark else HavenColors.TextPrimary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Text(
                text = candidate.label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isCandidateSelected) HavenColors.TextOnDark else HavenColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            if (isCandidateSelected) {
              Icon(
                Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = HavenColors.TextOnDark,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      } else if (searchQuery.trim().length >= 2 && !isSearching) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(HavenColors.SurfaceContainer)
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No places found for “${searchQuery.trim()}”.",
              fontFamily = InterFontFamily,
              fontSize = 14.sp,
              color = HavenColors.TextSecondary
            )
          }
        }
      }
    }

    // Anchored Action Area
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(HavenColors.Canvas)
        .havenBottomSafePadding()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(if (selectedCandidate != null) HavenColors.TextPrimary else HavenColors.BorderDefault)
          .clickable(enabled = selectedCandidate != null) {
            selectedCandidate?.let { onApplyReference(it) }
          }
          .testTag("apply_reference_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Apply reference location",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 14.sp,
          color = if (selectedCandidate != null) HavenColors.TextOnDark else HavenColors.TextSecondary
        )
      }

      TextButton(
        onClick = onTurnOffProximity,
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp)
          .testTag("turn_off_proximity_button")
      ) {
        Text(
          text = "Turn off proximity",
          fontFamily = InterFontFamily,
          fontSize = 13.sp,
          color = HavenColors.TextSecondary
        )
      }
    }
  }
}
