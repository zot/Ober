package ar.ober.swt;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class OberSwtDragger extends Canvas implements PropertyChangeListener, MouseMoveListener, MouseListener  {
	protected Point offset;
	protected Tracker tracker;
	protected Tracker activeTracker;
	protected OberSwtViewer viewer;

	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(Math.min(10, wHint), Math.min(10, hHint));
	}
	public OberSwtDragger(OberSwtViewer viewer, Composite parent, int style) {
		super(parent, style);
		FillLayout l = new FillLayout();
		
		this.viewer = viewer;
		setLayout(l);
		tracker = new Tracker(this);
		tracker.addMouseListener(this);
		tracker.addMouseMoveListener(this);
	}
	public void mouseMove(MouseEvent e) {
		if (activeTracker != null) {
			((OberSwtLayout)activeTracker.getParent().getLayout()).constrain(tracker.toDisplay(e.x + offset.x, e.y + offset.y), this);
		}
	}
	public void mouseDoubleClick(MouseEvent e) {}
	public void mouseDown(MouseEvent e) {
		offset = new Point(-e.x, -e.y);
		Composite container = splitterContainer();
		Point location = getLocation();
		Rectangle bounds = getBounds();

		activeTracker = new Tracker(container);
		activeTracker.draw = true;
		activeTracker.moveAbove(null);
		tracker.tracking = activeTracker.tracking = true;
		activeTracker.setBounds(location.x, location.y, bounds.width, bounds.height);
		((OberSwtLayout)activeTracker.getParent().getLayout()).constrain(toDisplay(0, 0), this);
	}
	public void mouseUp(MouseEvent e) {
		tracker.tracking = false;
		Composite oldOwner = viewer.wrapper.getParent();
		Composite splitterContainer = splitterContainer();
		Composite newOwner;
		OberSwtLayout layout = (OberSwtLayout)activeTracker.getParent().getLayout();
		Point wrapperLoc = viewer.wrapper.getParent().toDisplay(viewer.wrapper.getLocation());
		Point loc = getParent().toDisplay(getLocation());
		loc.x -= wrapperLoc.x;
		loc.y -= wrapperLoc.y;
		float position = layout.vertical ? (activeTracker.getBounds().y - loc.y) / (float)activeTracker.getParent().getBounds().height : (activeTracker.getBounds().x - loc.x) / (float)activeTracker.getParent().getBounds().width;

		if (splitterContainer != activeTracker.getParent()) {
			viewer.copyTextViewer(layout.viewer, position);
			viewer.removeFromParent();
			while (splitterContainer.getParent() != null) {
				splitterContainer = splitterContainer.getParent();
			}
			splitterContainer.redraw();
		} else {
			layout.setPosition(viewer, position);
		}
		newOwner = activeTracker.getParent();
		activeTracker.dispose();
		activeTracker = null;
		if (oldOwner != newOwner) {
			oldOwner.getParent().layout();
		}
	}
	public Composite splitterContainer() {
		Composite comp = this;

		do {
			comp = comp.getParent();
		} while (comp != null && !(comp.getLayout() instanceof OberSwtLayout));
		return comp;
	}
	public void propertyChange(PropertyChangeEvent evt) {
		redraw();
	}
	public static class Tracker extends Composite implements PaintListener, MouseTrackListener {
		boolean tracking = false;
		boolean draw = false;

		public Tracker(Composite parent) {
			super(parent, 0);
			addPaintListener(this);
			addMouseTrackListener(this);
		}
		public void paintControl(PaintEvent e) {
			Rectangle bounds = getBounds();

			e.gc.setBackground(Display.getCurrent().getSystemColor(draw ? SWT.COLOR_GREEN : SWT.COLOR_WIDGET_BACKGROUND));
			e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
			e.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			e.gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
		}
		public void mouseEnter(MouseEvent e) {
			draw = true;
			redraw();
		}
		public void mouseExit(MouseEvent e) {
			draw = false;
			redraw();
		}
		public void mouseHover(MouseEvent e) {}
	}
}
