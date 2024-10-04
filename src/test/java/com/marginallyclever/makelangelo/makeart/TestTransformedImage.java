package com.marginallyclever.makelangelo.makeart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

public class TestTransformedImage {

    @Test
    public void testImageInside() {

        // Arange
        double x = 3.0;
        double y = 4.0;
        BufferedImage bufferedimage = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_ARGB);
        TransformedImage transformedImage = new TransformedImage(bufferedimage);

        // Act
        Boolean answer = transformedImage.canSampleAt(x, y);

        // Oracle
        assertTrue(answer);
    }

    @Test
    public void testImageOutside() {

        // Arange
        double x = 11.0;
        double y = 11.0;
        BufferedImage bufferedimage = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_ARGB);
        TransformedImage transformedImage = new TransformedImage(bufferedimage);

        // Act
        Boolean answer = transformedImage.canSampleAt(x, y);

        // Oracle
        assertFalse(answer);
    }

    @Test
    public void testSampleZero() {

        // Arange
        double x0 = 11.0;
        double y0 = 11.0;
        double x1 = 10.0;
        double y1 = 10.0;
        BufferedImage bufferedimage = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_ARGB);
        TransformedImage transformedImage = new TransformedImage(bufferedimage);

        // Act
        int answer = transformedImage.sample(x0, y0, x1, y1);

        // Oracle
        assertEquals(answer, 0);
    }
}
