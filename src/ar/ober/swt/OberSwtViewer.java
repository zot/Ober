package ar.ober.swt;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
		v.setTagText("Ober: Newcol, New, Quit, Help");
		return v;
	}

	public void insertInBackground(final Reader rdr, final String prompt, final boolean moveCaret, final boolean clear) {
		new Thread(new SwtBackgroundInserter(rdr, prompt, moveCaret, clear)).start();
	}
	public void newFocus(Widget w) {
		boolean hasFocus = w == component || w == tag;

		if (active != hasFocus) {
			active = hasFocus;
			if (hasFocus) {
				File file = getFile();

				if (ober.getActiveViewer() != null) {
					((OberSwtViewer)ober.getActiveViewer()).newFocus(w);
				}
				ober.activated(this);
				wrapper.getShell().setText("Ober: " + (file == null ? "null" : file.getAbsolutePath()));
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
		return ((Event)event).widget == tag;
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
			if (style != null) {
				StyleRange r = (StyleRange) ((StyleRange)style).clone();
				
				r.start = p;
				r.length = str.length();
				((StyledText)component).replaceStyleRanges(p, str.length(), new StyleRange[] {r});
			} else {
				((StyledText)component).replaceStyleRanges(p, str.length(), new StyleRange[0]);
			}
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
		StyledText tmpTxt = new OberStyledText(((OberSwtViewer)parentViewer).component, 0);
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
		StyleRange oldRanges[] = ((StyledText)((OberSwtViewer)viewer).component).getStyleRanges();
		StyleRange newRanges[] = new StyleRange[oldRanges.length];

		((StyledText)component).setContent(((StyledText)((OberSwtViewer)viewer).component).getContent());
		for (int i = 0; i < oldRanges.length; i++) {
			newRanges[i] = (StyleRange)oldRanges[i].clone();
		}
		((StyledText)component).setStyleRanges(newRanges);
	}
	public void createGui() {
		createGui(null, -1);
	}
	public void createGui(Composite parent, float position) {
		FormLayout tagLayout = new FormLayout();
		FormLayout wrapperLayout = new FormLayout();
		FormData data;

		if (parent == null) {
			parent = parentViewer != null ? ((OberSwtViewer)parentViewer).component : new Shell();
		}
		if (parent instanceof Shell) {
			parent.setLayout(new FillLayout());
		}
		wrapper = new Composite(parent, 0);
		wrapper.setData(this);
		wrapper.setLayout(wrapperLayout);
		tagPanel = new Composite(wrapper, 0);
		tagPanel.setData("tagPanel");
		tagPanel.setLayout(tagLayout);
		tagLayout.marginHeight = tagLayout.marginWidth = 2;
		tagPanel.addPaintListener(this);
		ober.addViewer(this);
		if (type != MAIN_TYPE)  {
			dragger = new OberSwtDragger(this, tagPanel, 0);
			tag = new OberStyledText(tagPanel, SWT.SINGLE);
			data = new FormData();
			data.left = new FormAttachment(0, 0);
			data.top = new FormAttachment(0, 0);
			data.bottom = new FormAttachment(100, 0);
			data.width = 24;
			dragger.setLayoutData(data);
			data = new FormData();
			data.left = new FormAttachment(dragger, 2);
			data.right = new FormAttachment(100, 0);
			data.top = new FormAttachment(0, 0);
			data.bottom = new FormAttachment(100, 0);
			data.height = SWT.DEFAULT;
			tag.setLayoutData(data);
		} else {
			tag = new OberStyledText(tagPanel, SWT.SINGLE);
			data = new FormData();
			data.left = new FormAttachment(0, 0);
			data.top = new FormAttachment(0, 0);
			data.right = new FormAttachment(100, 0);
			data.bottom = new FormAttachment(100, 0);
			data.height = SWT.DEFAULT;
			tag.setLayoutData(data);
		}
		bindKeys(tag);
		tag.setData("tag");
		tag.setText(" ");
		//((FormData)tag.getLayoutData()).bottom = new FormAttachment(0, tag.getLineHeight() + 100 - tag.computeTrim(0, 0, 100, 100).height);
		data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.top = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.height = SWT.DEFAULT;
		tagPanel.setLayoutData(data);
		tag.addMouseListener(this);
		Control cont; 
		if (type == VIEWER_TYPE)  {
			FormLayout borderLayout = new FormLayout();
			Composite border = new Composite(wrapper, 0);
			border.setData("text border");
			border.setLayout(borderLayout);
			border.addPaintListener(this);
			StyledText txt = new OberStyledText(border, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			OberSwtDocument doc = new OberSwtDocument(txt.getContent());
			txt.setContent(doc);
			txt.setBackground(txt.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			txt.setFont(new Font(txt.getDisplay(), new FontData("Courier", 12, SWT.NORMAL)));
			doc.addPropertyChangeListener(dragger);
			component = txt;
			txt.addMouseListener(this);
			data = new FormData();
			data.top = new FormAttachment(0, 0);
			data.left = new FormAttachment(0, 1);
			data.right = new FormAttachment(100, -1);
			data.bottom = new FormAttachment(100, -1);
			txt.setLayoutData(data);
			bindKeys(txt);
			cont = border;
		} else  {
			component = new Composite(wrapper, 0);
			component.setData("panel");
			component.setLayout(new OberSwtLayout(this, type == TRACK_TYPE));
			cont = component;
		}
		currentFocus = component;
		data = new FormData();
		data.top = new FormAttachment(tagPanel);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.bottom = new FormAttachment(100, 0);
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
	protected void bindKeys(StyledText txt) {
		txt.setKeyBinding(SWT.CONTROL | 'C', ST.COPY);
		txt.setKeyBinding(SWT.CONTROL | 'V', ST.PASTE);
		txt.setKeyBinding(SWT.CONTROL | 'X', ST.CUT);
		txt.setDoubleClickEnabled(true);
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
		Point size = ((Control)e.widget).getSize();

		e.gc.setLineWidth(1);
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
	public class SwtBackgroundInserter extends BackgroundInserter {
		public SwtBackgroundInserter(Reader rdr, String prompt, boolean moveC, boolean clear) {
			super(rdr, prompt, moveC, clear);
		}
		public void superFinish() {
			super.finish();
		}
		public void finish() {
			wrapper.getDisplay().syncExec(new Runnable() {
				public void run() {
					superFinish();
				}
			});
		}
		public void superInit() {
			super.initialize();
		}
		public void initialize() {
			wrapper.getDisplay().syncExec(new Runnable() {
				public void run() {
					superInit();
				}
			});
		}
		public void superInsertSnippet(String snippet) {
			super.insertSnippet(snippet);
		}
		public void insertSnippet(final String snippet) {
			wrapper.getDisplay().syncExec(new Runnable() {
				public void run() {
					superInsertSnippet(snippet);
				}
			});
		}
	}
	public class OberStyledText extends StyledText implements Listener {
		protected Listener upListener;
		protected Listener downListener;

		public OberStyledText(Composite parent, int style) {
			super(parent, style);
			super.addListener(SWT.KeyDown, this);
			super.addListener(SWT.KeyUp, this);
			super.addListener(SWT.HardKeyDown, this);
			super.addListener(SWT.HardKeyUp, this);
		}
		public void addListener(int evtType, Listener l) {
			switch (evtType) {
				case SWT.KeyDown:
					downListener = l;
					break;
				case SWT.KeyUp:
					upListener = l;
					break;
				default:
					super.addListener(evtType, l);
			}
		}
		public void handleEvent(Event event) {
			switch (event.type) {
				case SWT.KeyDown:
					if (!ober.handleKey(OberSwtViewer.this, event)) {
						if (downListener != null) {
							downListener.handleEvent(event);
						}
					}
					break;
				case SWT.KeyUp:
				case SWT.HardKeyUp:
					if (upListener != null) {
						upListener.handleEvent(event);
					}
					break;
			}
		}
	}
}
