package pb.app;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Class for maintaining a path.
 * You probably don't need to modify this class.
 * @author aaron
 *
 */
public class WhiteboardPath {
	private static Logger log = Logger.getLogger(WhiteboardPath.class.getName());
	
	/**
	 * List of points in the path.
	 */
	ArrayList<WhiteboardPoint> points;
	
	/**
	 * Color of the path.
	 */
	Color color;
	
	/**
	 * Create a new path with a color.
	 * @param color
	 */
	public WhiteboardPath(Color color) {
		this.color=color;
		points=new ArrayList<>();
	}
	
	/**
	 * Initialize a path from a string, in the format color>POINTS, where
	 * POINTS has format point>point>....
	 * @param data
	 */
	public WhiteboardPath(String data) {
		String[] parts = data.split(">");
		points=new ArrayList<>();
		this.color=Color.black;
		if(parts.length>=1) {
			color=parseColor(parts[0]);
			for(int i=1;i<parts.length;i++) {
				points.add(new WhiteboardPoint(parts[i]));
			}
		}
	}
	
	/**
	 * Add a point to the class.
	 * @param x
	 * @param y
	 */
	public void addPoint(int x, int y) {
		points.add(new WhiteboardPoint(x,y));
	}
	
	/**
	 * 
	 * @return the length of the path
	 */
	public int length() {
		return points.size();
	}
	
	/**
	 * Draw the path on the given graphics resource.
	 * @param g2
	 */
	public void drawOnBoard(Graphics2D g2) {
		if(points.size()<=1) {
			return;
		}
		g2.setPaint(color);
		for(int i=1;i<points.size();i++) {
			g2.drawLine(points.get(i-1).x, points.get(i-1).y,
					points.get(i).x, points.get(i).y);
		}
	}
	
	/**
	 * 
	 * @return the path as a string in the format color>POINTS
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(colorString()+">");
		for(int i=0;i<points.size();i++) {
			sb.append(points.get(i).toString());
			if(i!=points.size()-1) {
				sb.append(">");
			}
		}
		return sb.toString();
	}
	
	/*
	 * Private methods to format/parse color value.
	 */
	
	private Color parseColor(String data) {
		switch(data) {
		case "black": return Color.black;
		case "red": return Color.red;
		default: log.warning("color defaulting to black");
			return Color.black;
		}
	}
	
	private String colorString() {
		if(color==Color.black) return "black";
		if(color==Color.red) return "red";
		log.warning("color defaulting to black");
		return "black";
	}
}
