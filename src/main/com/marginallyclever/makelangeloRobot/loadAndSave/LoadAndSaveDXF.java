package com.marginallyclever.makelangeloRobot.loadAndSave;

import java.awt.GridLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.kabeja.dxf.Bounds;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEntity;
import org.kabeja.dxf.DXFLWPolyline;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFLine;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.dxf.DXFSpline;
import org.kabeja.dxf.DXFVertex;
import org.kabeja.dxf.helpers.DXFSplineConverter;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;

import com.marginallyclever.convenience.ColorRGB;
import com.marginallyclever.convenience.MathHelper;
import com.marginallyclever.convenience.Turtle;
import com.marginallyclever.makelangelo.Log;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.select.SelectFloat;
import com.marginallyclever.makelangeloRobot.ImageManipulator;
import com.marginallyclever.makelangeloRobot.MakelangeloRobot;
import com.marginallyclever.makelangeloRobot.settings.MakelangeloRobotSettings;

/**
 * Reads in DXF file and converts it to a temporary gcode file, then calls LoadGCode. 
 * @author Dan Royer
 *
 */
public class LoadAndSaveDXF extends ImageManipulator implements LoadAndSaveFileType {
	private static boolean shouldScaleOnLoad=true;
	private static boolean shouldInfillOnLoad=true;
	private static boolean shouldOptimizePathingOnLoad=false;
	private static FileNameExtensionFilter filter = new FileNameExtensionFilter(Translator.get("FileTypeDXF"), "dxf");
	private double previousX,previousY;
	private double scale,imageCenterX,imageCenterY;
	private boolean writeNow;

	private double toolMinimumPenUpMove=2.0;
	private double toolMinimumPenDownMove=1.0;
	private double toolMinimumPenUpMoveSq;
	private double toolMinimumPenDownMoveSq;
	
	@Override
	public String getName() { return "DXF"; }
	
	@Override
	public FileNameExtensionFilter getFileNameFilter() {
		return filter;
	}

	@Override
	public boolean canLoad(String filename) {
		String ext = filename.substring(filename.lastIndexOf('.'));
		return (ext.equalsIgnoreCase(".dxf"));
	}

	@Override
	public boolean canSave(String filename) {
		String ext = filename.substring(filename.lastIndexOf('.'));
		return (ext.equalsIgnoreCase(".dxf"));
	}

