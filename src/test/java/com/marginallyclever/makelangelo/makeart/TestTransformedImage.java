package com.marginallyclever.makelangelo.makeart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import java.awt.Color;

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
        assertEquals(0, answer);
    }

    // @Test
    // public void testSampleUnchecked() {

    // // Arange
    // double x0 = 3.0;
    // double y0 = 4.0;
    // double x1 = 2.0;
    // double y1 = 3.0;
    // BufferedImage bufferedImage = new BufferedImage(10, 10,
    // BufferedImage.TYPE_INT_ARGB);

    // int redColor = new Color(255, 0, 0, 255).getRGB(); // Rouge opaque
    // int blueColor = new Color(0, 0, 255, 255).getRGB(); // Bleu opaque

    // // Remplit l'image avec du bleu
    // for (int i = 0; i < bufferedImage.getWidth(); i++) {
    // for (int j = 0; j < bufferedImage.getHeight(); j++) {
    // bufferedImage.setRGB(i, j, blueColor);
    // }
    // }

    // // Défini du pixel (3,4) comme rouge
    // bufferedImage.setRGB((int) Math.round(x0), (int) Math.round(y0), redColor);

    // TransformedImage transformedImage = new TransformedImage(bufferedImage);

    // // Act
    // int answer = transformedImage.sample(x0, y0, x1, y1);

    // // Oracle
    // // assertEquals(0, answer);
    // }
}
