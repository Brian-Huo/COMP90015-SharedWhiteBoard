package pb.app;

import java.util.logging.Logger;

/**
 * Utility class to store a point.
 * You probably don't need to change this class.
 * @author aaron
 *
 */
public class WhiteboardPoint {
	private static Logger log = Logger.getLogger(WhiteboardPoint.class.getName());
	
	/**
	 * x coordinate
	 */
	public int x;
	
	/**
	 * y coordinate
	 */
	public int y;
	
	/**
	 * Initialize the point.
	 * @param x
	 * @param y
	 */
	public WhiteboardPoint(int x, int y) {
		this.x=x;
		this.y=y;
	}

	/**
	 * Initialize point from a string with format x,y
	 * @param data
	 */
	public WhiteboardPoint(String data) {
		String[] parts=data.split(",");
		if(parts.length==2) {
			try {
				x=Integer.parseInt(parts[0]);
				y=Integer.parseInt(parts[1]);
				return;
			} catch (NumberFormatException e) {
				
			}
		}
		log.severe("invalid point ["+data+"] defaulting to (0,0)");
		x=0;
		y=0;
	}

	/**
	 * @return point as a string in format x,y
	 */
	public String toString() {
		return x+","+y;
	}
}
