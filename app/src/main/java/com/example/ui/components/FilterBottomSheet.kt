package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Filter Bottom Sheet Modal matching the web specification:
 * - 16dp top corner radius
 * - Drag grabber (36x4dp)
 * - Header with "Filters" and close button
 * - Price section with Currency picker (Any, NGN, USD, EUR, GBP, CAD, AUD, KES, GHS)
 * - Min Price & Max Price (0 for Free) input row
 * - Footer with "Clear" and "Show results"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
  currency: String,
  minPriceText: String,
  maxPriceText: String,
  onCurrencyChange: (String) -> Unit,
  onMinPriceChange: (String) -> Unit,
  onMaxPriceChange: (String) -> Unit,
  onApply: () -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isCurrencyDropdownOpen by remember { mutableStateOf(false) }

  val currencies = listOf("Any", "NGN", "USD", "EUR", "GBP", "CAD", "AUD", "KES", "GHS")

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = HavenColors.Canvas,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(width = 36.dp, height = 4.dp)
            .clip(CircleShape)
            .background(HavenColors.BorderDefault)
        )
      }
    },
    modifier = modifier.testTag("filter_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    ) {
      // Sheet Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .border(width = 1.dp, color = HavenColors.BorderSubtle)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Filters",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary
        )

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .testTag("close_filters_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close filters",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Sheet Body
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Price",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp,
          color = HavenColors.TextPrimary
        )

        // Currency Selector Field
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Currency",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
              .clip(RoundedCornerShape(6.dp))
              .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(6.dp))
              .clickable { isCurrencyDropdownOpen = true }
              .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = currency,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary
              )
              Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Select currency",
                tint = HavenColors.TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }

            DropdownMenu(
              expanded = isCurrencyDropdownOpen,
              onDismissRequest = { isCurrencyDropdownOpen = false },
              modifier = Modifier.background(HavenColors.Canvas)
            ) {
              currencies.forEach { curr ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = curr,
                      fontFamily = InterFontFamily,
                      fontSize = 14.sp,
                      color = HavenColors.TextPrimary,
                      fontWeight = if (curr == currency) FontWeight.SemiBold else FontWeight.Normal
                    )
                  },
                  onClick = {
                    onCurrencyChange(curr)
                    isCurrencyDropdownOpen = false
                  }
                )
              }
            }
          }
        }

        // Min / Max Price Inputs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Min price
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Min price",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
              color = HavenColors.TextSecondary
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              if (minPriceText.isEmpty()) {
                Text(
                  text = "0",
                  fontFamily = InterFontFamily,
                  fontSize = 14.sp,
                  color = HavenColors.TextMuted
                )
              }
              BasicTextField(
                value = minPriceText,
                onValueChange = onMinPriceChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                  fontFamily = InterFontFamily,
                  fontSize = 14.sp,
                  color = HavenColors.TextPrimary
                ),
                cursorBrush = SolidColor(HavenColors.Primary),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }

          // Max price
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Max price (0 for Free)",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
              color = HavenColors.TextSecondary
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              if (maxPriceText.isEmpty()) {
                Text(
                  text = "No limit",
                  fontFamily = InterFontFamily,
                  fontSize = 14.sp,
                  color = HavenColors.TextMuted
                )
              }
              BasicTextField(
                value = maxPriceText,
                onValueChange = onMaxPriceChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                  fontFamily = InterFontFamily,
                  fontSize = 14.sp,
                  color = HavenColors.TextPrimary
                ),
                cursorBrush = SolidColor(HavenColors.Primary),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Sheet Footer
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, HavenColors.BorderSubtle)
          .background(HavenColors.Canvas)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Clear",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 13.sp,
          color = HavenColors.TextPrimary,
          textDecoration = TextDecoration.Underline,
          modifier = Modifier
            .clickable { onClear() }
            .padding(4.dp)
            .testTag("clear_filters_button")
        )

        Button(
          onClick = onApply,
          shape = RoundedCornerShape(6.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = HavenColors.Primary,
            contentColor = HavenColors.TextOnDark
          ),
          modifier = Modifier
            .height(40.dp)
            .testTag("apply_filters_button")
        ) {
          Text(
            text = "Show results",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}
