package ar.ober.swt;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.SWTEventObject;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import ar.ober.Ober;
import ar.ober.OberContext;
import ar.ober.OberViewer;

public class OberSwtViewer extends OberViewer implements PaintListener, MouseListener {
	public Composite tagPanel;
	public StyledText tag;
	public Composite component;
	public Composite wrapper;
	public OberSwtDragger dragger;
	public Control currentFocus;

	public static OberSwtViewer createView(Composite parent) {
		OberSwtGui gui = new OberSwtGui();
		Ober ober = new Ober(gui);
		OberSwtViewer v = new OberSwtViewer();
		
		v.ober = ober;
		v.type = OberViewer.MAIN_TYPE;
		v.createGui(parent, -1);
		return v;
	}
	public void newFocus(Widget w) {
		boolean hasFocus = w == component || w == tag;

		if (active != hasFocus) {
			active = hasFocus;
			if (hasFocus) {
				String n[] = getFilename();

				if (ober.getActiveViewer() != null) {
					((OberSwtViewer)ober.getActiveViewer()).newFocus(w);
				}
				ober.activated(this);
				wrapper.getShell().setText("Ober: " + (n == null ? "null" : n[1]));
			}
			wrapper.redraw();
		}
		if (hasFocus) {
			currentFocus = (Control)w;
		}
	}
	public void setTagText(String text) {
		tag.setText(text);
	}
	public void setTagBackground(int color) {
		tag.setBackground(new org.eclipse.swt.graphics.Color(tag.getDisplay(), (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF));
	}
	public int getTagCaretPosition()  {
		return tag.getCaretOffset();
	}
	public boolean inTag(Object event) {
		return ((SWTEventObject)event).getSource() == tag;
	}
	public int getWidth() {
		return wrapper.getBounds().width;
	}
	public void setText(String txt) {
		((StyledText)component).setText(txt);
	}
	public String getText(int start, int length) {
		return ((StyledText)component).getText(start, length);
	}
	public int getCaretPosition() {
		return ((StyledText)component).getCaretOffset();
	}
	public int getDocumentLength() {
		return ((StyledText)component).getCharCount();
	}
	public void insertString(final int pos, final String str, final Object style) {
		if (Display.getCurrent() == null) {
			tag.getDisplay().asyncExec(new Runnable() {
				public void run() {
					insertString(pos, str, style);
				}
			});
		} else {
			int p = pos == -1 ? getDocumentLength() : pos;
			((StyledText)component).replaceTextRange(p, 0, str);
			((StyledText)component).replaceStyleRanges(p, 0, style == null ? new StyleRange[0] : new StyleRange[] {(StyleRange)style});
		}
	}
	public void setCaretPosition(int pos) {
		((StyledText)component).setCaretOffset(pos);
	}
	public void removeFromParent() {
		ober.removeViewer(this);
		((OberSwtViewer)parentViewer).layout().remove(this.wrapper);
		parentViewer.removeChild(this);
		parentViewer = null;
		wrapper.dispose();
	}
	public void requestFocus() {
		component.setFocus();
	}
	public String getTagText() {
		return tag.getText();
	}
	public boolean isDirty() {
		return ((OberSwtDocument)((StyledText)component).getContent()).isDirty();
	}
	public void markClean() {
		((OberSwtDocument)((StyledText)component).getContent()).markClean();
	}
	public void markDirty() {
		((OberSwtDocument)((StyledText)component).getContent()).markClean();
	}
	public void setTrackingChanges(boolean track) {
		((OberSwtDocument)((StyledText)component).getContent()).setTrackingChanges(track);
	}
	public void detachDocument() throws IOException, ClassNotFoundException {
		StyledText tmpTxt = new StyledText(((OberSwtViewer)parentViewer).component, 0);
		OberSwtDocument doc = new OberSwtDocument(tmpTxt.getContent());
		
		((OberSwtDocument)((StyledText)component).getContent()).removePropertyChangeListener(dragger);
		doc.addPropertyChangeListener(dragger);
		((StyledText)component).setContent(doc);
		tmpTxt.dispose();
	}
	public void openFrame() {
		final Shell shell = new Shell();
		
		shell.setText("Ober");
		shell.setBounds(50, 50, 300, 300);
		shell.setLayout(new FillLayout());
		wrapper.setParent(shell);
		shell.open();
	}
	protected void useDocument(OberViewer viewer) {
		((StyledText)component).setContent(((StyledText)((OberSwtViewer)viewer).component).getContent());
	}
	protected void createGui() {
		createGui(null, -1);
	}
	protected void createGui(Composite parent, float position) {
		GridLayout tagLayout = new GridLayout();
		GridLayout wrapperLayout = new GridLayout();
		GridData data;

		if (parent == null) {
			parent = parentViewer != null ? ((OberSwtViewer)parentViewer).component : new Shell();
		}
		if (parent instanceof Shell) {
			parent.setLayout(new FillLayout());
		}
		wrapper = new Composite(parent, 0);
		wrapper.setData(this);
		wrapperLayout.verticalSpacing = wrapperLayout.horizontalSpacing = wrapperLayout.marginHeight = wrapperLayout.marginWidth = 0;
		wrapper.setLayout(wrapperLayout);
		tagPanel = new Composite(wrapper, 0);
		tagPanel.setData("tagPanel");
		tagLayout.verticalSpacing = tagLayout.horizontalSpacing = 4;
		tagLayout.marginHeight = tagLayout.marginWidth = 4;
		tagPanel.setLayout(tagLayout);
		tagPanel.addPaintListener(this);
		ober.addViewer(this);
		if (type != MAIN_TYPE)  {
			tagLayout.numColumns = 2;
			dragger = new OberSwtDragger(this, tagPanel, 0);
			data = new GridData();
			data.horizontalAlignment = data.verticalAlignment = GridData.FILL;
			dragger.setLayoutData(data);
			tag = new StyledText(tagPanel, SWT.SINGLE);
			data.widthHint = tag.getLineHeight();
		} else {
			tag = new StyledText(tagPanel, SWT.SINGLE);
		}
		tag.setData("tag");
		data = new GridData();
		data.horizontalAlignment = data.verticalAlignment = GridData.FILL;
		data.heightHint = tag.getLineHeight();
		data.grabExcessHorizontalSpace = true;
		tagPanel.setLayoutData(data);
		data = new GridData();
		data.horizontalAlignment = data.verticalAlignment = GridData.FILL;
		data.heightHint = tag.getLineHeight();
		data.grabExcessHorizontalSpace = true;
		tag.setLayoutData(data);
		tag.addMouseListener(this);
		((OberSwtGui)ober.gui).addFocusListener(tag);
		Control cont; 
		if (type == VIEWER_TYPE)  {
			FillLayout borderLayout = new FillLayout();
			Composite border = new Composite(wrapper, 0);
			border.setData("text border");
			border.setLayout(borderLayout);
			borderLayout.marginHeight = borderLayout.marginWidth = 2;
			border.addPaintListener(this);
			StyledText txt = new StyledText(border, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			OberSwtDocument doc = new OberSwtDocument(txt.getContent());
			txt.setContent(doc);
			doc.addPropertyChangeListener(dragger);
			component = txt;
			txt.addMouseListener(this);
			((OberSwtGui)ober.gui).addFocusListener(txt);
			cont = border;
		} else  {
			component = new Composite(wrapper, 0);
			component.setData("panel");
			component.setLayout(new OberSwtLayout(this, type == TRACK_TYPE));
			cont = component;
		}
		currentFocus = component;
		data = new GridData();
		data.horizontalAlignment = data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = data.grabExcessVerticalSpace = true;
		cont.setLayoutData(data);
		wrapper.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dying();
			}
		});
		tag.setText("Help");
		tag.setBackground(new Color(tag.getDisplay(), (Ober.VIEWER_COLOR >> 16) & 0xFF, (Ober.VIEWER_COLOR >> 8) & 0xFF, Ober.VIEWER_COLOR & 0xFF));
		if (parentViewer == null) {
		 	if (parent instanceof Shell) {
				((Shell)parent).open();
		 	} else {
		 		parent.layout();
		 	}
		} else {
			((OberSwtViewer)parentViewer).insertViewer(this, position);
		}
	}
	protected void insertViewer(OberViewer v, float position) {
		v.setParentViewer(this);
		if (position < 0) {
			layout().insert(((OberSwtViewer)v).wrapper);
		} else {
			layout().addControl(((OberSwtViewer)v).wrapper, position);
		}
		component.layout();
	}
	private OberSwtLayout layout() {
		return (OberSwtLayout)component.getLayout();
	}
	public void paintControl(PaintEvent e) {
		Point size = wrapper.getSize();

		e.gc.setLineWidth(2);
		e.gc.setForeground(e.display.getSystemColor(active ? SWT.COLOR_RED : SWT.COLOR_GREEN));
		e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
	}
	public void mouseDoubleClick(MouseEvent e) {}
	public void mouseDown(MouseEvent e) {
		if (firstButton(e)) {
			newFocus(e.widget);
		} else if (secondButton(e)) {
			StyledText textComponent = (StyledText) e.widget;
				
			try {
				findFile(this, textComponent.getText(), textComponent.getOffsetAtLocation(new Point(e.x, e.y)));
			} catch (Exception e1) {
				error(e1);
			}
			if (ober.getActiveViewer() != null) {
				((OberSwtViewer)ober.getActiveViewer()).component.setFocus();
			}
		} else if (thirdButton(e)) {
			StyledText textComponent = (StyledText) e.widget;

			executeCommand(new OberContext(this, textComponent.getOffsetAtLocation(new Point(e.x, e.y)), textComponent.getText()).findArgs());
			if (ober.getActiveViewer() != null) {
				((OberSwtViewer)ober.getActiveViewer()).setFocus();
			}
		}
	}
	public void setFocus() {
		currentFocus.setFocus();
	}
	public boolean firstButton(MouseEvent e) {
		return e.button == 1 && (e.stateMask & (SWT.CONTROL | SWT.BUTTON2 | SWT.BUTTON3)) == 0;
	}
	public boolean secondButton(MouseEvent e) {
		return (e.button == 2 && (e.stateMask & (SWT.CONTROL | SWT.BUTTON1 | SWT.BUTTON3)) == 0)
			|| (e.button == 1 && (e.stateMask & SWT.CONTROL) != 0);
	}
	public boolean thirdButton(MouseEvent e) {
		return e.button == 3 && (e.stateMask & (SWT.CONTROL | SWT.BUTTON1 | SWT.BUTTON2)) == 0;
	}
	public void mouseUp(MouseEvent e) {}
	public void setPosition(float frac) {
		((OberSwtViewer)parentViewer).layout().setPosition(this, frac);
	}
	public OberSwtViewer copyTextViewer(OberSwtViewer parent, float position) {
		OberSwtViewer copy = new OberSwtViewer();
		
		copy.ober = ober;
		copy.parentViewer = parent;
		copy.type = type;
		copy.createGui(null, position);
		copy.useDocument(this);
		return copy;
	}
}