	@Override
	public boolean load(InputStream in,MakelangeloRobot robot) {
		final JCheckBox checkScale = new JCheckBox(Translator.get("DXFScaleOnLoad"));
		final JCheckBox checkInfill = new JCheckBox(Translator.get("DXFInfillOnLoad"));
		final JCheckBox checkOptimize = new JCheckBox(Translator.get("DXFOptimizeOnLoad"));
		final SelectFloat chooseMinimumPenUpMove = new SelectFloat((float)toolMinimumPenUpMove);
		final SelectFloat chooseMinimumPenDownMove = new SelectFloat((float)toolMinimumPenDownMove);

		JPanel panel = new JPanel(new GridLayout(0, 1));
		panel.add(checkScale);
		//panel.add(checkInfill);
		panel.add(checkOptimize);
		panel.add(new JLabel(Translator.get("Minimum pen up move (mm)")));
		panel.add(chooseMinimumPenUpMove);
		panel.add(new JLabel(Translator.get("Minimum pen down move (mm)")));
		panel.add(chooseMinimumPenDownMove);

		checkScale.setSelected(shouldScaleOnLoad);
		//checkInfill.setSelected(shouldInfillOnLoad);
		checkOptimize.setSelected(shouldOptimizePathingOnLoad);

		int result = JOptionPane.showConfirmDialog(robot.getControlPanel(), panel, getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			shouldScaleOnLoad = checkScale.isSelected();
			shouldInfillOnLoad = checkInfill.isSelected();
			shouldOptimizePathingOnLoad = checkOptimize.isSelected();

			toolMinimumPenUpMove = ((Number)chooseMinimumPenUpMove.getValue()).doubleValue();
			toolMinimumPenDownMove = ((Number)chooseMinimumPenDownMove.getValue()).doubleValue();
			if(toolMinimumPenUpMove<0) toolMinimumPenUpMove=0;
			if(toolMinimumPenDownMove<0.1) toolMinimumPenUpMove=0.1;
			
			toolMinimumPenUpMoveSq = toolMinimumPenUpMove*toolMinimumPenUpMove;
			toolMinimumPenDownMoveSq = toolMinimumPenDownMove*toolMinimumPenDownMove;
			
			return loadNow(in,robot);
		}
		return false;
	}

	
	// count all entities in all layers
	@SuppressWarnings("unchecked")
	protected void countAllEntities(DXFDocument doc) {
		Iterator<DXFLayer> layerIter = (Iterator<DXFLayer>) doc.getDXFLayerIterator();
		int entityTotal = 0;
		while (layerIter.hasNext()) {
			DXFLayer layer = (DXFLayer) layerIter.next();
			int color = layer.getColor();
			System.out.println("Found layer " + layer.getName() + "(RGB="+color+")");
			Iterator<String> entityIter = (Iterator<String>) layer.getDXFEntityTypeIterator();
			while (entityIter.hasNext()) {
				String entityType = (String) entityIter.next();
				List<DXFEntity> entityList = (List<DXFEntity>) layer.getDXFEntities(entityType);
				System.out.println("Found " + entityList.size() + " of type " + entityType);
				entityTotal += entityList.size();
			}
		}
		System.out.println(entityTotal + " total entities.");
	}

	
	/**
	 * Put every entity into a bucket.
	 * @param doc
	 * @param grid
	 * @param groups
	 */
	@SuppressWarnings("unchecked")
	protected void sortEntitiesIntoBucketsAndGroups(DXFDocument doc,DXFLayer layer,DXFBucketGrid grid,List<DXFGroup> groups) {
		//Log.info("Sorting layer "+layer.getName()+" into buckets...");

		Iterator<String> entityTypeIter = (Iterator<String>) layer.getDXFEntityTypeIterator();
		while (entityTypeIter.hasNext()) {
			String entityType = (String) entityTypeIter.next();
			List<DXFEntity> entityList = (List<DXFEntity>)layer.getDXFEntities(entityType);
			Iterator<DXFEntity> iter = entityList.iterator();
			while(iter.hasNext()) {
				DXFEntity e = iter.next();
				DXFBucketEntity be = new DXFBucketEntity(e);
				
				if (e.getType().equals(DXFConstants.ENTITY_TYPE_LINE)) {
					DXFLine line = (DXFLine)e;
					grid.addEntity(be, line.getStartPoint());
					grid.addEntity(be, line.getEndPoint());
					continue;
				}
				if(e.getType().equals(DXFConstants.ENTITY_TYPE_SPLINE)) {
					e = DXFSplineConverter.toDXFPolyline((DXFSpline)e);
					// fall through to the next case, polyline.
				}
				if(e.getType().equals(DXFConstants.ENTITY_TYPE_POLYLINE)) {
					DXFPolyline polyLine = (DXFPolyline)e;
					
					if(!polyLine.isClosed()) {
						grid.addEntity(be, polyLine.getVertex(0).getPoint());
						grid.addEntity(be, polyLine.getVertex(polyLine.getVertexCount()-1).getPoint());
					} else {
						grid.addEntity(be, polyLine.getVertex(0).getPoint());
						grid.addEntity(be, polyLine.getVertex(0).getPoint());
					}
					continue;
				}
				if(e.getType().equals(DXFConstants.ENTITY_TYPE_LWPOLYLINE)) {
					DXFLWPolyline polyLine = (DXFLWPolyline)e;
					if(!polyLine.isClosed()) {
						grid.addEntity(be, polyLine.getVertex(0).getPoint());
						grid.addEntity(be, polyLine.getVertex(polyLine.getVertexCount()-1).getPoint());
					} else {
						grid.addEntity(be, polyLine.getVertex(0).getPoint());
						grid.addEntity(be, polyLine.getVertex(0).getPoint());
					}
					continue;
				}
				//if(e.getType().equals(DXFConstants.ENTITY_TYPE_ARC)) {}
				//if(e.getType().equals(DXFConstants.ENTITY_TYPE_CIRCLE)) {}
				// I don't know this entity type.
				Log.error("Unknown DXF type "+e.getType());
			}
		}
		
		//grid.countEntitiesInBuckets();
	}
	
	
	/**
	 * 
	 * @param in
	 * @param robot
	 * @return true if load is successful.
	 */
	@SuppressWarnings("unchecked")
	private boolean loadNow(InputStream in,MakelangeloRobot robot) {
		Log.info(Translator.get("FileTypeDXF2")+"...");

		// Read in the DXF file
		Parser parser = ParserBuilder.createDefaultParser();
		try {
			parser.parse(in, DXFParser.DEFAULT_ENCODING);
		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
		DXFDocument doc = parser.getDocument();
		Bounds bounds = doc.getBounds();
		imageCenterX = (bounds.getMaximumX() + bounds.getMinimumX()) / 2.0;
		imageCenterY = (bounds.getMaximumY() + bounds.getMinimumY()) / 2.0;
		// find the scale to fit the image on the paper without
		// altering the aspect ratio
		double imageWidth  = (bounds.getMaximumX() - bounds.getMinimumX());
		double imageHeight = (bounds.getMaximumY() - bounds.getMinimumY());
		double paperHeight = robot.getSettings().getMarginHeight();
		double paperWidth  = robot.getSettings().getMarginWidth ();

		scale = 1;
		if(shouldScaleOnLoad) {
			double innerAspectRatio = imageWidth / imageHeight;
			double outerAspectRatio = paperWidth / paperHeight;
			scale = (innerAspectRatio >= outerAspectRatio) ?
					(paperWidth / imageWidth) :
					(paperHeight / imageHeight);
		}

		// prepare for exporting
		machine = robot.getSettings();
		turtle = new Turtle();

		previousX = machine.getHomeX();
		previousY = machine.getHomeY();
		turtle.setX(previousX);
		turtle.setY(previousY);
		

		// convert each entity
		Iterator<DXFLayer> layerIter = doc.getDXFLayerIterator();
		while (layerIter.hasNext()) {
			DXFLayer layer = (DXFLayer) layerIter.next();
			int color = layer.getColor();
			System.out.println("Found layer " + layer.getName() + "(RGB="+color+")");
			
			// Some DXF layers are empty.  Only write the tool change command if there's something on this layer.
			Iterator<String> entityTypeIter = (Iterator<String>)layer.getDXFEntityTypeIterator();
			if (!entityTypeIter.hasNext()) {
				continue;
			}

			turtle.setColor(new ColorRGB(color));
			
			// Sort the entities on this layer into the buckets.
			// Buckets are arranged in an XY grid.
			// All non-closed entities would appear in two buckets.
			// One Entity might be in the same bucket twice.
			Point topLeft = new Point();
			Point bottomRight = new Point();
			topLeft.setX(bounds.getMinimumX());
			topLeft.setY(bounds.getMinimumY());
			bottomRight.setX(bounds.getMaximumX());
			bottomRight.setY(bounds.getMaximumY());
			DXFBucketGrid grid = new DXFBucketGrid(15,15,topLeft,bottomRight);
			List<DXFGroup> groups = new LinkedList<DXFGroup>();

			sortEntitiesIntoBucketsAndGroups(doc,layer,grid,groups);

			//DXFGroup infillGroup=null;
			if(shouldInfillOnLoad) {
				//infillGroup = infillClosedAreas(layer);
			}

			if(shouldOptimizePathingOnLoad) {
				double DXF_EPSILON = 0.1;
				
				// Use the buckets to narrow the search field and find neighboring entities
				grid.sortEntitiesIntoContinguousGroups(groups,DXF_EPSILON);
			} else {
				grid.dumpEverythingIntoABucket(groups);
			}
			
			removeDuplicates(groups);

			// We have a list of groups. 
			// Each group is set of lines that make a continuous path.
			// Maybe even a closed path!
			// Some of the lines in each group may be flipped. 

			// TODO fill in the closed groups if the user says ok. (does not belong in DXF)
			
			//if(infillGroup!=null) {
			//	groups.add(infillGroup);
			//}
			
			Iterator<DXFGroup> groupIter = groups.iterator();
			while(groupIter.hasNext()) {
				DXFGroup g = groupIter.next();
				
				double px=previousX;
				double py=previousY;
				
				// each separate line might be forwards or backwards.
				// scan backward through the group to find the starting of the first line (previousX,previousY).
				writeNow=false;
				Iterator<DXFBucketEntity> ents = g.entities.descendingIterator();
				while(ents.hasNext()) {
					parseEntity(ents.next().entity);
				}
				
				previousX=px;
				previousY=py;

				// work forward through the loop, writing as you go.
				writeNow=true;
				ents = g.entities.iterator();
				while(ents.hasNext()) {
					parseEntity(ents.next().entity);
				}
			}
			//turtle.penUp();
		}

		robot.setTurtle(turtle);
		return true;
	}

	protected void parseEntity(DXFEntity e) {
		if (e.getType().equals(DXFConstants.ENTITY_TYPE_LINE)) {
			parseDXFLine((DXFLine)e);
		} else if (e.getType().equals(DXFConstants.ENTITY_TYPE_SPLINE)) {
			DXFPolyline polyLine = DXFSplineConverter.toDXFPolyline((DXFSpline)e);

		    
			parseDXFPolyline(polyLine);
		} else if (e.getType().equals(DXFConstants.ENTITY_TYPE_POLYLINE)
				|| e.getType().equals(DXFConstants.ENTITY_TYPE_LWPOLYLINE)) {
			parseDXFPolyline((DXFPolyline)e);
		}
	}
	
	protected double distanceFromPrevious(Point p) {
		double dx = previousX - p.getX();
		double dy = previousY - p.getY();
		return dx*dx+dy*dy;
	}
	
	protected Point getEntityStart(DXFEntity e) {
		if (e.getType().equals(DXFConstants.ENTITY_TYPE_LINE)) {
			DXFLine line = (DXFLine)e;
			return line.getStartPoint();
		} else if (e.getType().equals(DXFConstants.ENTITY_TYPE_SPLINE)) {
			DXFPolyline line = DXFSplineConverter.toDXFPolyline((DXFSpline)e);
			DXFVertex v = line.getVertex(0);
			return new Point(v.getX(),v.getY(),v.getZ());
		} else if (e.getType().equals(DXFConstants.ENTITY_TYPE_POLYLINE)
				|| e.getType().equals(DXFConstants.ENTITY_TYPE_LWPOLYLINE)) {
			DXFPolyline line = (DXFPolyline)e;
			DXFVertex v = line.getVertex(0);
			return new Point(v.getX(),v.getY(),v.getZ());
		}
		assert(false);
		return null;
	}

	protected Point getEntityEnd(DXFEntity e) {
		if (e.getType().equals(DXFConstants.ENTITY_TYPE_LINE)) {
			DXFLine line = (DXFLine)e;
			return line.getEndPoint();
		} else if (e.getType().equals(DXFConstants.ENTITY_TYPE_SPLINE)) {
			DXFPolyline line = DXFSplineConverter.toDXFPolyline((DXFSpline)e);
			int n=0;
			if(!line.isClosed()) n=line.getVertexCount()-1;
			DXFVertex v = line.getVertex(n);
			return new Point(v.getX(),v.getY(),v.getZ());
		} else if (e.getType().equals(DXFConstants.ENTITY_TYPE_POLYLINE)
				|| e.getType().equals(DXFConstants.ENTITY_TYPE_LWPOLYLINE)) {
			DXFPolyline line = (DXFPolyline)e;
			int n=0;
			if(!line.isClosed()) n=line.getVertexCount()-1;
			DXFVertex v = line.getVertex(n);
			return new Point(v.getX(),v.getY(),v.getZ());
		}
		assert(false);
		return null;
	}
	
	/**
	 * http://stackoverflow.com/questions/203984/how-do-i-remove-repeated-elements-from-arraylist
	 * @param groups
	 */
	protected void removeDuplicates(List<DXFGroup> groups) {
		int totalRemoved=0;
		
		Iterator<DXFGroup> g = groups.iterator();
		while(g.hasNext()) {
			DXFGroup group = g.next();
			int before = group.entities.size();
			Set<DXFBucketEntity> hs = new LinkedHashSet<>();
			hs.addAll(group.entities);
			group.entities.clear();
			group.entities.addAll(hs);
			int after = group.entities.size();
			totalRemoved += before - after;
		}
		if(totalRemoved!=0) System.out.println(totalRemoved+" duplicates removed.");
	}
	
	protected double TX(double x) {
		return (x-imageCenterX) * scale;
	}
	protected double TY(double y) {
		return (y-imageCenterY) * scale;
	}
	
	protected void parseDXFLine(DXFLine entity) {
		Point start = entity.getStartPoint();
		Point end = entity.getEndPoint();

		double x = TX(start.getX());
		double y = TY(start.getY());
		double x2 = TX(end.getX());
		double y2 = TY(end.getY());
			
		// which end is closer to the previous point?
		double dx = previousX - x;
		double dy = previousY - y;
		double dx2 = previousX - x2;
		double dy2 = previousY - y2;
		if ( dx * dx + dy * dy < dx2 * dx2 + dy2 * dy2 ) {
			parseDXFLineEnds(x,y,x2,y2);
		} else {
			parseDXFLineEnds(x2,y2,x,y);
		}
	}
	
	protected void parseDXFLineEnds(double x,double y,double x2,double y2) {
		double dx = x - previousX;
		double dy = y - previousY;
		
		// next line start too far away?
		//if(dx*dx + dy*dy > toolMinimumStepSize ) 
		{ 
			// lift pen and move to that location
			if(writeNow) {
				boolean liftToStartNextLine = (dx * dx + dy * dy > toolMinimumPenUpMoveSq);
				if(liftToStartNextLine) {
					turtle.jumpTo(x,y);
				} else {
					turtle.penDown();
					turtle.moveTo(x,y);
				}
			}
			previousX = x;
			previousY = y;
		}

		dx = x2 - previousX;
		dy = y2 - previousY;
		
		if(dx*dx + dy*dy > toolMinimumPenDownMoveSq ) {
			// lower pen and draw to end of line
			if(writeNow) {
				turtle.penDown();
				turtle.moveTo(x2,y2);
			}
			previousX = x2;
			previousY = y2;
		}
	}

	
	protected void parseDXFPolyline(DXFPolyline entity) {
		if(entity.isClosed()) {
			// only one end to care about
			parseDXFPolylineForward(entity);
		} else {
			// which end is closest to the previous (x,y)?
			int n = entity.getVertexCount()-1;
			double x = TX(entity.getVertex(0).getX());
			double y = TY(entity.getVertex(0).getY());
			double x2 = TX(entity.getVertex(n).getX());
			double y2 = TY(entity.getVertex(n).getY());

			// which end is closer to the previous (x,y) ?
			double dx = x - previousX;
			double dy = y - previousY;
			double dx2 = x2 - previousX;
			double dy2 = y2 - previousY;
			if ( dx * dx + dy * dy < dx2 * dx2 + dy2 * dy2 ) {
				// first point is closer
				parseDXFPolylineForward(entity);
			} else {
				// last point is closer
				parseDXFPolylineBackward(entity);
			}
		}
	}
	
	protected void parseDXFPolylineForward(DXFPolyline entity) {
		boolean first = true;
		int c = entity.getVertexCount();
		int count = c + (entity.isClosed()?1:0);
		DXFVertex v;
		double x,y;
		for (int j = 0; j < count; ++j) {
			v = entity.getVertex(j % c);
			x = TX(v.getX());
			y = TY(v.getY());
			parsePolylineShared(x,y,first,j<count-1);
			first = false;
		}
	}
	
	protected void parseDXFPolylineBackward(DXFPolyline entity) {
		boolean first = true;
		int c = entity.getVertexCount();
		int count = c + (entity.isClosed()?1:0);
		DXFVertex v;
		double x,y;
		for (int j = 0; j < count; ++j) {
			v = entity.getVertex((c*2-1-j) % c);
			x = TX(v.getX());
			y = TY(v.getY());
			parsePolylineShared(x,y,first,j<count-1);
			first = false;
		}
	}
	
	protected void parsePolylineShared(double x,double y,boolean first,boolean notLast) {
		double dx = x - previousX;
		double dy = y - previousY;

		if (first == true) {
			if(dx*dx + dy*dy > toolMinimumPenDownMoveSq ) { 
				// line does not start at last tool location, lift and move.
				if(writeNow) {
					boolean liftToStartNextLine = (dx * dx + dy * dy > toolMinimumPenUpMoveSq); 
					if(liftToStartNextLine) {
						turtle.jumpTo(x,y);
					} else {
						turtle.penDown();
						turtle.moveTo(x,y);
					}
				}
				previousX = x;
				previousY = y;
			}
			// else line starts right here, pen is down, do nothing extra.
		} else {
			// not the first point, draw.
			if (!notLast || dx * dx + dy * dy > toolMinimumPenDownMoveSq) {
				if(writeNow) {
					turtle.penDown();
					turtle.moveTo(x,y);
				}
				previousX = x;
				previousY = y;
			}
		}
	}
	
	@Override
	/**
	 * see http://paulbourke.net/dataformats/dxf/min3d.html for details
	 * @param outputStream where to write the data
	 * @param robot the robot from which the data is obtained
	 * @return true if save succeeded.
	 */
	public boolean save(OutputStream outputStream, MakelangeloRobot robot) {
		Log.info("saving...");
		Turtle turtle = robot.getTurtle();
		MakelangeloRobotSettings settings = robot.getSettings();
		
		try(OutputStreamWriter out = new OutputStreamWriter(outputStream)) {
			// header
			out.write("999\nDXF created by Makelangelo software (http://makelangelo.com)\n");
			out.write("0\nSECTION\n");
			out.write("2\nHEADER\n");
			out.write("9\n$ACADVER\n1\nAC1006\n");
			out.write("9\n$INSBASE\n");
			out.write("10\n"+settings.getPaperLeft()+"\n");
			out.write("20\n"+settings.getPaperBottom()+"\n");
			out.write("30\n0.0\n");
			out.write("9\n$EXTMIN\n");
			out.write("10\n"+settings.getPaperLeft()+"\n");
			out.write("20\n"+settings.getPaperBottom()+"\n");
			out.write("30\n0.0\n");
			out.write("9\n$EXTMAX\n");
			out.write("10\n"+settings.getPaperRight()+"\n");
			out.write("20\n"+settings.getPaperTop()+"\n");
			out.write("30\n0.0\n");
			out.write("0\nENDSEC\n");

			// tables section
			out.write("0\nSECTION\n");
			out.write("2\nTABLES\n");
			// line type
			out.write("0\nTABLE\n");
			out.write("2\nLTYPE\n");
			out.write("70\n1\n");
			out.write("0\nLTYPE\n");
			out.write("2\nCONTINUOUS\n");
			out.write("70\n64\n");
			out.write("3\nSolid line\n");
			out.write("72\n65\n");
			out.write("73\n0\n");
			out.write("40\n0.000\n");
			out.write("0\nENDTAB\n");
			// layers
			out.write("0\nTABLE\n");
			out.write("2\nLAYER\n");
			out.write("70\n6\n");
			out.write("0\nLAYER\n");
			out.write("2\n1\n");
			out.write("70\n64\n");
			out.write("62\n7\n");
			out.write("6\nCONTINUOUS\n");
			out.write("0\nLAYER\n");
			out.write("2\n2\n");
			out.write("70\n64\n");
			out.write("62\n7\n");
			out.write("6\nCONTINUOUS\n");
			out.write("0\nENDTAB\n");
			out.write("0\nTABLE\n");
			out.write("2\nSTYLE\n");
			out.write("70\n0\n");
			out.write("0\nENDTAB\n");
			// end tables
			out.write("0\nENDSEC\n");

			// empty blocks section (good form?)
			out.write("0\nSECTION\n");
			out.write("0\nBLOCKS\n");
			out.write("0\nENDSEC\n");
			// now the lines
			out.write("0\nSECTION\n");
			out.write("2\nENTITIES\n");

			boolean isUp=true;
			double x0 = settings.getHomeX();
			double y0 = settings.getHomeY();
			
			String matchUp = settings.getPenUpString();
			String matchDown = settings.getPenDownString();
			
			if(matchUp.contains(";")) {
				matchUp = matchUp.substring(0, matchUp.indexOf(";"));
			}
			matchUp = matchUp.replaceAll("\n", "");

			if(matchDown.contains(";")) {
				matchDown = matchDown.substring(0, matchDown.indexOf(";"));
			}
			matchDown = matchDown.replaceAll("\n", "");
			
			
			for( Turtle.Movement m : turtle.history ) {
				switch(m.type) {
				case TRAVEL:
					isUp=true;
					x0=m.x;
					y0=m.y;
					break;
				case DRAW:
					if(isUp) isUp=false;
					else {
						out.write("0\nLINE\n");
						out.write("8\n1\n");  // layer 1
						out.write("10\n"+MathHelper.roundOff3(x0)+"\n");
						out.write("20\n"+MathHelper.roundOff3(-y0)+"\n");
						out.write("11\n"+MathHelper.roundOff3(m.x)+"\n");
						out.write("21\n"+MathHelper.roundOff3(-m.y)+"\n");
					}
					x0=m.x;
					y0=m.y;
					
					break;
				case TOOL_CHANGE:
					// TODO new layer?  m.getColor()
					break;
				}
			}
			// wrap it up
			out.write("0\nENDSEC\n");
			out.write("0\nEOF\n");
			out.flush();
		}
		catch(IOException e) {
			Log.error(Translator.get("SaveError") +" "+ e.getLocalizedMessage());
			return false;
		}
		
		Log.info("done.");
		return true;
	}

	@Override
	public boolean canLoad() {
		return true;
	}

	@Override
	public boolean canSave() {
		return true;
	}
}
