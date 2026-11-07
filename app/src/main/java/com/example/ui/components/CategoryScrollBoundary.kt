package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/** A category swipe stays in its strip, including when the strip reaches its end. */
internal object CategoryScrollBoundary : NestedScrollConnection {
  override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
    Offset(available.x, 0f)

  override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
    Velocity(available.x, 0f)
}
