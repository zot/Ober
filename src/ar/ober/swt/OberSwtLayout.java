package ar.ober.swt;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

public class OberSwtLayout extends Layout {
	protected boolean vertical;
	protected ArrayList controls = new ArrayList();
	protected float positions[] = new float[8];
	protected OberSwtViewer viewer;

	public OberSwtLayout(OberSwtViewer vw, boolean vert) {
		viewer = vw;
		vertical = vert;
	}
	public void insert(Control control) {
		float max = 0;
		int maxIndex = 0;
		float position;

		for (int i = 0; i < controls.size(); i++) {
			float next = (i == controls.size() - 1 ? 1 : positions[i + 1]);

			if (next - positions[i] > max) {
				max = next - positions[i];
				maxIndex = i;
			}
		}
		addControl(control, Math.min(controls.size(), maxIndex + 1), (positions[maxIndex] + (maxIndex == controls.size() - 1 ? 1 : positions[maxIndex + 1])) / 2);
	}
	public void addControl(Control control, float position) {
		int index = controls.size();

		for (int i = 0; i < controls.size(); i++) {
			float next = (i == controls.size() - 1 ? 1 : positions[i + 1]);

			if (positions[i] > position) {
				index = i;
				break;
			}
		}
		addControl(control, index, position);
	}
	public void addControl(Control control, int index, float position) {
		float newPositions[] = positions;

		controls.add(index, control);
		while (newPositions.length < controls.size()) {
			newPositions = new float[newPositions.length * 2];
		}
		if (index > 0) {
			System.arraycopy(positions, 0, newPositions, 0, index);
		}
		if (controls.size() - index - 1 > 0) {
			System.arraycopy(positions, index, newPositions, index + 1, controls.size() - index - 1);
		}
		newPositions[index] = position;
		positions = newPositions;
		positions[0] = 0;
		if (control.getParent() != viewer.component && !control.setParent(viewer.component)) {
			System.out.println("Oops, reparent failed for: " + control);
		}
		viewer.component.layout();
	}
	public void remove(Control control) {
		int index = controls.indexOf(control);

		if (controls.size() - index - 1 > 0) {
			System.arraycopy(positions, index + 1, positions, index, controls.size() - index - 1);
		}
		controls.remove(index);
		positions[0] = 0;
		viewer.component.layout();
	}
	protected Point computeSize(Composite comp, int wHint, int hHint, boolean flushCache) {
		int total = 0;
		
		for (int i = 0; i < controls.size(); i++) {
			try {
				Point size = ((Control)controls.get(i)).computeSize(wHint, hHint);
			
				total += vertical ? size.y : size.x;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return vertical ? new Point(10, Math.min(10, total)) : new Point(Math.min(10, total), 10);
	}
	protected void layout(Composite comp, boolean flushCache) {
		for (int index = 0; index < controls.size(); index++) {
			float position = positions[index];
			float size = (index == controls.size() - 1 ? 1 : positions[index + 1]) - position;
			Point vs = comp.getSize();
	
			((Control)controls.get(index)).setBounds(vertical ? 0 : Math.round(vs.x * position), vertical ? Math.round(vs.y * position) : 0, Math.round(vertical ? vs.x : vs.x * size), Math.round(vertical ? vs.y * size : vs.y));
		}
	}
	public void setPosition(OberSwtViewer viewer, float position)  {
		int index = 0;

		remove(viewer.wrapper);
		for (int i = controls.size(); i-- > 0; ) {
			if (positions[i] < position) {
				index = i + 1;
				break;
			}
		}
		addControl(viewer.wrapper, index, position);
	}
	public Control findComponentAt(Control c, Point p) {
		if (c instanceof OberSwtDragger || c instanceof OberSwtDragger.Tracker) {
			return null;
		}
		Rectangle bounds = c.getBounds();

		bounds.x = bounds.y = 0;
		if (bounds.contains(c.toControl(p))) {
			if (c instanceof Composite) {
				Composite comp = (Composite)c;
				Control children[] = comp.getChildren();
			
				for (int i = 0; i < children.length; i++) {
					c = findComponentAt(children[i], p);
					if (c != null) {
						break;
					}
				}
				return c;
			}
		}
		return null;
	}
	public void constrain(Point displayPoint, OberSwtDragger widget) {
		Point splitterPoint = widget.viewer.wrapper.getParent().toControl(displayPoint);
		Control target = findComponentAt(widget.getShell(), displayPoint);
		Composite targetParent = widget.activeTracker.getParent();

		if (target != null) {
			OberSwtLayout contLayout = (OberSwtLayout)widget.activeTracker.getParent().getLayout();

			if (target instanceof Composite) {
				targetParent = (Composite)target;
			} else {
				targetParent = target.getParent();
			}
			while (targetParent != null) {
				if (targetParent.getLayout() instanceof OberSwtLayout && ((OberSwtLayout)targetParent.getLayout()).vertical == contLayout.vertical) {
					break;
				}
				targetParent = targetParent.getParent();
			}
			if (targetParent != null && targetParent != widget.activeTracker.getParent()) {
				Rectangle r = widget.activeTracker.getBounds();

				widget.activeTracker.dispose();
				widget.activeTracker = new OberSwtDragger.Tracker(targetParent);
				widget.activeTracker.draw = true;
				widget.activeTracker.tracking = true;
				widget.activeTracker.moveAbove(null);
				widget.activeTracker.setBounds(r);
			} else {
				targetParent = widget.activeTracker.getParent();
			}
		}
		if (vertical) {
			widget.activeTracker.setLocation(widget.getLocation().x, Math.max(0, Math.min(splitterPoint.y, targetParent.getBounds().height - widget.activeTracker.getBounds().height)));
		} else {
			widget.activeTracker.setLocation(Math.max(0, Math.min(splitterPoint.x, widget.activeTracker.getParent().getBounds().width - widget.activeTracker.getBounds().width)), widget.getLocation().y);
		}
	}
}
