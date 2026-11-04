package com.example

import com.example.model.ClickButton
import com.example.model.RemoteCommand
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun verifyRemoteCommandSerialization() {
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

        val action = RemoteCommand.Action("WIN_TAB")
        assertEquals("A:WIN_TAB:device_1", action.toWireString("device_1"))

        val jump = RemoteCommand.DisplayJump("next")
        assertEquals("J:next:device_1", jump.toWireString("device_1"))

        val tabSwitch = RemoteCommand.TabSwitch(1)
        assertEquals("W:1:device_1", tabSwitch.toWireString("device_1"))
    }
}
