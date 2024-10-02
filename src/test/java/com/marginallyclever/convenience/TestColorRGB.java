package com.marginallyclever.convenience;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestColorRGB {
    private void assertColorsEqual(ColorRGB c,int r,int g,int b) {
        Assertions.assertEquals(r,c.red);
        Assertions.assertEquals(g,c.green);
        Assertions.assertEquals(b,c.blue);
    }

    @Test
    public void test() {
        assertColorsEqual(new ColorRGB(0xff123456),0x12,0x34,0x56);
    }
    @Test 
    public void TestColorRGB() {
        //  Creating two test that will test the set method of the ColorRGB class
        ColorRGB c = new ColorRGB();
        ColorRGB c2 = new ColorRGB(0x12,0x34,0x56);
        // Setting the color of c to the color of c2
        c.set(c2);
        // Checking if the colors are equal
        Assertions.assertEquals(c.red, c2.red);
        Assertions.assertEquals(c.green, c2.green);
        Assertions.assertEquals(c.blue, c2.blue);

    }

    @Test 
    public void TestColorRGBSub(){
        // Creating a new ColorRGB object
        ColorRGB c = new ColorRGB(0x12,0x34,0x56);
        // Creating a new ColorRGB object
        ColorRGB c3 = new ColorRGB(0x12,0x34,0x56);
        // Subtracting the color of c2 from the color of c
        ColorRGB c2 = c.sub(c3);
        // Checking if the colors are equal

        Assertions.assertNotEquals(c.red, c2.red);
        Assertions.assertNotEquals(c.green, c2.green);
        Assertions.assertNotEquals(c.blue, c2.blue);
    }

   
   
}
