package com.example

import com.example.model.ClickButton
import com.example.model.RemoteCommand
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun wireFormat_isStable() {
        assertEquals("M:15,-8:dev1", RemoteCommand.Move(15f, -8f).toWireString("dev1"))
        assertEquals("C:L:dev1", RemoteCommand.Click(ClickButton.LEFT).toWireString("dev1"))
        assertEquals("S:3:dev1", RemoteCommand.Scroll(3f).toWireString("dev1"))
        assertEquals("K:ENTER:dev1", RemoteCommand.Key("ENTER").toWireString("dev1"))
        assertEquals("T:a\\:b:dev1", RemoteCommand.Text("a:b").toWireString("dev1"))
        assertEquals("P:dev1:Pixel", RemoteCommand.Presence("dev1", "Pixel").toWireString("dev1"))
    }
}
