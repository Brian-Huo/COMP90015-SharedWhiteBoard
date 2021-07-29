package pb.app;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Class to maintain whiteboard information. You should probably modify this
 * class.
 * @author aaron
 *
 */
public class Whiteboard {
	private static Logger log = Logger.getLogger(Whiteboard.class.getName());

	/**
	 * Paths for this whiteboard.
	 */
	private ArrayList<WhiteboardPath> paths;
	
	/**
	 * Name of the whiteboard, peer:port:boarid
	 */
	private String name;
	
	/**
	 * The current version number of this whiteboard.
	 */
	private long version;
	
	/**
	 * Whether this whiteboard is being shared or not. Only relevant
	 * for boards that are created locally.
	 */
	private boolean shared=false;
	
	/**
	 * Whether this whiteboard is a remote board, i.e. not created
	 * locally but rather being managed on another peer.
	 */
	private boolean remote=false;
	
	/**
	 * Initialize the whiteboard.
	 * @param remote is true if the whiteboard is remotely managed, otherwise
	 * the whiteboard is locally managed.
	 */
	public Whiteboard(String name,boolean remote) {
		paths = new ArrayList<>();
		this.name=name;
		this.version=0;
		this.remote=remote;
	}
	
	/**
	 * Initialize a whiteboard from a string.
	 * 
	 * @param name the board name, i.e. peer:port:boardid
	 * @param data the board data, i.e. version%PATHS 
	 */
	public void whiteboardFromString(String name,String data) {
		String[] parts = data.split("%");
		paths = new ArrayList<>();
		this.name=name;
		version=-1;
		if(parts.length<1) {
			log.severe("whiteboard data is malformed: "+data);
			return;
		}
		try {
			version=Integer.parseInt(parts[0]);
		} catch (NumberFormatException e) {
			log.severe("whiteboard data is malformed: "+data);
			return;
		}
		if(parts.length>1) {
			for (int i = 1; i < parts.length; i++) {
				String path = parts[i];
				if (path.length() > 0) {
					paths.add(new WhiteboardPath(path));
				}
			}
		}
	}
	
	/**
	 * Convert this whiteboard to a string.
	 * 
	 * @return "name%version%" if the whiteboard has no paths or
	 *         "name%version%PATHS" for the case when there are one or more paths,
	 *         where each path is separated by a "%"
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append(getNameAndVersion());
		if(paths.size()==0) 
			sb.append("%");
		else {
			for (int i = 0; i < paths.size(); i++) {
				sb.append("%"+paths.get(i));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Draw the white board on the drawing area. Clears the draw
	 * area and draws all paths.
	 * @param drawArea
	 */
	public void draw(DrawArea drawArea) {
		drawArea.clear();
		for(WhiteboardPath path : paths) {
			drawArea.drawPath(path);
		}
	}
	
	////
	// Methods that update the version of the board
	////
	
	/**
	 * Add a path to the whiteboard.
	 * @param newPath
	 * @param versionBeingUpdated should be the board version that the update applies to
	 * @return true if the update was accepted, false if it was rejected
	 */
	public synchronized boolean addPath(WhiteboardPath newPath,long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		paths.add(newPath);
		this.version++;
		return true;
	}
	
	/**
	 * Clear the board of all paths.
	 * @param versionBeingUpdated should be the board version that the update applies to
	 * @return true if the update was accepted, false if it was rejected
	 */
	public synchronized boolean clear(long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		paths.clear();
		this.version++;
		return true;
	}
	
	/**
	 * Remove the last path from the board.
	 */
	public synchronized boolean undo(long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		if(paths.size()>0) {
			paths.remove(paths.size()-1);
		}
		this.version++;
		return true;
	}
	
	/**
	 * 
	 * @return peer:port:boardid%version
	 */
	public String getNameAndVersion() {
		return getName()+"%"+getVersion();
	}
	
	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * @return true if the board is shared, false otherwise
	 */
	public boolean isShared() {
		return shared;
	}
	
	/**
	 * Set the shared status of the board
	 * @param shared
	 */
	public void setShared(boolean shared) {
		this.shared=shared;
	}

	/**
	 * 
	 * @return the version of the board
	 */
	public long getVersion() {
		return version;
	}
	
	/**
	 * 
	 * @return whether the board is maintained remotely or not
	 */
	public boolean isRemote() {
		return remote;
	}
	
}
