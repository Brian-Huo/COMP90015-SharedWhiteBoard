package pb.app;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.logging.Logger;

import javax.swing.JComponent;

/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 * 
 * You probably don't need to modify this class.
 */
@SuppressWarnings("serial")
public class DrawArea extends JComponent {
	private static Logger log = Logger.getLogger(DrawArea.class.getName());

	// Image in which we're going to draw
	private Image image;
	// Graphics2D object ==> used to draw on
	private Graphics2D g2;
	// Mouse coordinates
	private int currentX, currentY, oldX, oldY;
	
	private WhiteboardPath currentPath;
	
	private Color currentColor=Color.black;
	
	public DrawArea(WhiteboardApp whiteboardApp) {
		setDoubleBuffered(false);
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// save coord x,y when mouse is pressed
				oldX = e.getX();
				oldY = e.getY();
				currentPath = new WhiteboardPath(currentColor);
				currentPath.addPoint(oldX, oldY);
				if(g2!=null) {
					g2.setPaint(currentColor);
				}
			}
			
			public void mouseReleased(MouseEvent e) {
				if(currentPath!=null && currentPath.length()>1) {
					// a path has been created
					log.info("path created: "+currentPath.toString());
					whiteboardApp.pathCreatedLocally(currentPath);
					currentPath=null;
				}
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				currentX = e.getX();
				currentY = e.getY();
				if (g2 != null) {
					g2.drawLine(oldX, oldY, currentX, currentY);
					repaint();
					oldX = currentX;
					oldY = currentY;
					currentPath.addPoint(oldX, oldY);
				}
			}
		});
	}

	protected void paintComponent(Graphics g) {
		if (image == null) {
			image = createImage(getSize().width, getSize().height);
			g2 = (Graphics2D) image.getGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			clear();
		}

		g.drawImage(image, 0, 0, null);
	}
	
	/**
	 * Draw a whiteboard path on the board.
	 * @param whiteboardPath
	 */
	public void drawPath(WhiteboardPath whiteboardPath) {
		whiteboardPath.drawOnBoard(g2);
		repaint();
	}

	// now we create exposed methods
	public void clear() {
		if(g2!=null) {
			g2.setPaint(Color.white);
			// draw white on entire draw area to clear
			g2.fillRect(0, 0, getSize().width, getSize().height);
			repaint();
		}
	}

	public void setColor(Color color) {
		currentColor=color;
	}

}