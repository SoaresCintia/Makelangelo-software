package com.marginallyclever.filters;


import com.marginallyclever.makelangelo.C3;
import com.marginallyclever.makelangelo.MachineConfiguration;
import com.marginallyclever.makelangelo.MainGUI;
import com.marginallyclever.makelangelo.MultilingualSupport;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.imageio.ImageIO;


public class Filter_GeneratorRGBFloodFill extends Filter {	
	// The palette color mask has to match the tool index in the machine configuration
	C3 [] palette = new C3[] {
		new C3(0,0,0),
		new C3(255,0,0),
		new C3(0,255,0),
		new C3(0,0,255),
		new C3(255,255,255),
	};
	
	int diameter=0;
	int last_x,last_y;
	BufferedImage imgChanged;
	OutputStreamWriter osw;
	
	
	public Filter_GeneratorRGBFloodFill(MainGUI gui, MachineConfiguration mc,
			MultilingualSupport ms) {
		super(gui, mc, ms);
		// TODO Auto-generated constructor stub
	}

	public String getName() { return translator.get("RGBFloodFillName"); }

	C3 quantizeColor(C3 c) {
		C3 closest = palette[0];

	    for (C3 n : palette) 
	      if (n.diff(c) < closest.diff(c))
	        closest = n;

	    return closest;
	}

	/**
	 * Overrides MoveTo() because optimizing for zigzag is different logic than straight lines.
	 */
	protected void moveTo(float x,float y,boolean up) throws IOException {
		if(lastup!=up) {
			if(up) liftPen(osw);
			else   lowerPen(osw);
			lastup=up;
		}
		tool.writeMoveTo(osw, TX(x), TY(y));
	}
	
	// sample the pixels from x0,y0 (top left) to x1,y1 (bottom right)
	protected C3 takeImageSampleBlock(int x0,int y0,int x1,int y1) {
		// point sampling
		C3 value = new C3(0,0,0);
		int sum=0;
		
		if(x0<0) x0=0;
		if(x1>image_width-1) x1 = image_width-1;
		if(y0<0) y0=0;
		if(y1>image_height-1) y1 = image_height-1;

		for(int y=y0;y<y1;++y) {
			for(int x=x0;x<x1;++x) {
				value.add(new C3(imgChanged.getRGB(x, y)));
				++sum;
			}
		}

		if(sum==0) return new C3(255,255,255);
		
		return value.mul(1.0f/sum);
	}

	protected void setImagePixelWhite(int x0,int y0,int x1,int y1) {
		if(x0<0) x0=0;
		if(x1>image_width-1) x1 = image_width-1;
		if(y0<0) y0=0;
		if(y1>image_height-1) y1 = image_height-1;

		int c = palette[4].toInt();
		for(int y=y0;y<y1;++y) {
			for(int x=x0;x<x1;++x) {
				imgChanged.setRGB(x, y, c);
			}
		}
		imgChanged.flush();
	}

	
	/**
	 * Depth-first flood fill 
	 * @param color_index
	 * @param img
	 * @param out
	 * @throws IOException
	 */
	void floodFillBlob(int color_index,int x,int y) throws IOException {
		C3 original_color = takeImageSampleBlock(x, y, x+diameter, y+diameter);
		C3 quantized_color = quantizeColor(original_color);

		if(quantized_color.diff(palette[color_index])!=0) {
			//System.out.print("<font color='yellow'>Pop at "+x+", "+y+"</font>\n");
			return;  // visited or irrelevant.
		}

		// mark this spot as visited.
		setImagePixelWhite(x,y,x+diameter, y+diameter);
		// if the difference between the last filled pixel and this one is more than diameter*2, pen up, move, pen down.
		float dx=(float)(x-last_x);
		float dy=(float)(y-last_y);
		if(Math.sqrt(dx*dx+dy*dy) > diameter*2.0) {
			//System.out.print("Jump at "+x+", "+y+"\n");
			moveTo(last_x, last_y, true);
			moveTo(x, y, true);
			moveTo(x, y, false);
		} else {
			//System.out.print("Move to "+x+", "+y+"\n");
			moveTo(x, y, false);
		}
		// update the last position.
		last_x=x;
		last_y=y;
        
		floodFillBlob(color_index,x-diameter,y);
		floodFillBlob(color_index,x+diameter,y);
		floodFillBlob(color_index,x,y-diameter);
		floodFillBlob(color_index,x,y+diameter);
	}

	
	/**
	 * find blobs of color in the original image.  Send that to the flood fill system.
	 * @param color_index index into the list of colors at the top of the class
	 * @param img source bufferedimage
	 * @param out output stream for writing gcode.
	 * @throws IOException
	 */
	void scanForContiguousBlocks(int color_index) throws IOException {
		C3 original_color, quantized_color;
		
		int x,y;

		mainGUI.log("<font color='orange'>Palette color "+palette[color_index].toString()+"</font>\n");
		
		for(y=0;y<image_height;y+=diameter) {
			for(x=0;x<image_width;x+=diameter) {
				original_color = takeImageSampleBlock(x, y, x+diameter, y+diameter);
				quantized_color = quantizeColor(original_color); 
				if(quantized_color.diff(palette[color_index])==0) {
					// found blob

					//mainGUI.Log("<font color='red'>original color "+original_color.toString()+"</font>\n");
					//mainGUI.Log("<font color='blue'>quantized color "+quantized_color.toString()+"</font>\n");
					//mainGUI.Log("<font color='white'>Blob starts at "+x+", "+y+"</font>\n");

					try {
						floodFillBlob(color_index,x,y);
					} catch(IOException e) {
						e.printStackTrace();
					}
				
					//mainGUI.Log("<font color='white'>Blob ends at "+last_x+", "+last_y+"</font>\n");
				}
			}
		}
	}
	
	/**
	 * create horizontal lines across the image.  Raise and lower the pen to darken the appropriate areas
	 * @param img the image to convert.
	 */
	public void convert(BufferedImage img) throws IOException {
		// The picture might be in color.  Smash it to 255 shades of grey.
		//Filter_DitherFloydSteinbergRGB bw = new Filter_DitherFloydSteinbergRGB(mainGUI,machine,translator);
		//img = bw.process(img);
		
		// Open the destination file
		osw = new OutputStreamWriter(new FileOutputStream(dest),"UTF-8");
		// Set up the conversion from image space to paper space, select the current tool, etc.
		imageStart(img,osw);


		double pw = machine.getPaperWidth();
		//double ph = machine.GetPaperHeight();
		
		// figure out how many lines we're going to have on this image.
		float steps = ((float)pw*scale/(tool.getDiameter()*8.0f));
		
		
		// figure out how many lines we're going to have on this image.
		diameter = (int)(img.getWidth()/steps);
		if(diameter<1) diameter=1;
		
		imgChanged=img;

		last_x=img.getWidth()/2;
		last_y=img.getHeight()/2;
		
		int i;
		for(i=0;i<4;++i) {
			// "please change to tool X and press any key to continue"
			tool = machine.getTool(i);
			tool.writeChangeTo(osw);
			// Make sure the pen is up for the first move
			liftPen(osw);
			
			mainGUI.log("<font color='green'>Color "+i+"</font>\n");
			
			scanForContiguousBlocks(i);
		}
		mainGUI.log("<font color='green'>Signing my name</font>\n");
		
		// pen already lifted
		signName(osw);
		moveTo(0, 0, true);
		
		// close the file
		osw.close();
		
		try {
		    // save image
		    File outputfile = new File("saved.png");
		    ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
}


/**
 * This file is part of DrawbotGUI.
 *
 * DrawbotGUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DrawbotGUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */