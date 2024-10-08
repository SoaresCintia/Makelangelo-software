package com.marginallyclever.convenience;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.color.*;

public class TestNameThatColor {
    // creating an instance of the NameThatColor class
    private NameThatColor ntc = new NameThatColor();
    /**
     * This test checks if the array of colors was filled during the creation of the NameThatColor class
     */
    @Test
    public void arrayColorContentTest() {
        // getting the colors from the NameThatColor class
        var colors = ntc.getColors();
        // checking if the was filled during the creation of the NameThatColor class (Oracle)
        Assertions.assertFalse(colors.isEmpty());   
    }

    @Test
    public void findColorTest() {
        // calling the find method from the NameThatColor class
        var color = ntc.find(255, 255, 255);
        // checking if the color was found(Oracle)
        Assertions.assertEquals("white", color);
    }

    @Test
    public void findColorTest2() {
        // calling the find method from the NameThatColor class
        var color = ntc.find(new Color(0, 0, 0));
        // checking if the color was found(Oracle)
        Assertions.assertEquals("black", color);
    }

    @Test
    public void findColorTest3() {
        // calling the find method from the NameThatColor class
        var color = ntc.find(new ColorRGB(255, 0, 0));
        // checking if the color was found(Oracle)
        Assertions.assertEquals("red", color);
    }

    @Test
    public void findColorTest4() {
        // calling the find method from the NameThatColor class
        var color = ntc.find(new Long(0xFF0000));
        // checking if the color was found(Oracle)
        Assertions.assertEquals("red", color);
    }


}