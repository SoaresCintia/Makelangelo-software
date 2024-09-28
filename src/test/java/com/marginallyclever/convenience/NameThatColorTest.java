package com.marginallyclever.convenience;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals

public class NameThatColorTest {
    private NameThatColor ntc = new NameThatColor();
    @Test
    public void arrayColorContentTest() {
        var color = ntc.getColors();
        assertFalse(color.isEmpty());   
    }

    @Test
    public void findColorTest() {
        var color = ntc.find(255, 255, 255);
        assertEquals("White", color);
    }
}