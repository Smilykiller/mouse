package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ClickButton
import com.example.model.RemoteCommand
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AirGlide", appName)
    }

    @Test
    fun `verify remote command serialization`() {
        val move = RemoteCommand.Move(15f, -8f)
        assertEquals("M:15,-8:device_1", move.toWireString("device_1"))

        val click = RemoteCommand.Click(ClickButton.LEFT)
        assertEquals("C:L:device_1", click.toWireString("device_1"))

        val mouseDown = RemoteCommand.MouseDown(ClickButton.LEFT)
        assertEquals("D:L:device_1", mouseDown.toWireString("device_1"))

        val mouseUp = RemoteCommand.MouseUp(ClickButton.LEFT)
        assertEquals("U:L:device_1", mouseUp.toWireString("device_1"))

        val scroll = RemoteCommand.Scroll(3f)
        assertEquals("S:3:device_1", scroll.toWireString("device_1"))

        val key = RemoteCommand.Key("PAGE_UP")
        assertEquals("K:PAGE_UP:device_1", key.toWireString("device_1"))

        val text = RemoteCommand.Text("https://google.com")
        assertEquals("T:https\\://google.com:device_1", text.toWireString("device_1"))

        val presence = RemoteCommand.Presence("dev_123", "Pixel 9")
        assertEquals("P:dev_123:Pixel 9", presence.toWireString("dev_123"))
    }
}
