/*
(C) 2003 Bill Burdick

ar.ober.OberDragWidget

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober.swing;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;

public class OberDragWidget extends JComponent implements MouseListener, MouseMotionListener, PropertyChangeListener {
	public class Tracker extends Canvas {
		public Tracker() {
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
		}
		public void paint(Graphics g) {
			g.setColor(Color.GREEN);
			g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
			getBorder().paintBorder(tracker, g, 1, 1, getWidth() - 2, getHeight() - 2);
		}
	}

	protected Point offset;
	protected Tracker tracker = new Tracker();
	protected OberSwingViewer viewer;

	public OberDragWidget(OberSwingViewer v) {
		viewer = v;
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		addMouseListener(this);
		addMouseMotionListener(this);
		setMinimumSize(new Dimension(10, 10));
	}
	public void propertyChange(PropertyChangeEvent evt) {
		repaint();
	}
	public void paint(Graphics g) {
		g.setColor(viewer.isDirty() ? Color.BLACK : Color.WHITE);
		g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
		getBorder().paintBorder(tracker, g, 1, 1, getWidth() - 2, getHeight() - 2);
	}
	public Container splitterContainer() {
		Container comp = this;

		do {
			comp = comp.getParent();
		} while (comp != null && !(comp.getLayout() instanceof OberLayout));
		return comp;
	}
	public void mouseDragged(MouseEvent e) {
		((OberLayout)tracker.getParent().getLayout()).constrain(e.getX() + offset.x, e.getY() + offset.y, this, tracker);
	}
	public void mouseMoved(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		offset = new Point(-e.getX(), -e.getY());
		Point location = getLocation();

		transformPoint(location, getParent(), splitterContainer());
		tracker.setBounds(location.x, location.y, getWidth(), getHeight());
		splitterContainer().add(tracker);
	}
	public void mouseReleased(MouseEvent e) {
		Container oldOwner = viewer.getWrapper().getParent();
		Container splitterContainer = splitterContainer();
		Container newOwner;

		Point wrapperLoc = viewer.getWrapper().getLocationOnScreen();
		Point loc = getLocationOnScreen();
		loc.x -= wrapperLoc.x;
		loc.y -= wrapperLoc.y;
		if (splitterContainer != tracker.getParent()) {
			((OberLayout)tracker.getParent().getLayout()).insert(viewer, tracker.getParent(), new Point(tracker.getX() - loc.x, tracker.getY() - loc.y));
			while (splitterContainer.getParent() != null) {
				splitterContainer = splitterContainer.getParent();
			}
			splitterContainer.repaint();
		} else {
			((OberLayout)tracker.getParent().getLayout()).setPosition(viewer, new Point(tracker.getX() - loc.x, tracker.getY() - loc.y));
		}
		newOwner = tracker.getParent();
		tracker.getParent().remove(tracker);
		newOwner.validate();
		//layoutContainer(container);
		if (oldOwner != newOwner) {
			oldOwner.validate();
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	protected void transformPoint(Point windowPoint, Component container, Component top) {
		Point contLoc = container.getLocationOnScreen();
		Point topLoc = top.getLocationOnScreen();
		
		windowPoint.translate(contLoc.x - topLoc.x, contLoc.y - topLoc.y);
	}
}
