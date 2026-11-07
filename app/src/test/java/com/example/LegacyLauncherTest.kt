package com.example

import android.content.Context
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LegacyLauncherTest {
  @Test fun `pre-adaptive launcher retains Haven artwork and transparent corners`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val legacy = context.getDrawable(R.mipmap.ic_launcher)!!.toBitmap(192, 192)
    val round = context.getDrawable(R.mipmap.ic_launcher_round)!!.toBitmap(192, 192)
    assertEquals(0, android.graphics.Color.alpha(legacy.getPixel(0, 0)))
    assertEquals(255, android.graphics.Color.alpha(legacy.getPixel(96, 96)))
    assertTrue("Legacy icon must use the existing Haven artwork", legacy.sameAs(round))
  }
}
