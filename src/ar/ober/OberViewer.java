/*
(C) 2003 Bill Burdick

ar.ober.OberViewer

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class OberViewer {
	public static class ViewerEventAdaptor implements MouseListener, KeyListener {
		protected ArrayList oldMouseListeners = new ArrayList();
		protected ArrayList oldKeyListeners = new ArrayList();
		protected OberViewer viewer;
		
		public void setViewer(OberViewer v) {
			viewer = v;
		}
		public boolean firstButton(MouseEvent e) {
			return e.getButton() == 1 && (e.getModifiersEx() & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == 0;
		}
		public boolean secondButton(MouseEvent e) {
			return (e.getButton() == 2 && (e.getModifiersEx() & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == 0)
				|| (e.getButton() == 1 && (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0);
		}
		public boolean thirdButton(MouseEvent e) {
			return e.getButton() == 3 && (e.getModifiersEx() & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK)) == 0;
		}
		public void mouseClicked(MouseEvent e) {
			if (firstButton(e)) {
				for (int i = 0; i < oldMouseListeners.size(); i++) {
					((MouseListener)oldMouseListeners.get(i)).mouseClicked(e);
				}
			} else if (secondButton(e)) {
				viewer.findFile(viewer, (JTextComponent)e.getSource(), e.getPoint());
			} else if (thirdButton(e)) {
				viewer.executeCommand(new OberContext(e, (JTextComponent)e.getSource(), viewer));
			}
		}
		public void mousePressed(MouseEvent e) {
			if (firstButton(e)) {
				for (int i = 0; i < oldMouseListeners.size(); i++) {
					((MouseListener)oldMouseListeners.get(i)).mousePressed(e);
				}
			}
		}
		public void mouseReleased(MouseEvent e) {
			if (firstButton(e)) {
				for (int i = 0; i < oldMouseListeners.size(); i++) {
					((MouseListener)oldMouseListeners.get(i)).mouseReleased(e);
				}
			}
		}
		public void mouseEntered(MouseEvent e) {
			for (int i = 0; i < oldMouseListeners.size(); i++) {
				((MouseListener)oldMouseListeners.get(i)).mouseEntered(e);
			}
		}
		public void mouseExited(MouseEvent e) {
			for (int i = 0; i < oldMouseListeners.size(); i++) {
				((MouseListener)oldMouseListeners.get(i)).mouseExited(e);
			}
		}
		public void keyTyped(KeyEvent e) {
			for (int i = 0; i < oldKeyListeners.size(); i++) {
				((KeyListener)oldKeyListeners.get(i)).keyTyped(e);
			}
		}
		public void keyPressed(KeyEvent e) {
			if (!viewer.handleKey(e)) {
				for (int i = 0; i < oldKeyListeners.size(); i++) {
					((KeyListener)oldKeyListeners.get(i)).keyPressed(e);
				}
			}
		}
		public void keyReleased(KeyEvent e) {
			for (int i = 0; i < oldKeyListeners.size(); i++) {
				((KeyListener)oldKeyListeners.get(i)).keyReleased(e);
			}
		}
		public void addMouseListener(MouseListener l) {
			oldMouseListeners.add(l);
		}
		public void removeMouseListener(MouseListener l) {
			oldMouseListeners.remove(l);
		}
		public void addKeyListener(KeyListener l) {
			oldKeyListeners.add(l);
		}
		public void removeKeyListener(KeyListener l) {
			oldKeyListeners.remove(l);
		}
	}

	public static class Tag extends JTextField {
		protected ViewerEventAdaptor eventAdaptor;

		public Tag(OberViewer viewer) {
			super();
			getEventAdaptor().setViewer(viewer);
		}
		public ViewerEventAdaptor getEventAdaptor() {
			if (eventAdaptor == null) {
				eventAdaptor = new ViewerEventAdaptor();
				super.addMouseListener(eventAdaptor);
				super.addKeyListener(eventAdaptor);
			}
			return eventAdaptor;
		}
		public void addMouseListener(MouseListener l) {
			getEventAdaptor().addMouseListener(l);
		}
		public synchronized void removeMouseListener(MouseListener l) {
			getEventAdaptor().removeMouseListener(l);
		}
	}

	public static class AdaptedTextPane extends JTextPane {
		protected ViewerEventAdaptor eventAdaptor;

		public AdaptedTextPane(OberViewer viewer) {
			super();
			getEventAdaptor().setViewer(viewer);
		}
		public ViewerEventAdaptor getEventAdaptor() {
			if (eventAdaptor == null) {
				eventAdaptor = new ViewerEventAdaptor();
				super.addMouseListener(eventAdaptor);
				super.addKeyListener(eventAdaptor);
			}
			return eventAdaptor;
		}
		public void addMouseListener(MouseListener l) {
			getEventAdaptor().addMouseListener(l);
		}
		public synchronized void removeMouseListener(MouseListener l) {
			getEventAdaptor().removeMouseListener(l);
		}
	}
	
	protected Tag tag = new Tag(this);
	protected JPanel tagPanel = new JPanel();
	protected JPanel wrapper = new JPanel();
	protected OberDragWidget dragger = new OberDragWidget(this);
	protected boolean active = false;
	protected Ober ober;
	protected Component component;
	protected OberViewer parentViewer;
	protected ArrayList children = new ArrayList();
	protected int type;
	protected HashMap properties = new HashMap();

	public static final int VIEWER_TYPE = 0;
	public static final int TRACK_TYPE = 1;
	public static final int MAIN_TYPE = 2;
	public static final Pattern LINE = Pattern.compile("(?m)^.*$");
	public static final String VIEWER_NAME = "^[a-zA-Z0-9_]+(?=:)";
	public static final Pattern VIEWER_NAME_PATTERN = Pattern.compile(VIEWER_NAME);
	//public static final String ARG = "(?<=^|[^a-zA-Z0-9_<>|!.:$])[a-zA-Z0-9_<>|!.:$]+(?=[^a-zA-Z0-9_<>|!.:$]|$)|\\[";
	public static final String ARG = "([a-zA-Z0-9_<>|!.:$/]|\\\\\\[)+|\\[";
	public static final Pattern ARG_PATTERN = Pattern.compile(ARG);
	public static final String FILE = "((?:[a-zA-Z]+://(?:[a-zA-Z0-9._\\-]+(?:(?::[a-zA-Z0-9._\\-]+)?@[a-zA-Z0-9._\\-]+)?)?)?)((?:[a-zA-Z]:)?[a-zA-Z0-9./\\\\_\\-:]+)(?:[^a-zA-Z0-9./\\\\_\\-:]|$)";
	public static final Pattern FILE_PATTERN = Pattern.compile(FILE);
	public static final String TAG_FILE = "File:\\s" + FILE;
	public static final Pattern TAG_FILE_PATTERN = Pattern.compile(TAG_FILE);
	public static final Pattern COMMENT_PATTERN = Pattern.compile("\\s*#");

	public OberViewer(Ober o) {
		this(VIEWER_TYPE, o);
	}
	public OberViewer(int type, Ober o) {
		this.type = type;
		ober = o;
		createGui();
	}
	public OberViewer getFocus() {
		return ober.getActiveViewer();
	}
	public HashMap getProperties() {
		return properties;
	}
	public Ober getOber() {
		return ober;
	}
	protected void createGui() {
		Dimension tabPref = tag.getPreferredSize();

		(type == VIEWER_TYPE ? (JComponent)wrapper : tagPanel).setBorder(BorderFactory.createLineBorder(Color.GRAY));
		ober.addViewer(this);
		dragger.setPreferredSize(new Dimension(tabPref.height, tabPref.height));
		tagPanel.setLayout(new BorderLayout());
		tagPanel.add(dragger, BorderLayout.WEST);
		tagPanel.add(tag, BorderLayout.CENTER);
		wrapper.setLayout(new BorderLayout());
		wrapper.add(tagPanel, BorderLayout.NORTH);
		wrapper.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				if ((e.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.HIERARCHY_CHANGED)) != 0) {
					if (e.getChanged().getParent() == null) {
						dying();
					}
				}
			}
		});
		tag.setText("Help");
		tag.setBackground(Ober.VIEWER_COLOR);
	}
	public void dying() {
		ober.removeViewer(OberViewer.this);
	}
	public void acceptViewer(OberViewer v) {
		if (v.getType() >= type) {
			parentViewer.acceptViewer(v);
		} else if (v.getType() == VIEWER_TYPE && type == MAIN_TYPE){
			int maxWidth = 0;
			int trackIndex = 0;

			if (children.isEmpty()) {
				acceptViewer(ober.createTrack());
			}
			for (int i = 0; i < children.size(); i++) {
				int wid = ((OberViewer)children.get(i)).getWrapper().getWidth();

				if (wid > maxWidth) {
					maxWidth = wid;
					trackIndex = i;
				}
			}
			((OberViewer)children.get(trackIndex)).acceptViewer(v);
		} else {
			layout().insert(v, (Container)component);
			component.validate();
		}
	}
	public OberLayout layout() {
		return (OberLayout)((Container)component).getLayout();
	}
	public Component getComponent() {
		return component;
	}
	public void setComponent(Component comp, Component compWrapper) {
		component = comp;
		wrapper.add(compWrapper, BorderLayout.CENTER);
	}
	public void newFocus(Component c) {
		if (active && (c == null || !wrapper.isAncestorOf(c))) {
			active = false;
			(type == VIEWER_TYPE ? (JComponent)wrapper : tagPanel).setBorder(BorderFactory.createLineBorder(Color.GRAY));
			ober.deactivated(this);
		} else if (!active && c!= null && wrapper.isAncestorOf(c)) {
			active = true;
			(type == VIEWER_TYPE ? (JComponent)wrapper : tagPanel).setBorder(BorderFactory.createLineBorder(Color.RED));
			ober.activated(this);
		}
	}
	public boolean handleKey(KeyEvent e) {
		return ober.handleKey(this, e);
	}
	public void executeCommand(OberContext ctx) {
		if (!ctx.getArguments().isEmpty()) {
			ober.executeCommand(ctx);
		}
	}
	public JPanel getWrapper() {
		return wrapper;
	}
	public Tag getTag() {
		return tag;
	}
	public int getType() {
		return type;
	}
	public OberViewer getParentViewer() {
		return parentViewer;
	}
	public void setParentViewer(OberViewer pv) {
		if (parentViewer != null) {
			parentViewer.removeChild(this);
		}
		this.parentViewer = pv;
		if (parentViewer != null) {
			parentViewer.addChild(this);
		}
	}
	protected void addChild(OberViewer viewer) {
		children.add(viewer);
	}
	protected void removeChild(OberViewer viewer) {
		children.remove(viewer);
	}
	protected void error(Throwable t) {
		StringWriter w = new StringWriter();
		
		t.printStackTrace(new PrintWriter(w));
		error(w.toString());
	}
	protected void error(String msg) {
		msg("Errors", msg);
	}
	protected void msg(String name, String msg) {
		OberViewer v = topViewer().findViewerNamed(name);
		
		if (v == null) {
			v = ober.createTextViewer();
			v.setName(name);
			topViewer().acceptViewer(v);
		}
		JTextPane text = (JTextPane)v.getComponent();
		msg += "\n";
		try {
			text.getDocument().insertString(text.getDocument().getLength(), msg, null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	public OberViewer topViewer() {
		return type == MAIN_TYPE ? this : parentViewer == null ? null : parentViewer.topViewer();
	}
	protected OberViewer findViewerNamed(String string) {
		if (getName().equals(string)) {
			return this;
		}
		for (int i = 0; i < children.size(); i++) {
			OberViewer v = getChild(i).findViewerNamed(string);
			
			if (v != null) {
				return v;
			}
		}
		return null;
	}
	protected OberViewer findViewerForFile(String string) {
		String filename[] = getFilename();

		if ("File".equals(getName()) && string.equals(filename == null ? null : filename[1])) {
			return this;
		}
		for (int i = 0; i < children.size(); i++) {
			OberViewer v = getChild(i).findViewerForFile(string);
			
			if (v != null) {
				return v;
			}
		}
		return null;
	}
	protected OberViewer getChild(int i) {
		return (OberViewer)children.get(i);
	}
	public String getName() {
		String tabText = tag.getText();
		Matcher m = VIEWER_NAME_PATTERN.matcher(tabText);

		return m.find() ? tabText.substring(m.start(), m.end()) : null;
	}
	public void setName(String name) {
		String tabText = tag.getText();
		Matcher m = VIEWER_NAME_PATTERN.matcher(tabText);

		if (m.find()) {
			tag.setText(tabText.substring(0, m.start()) + name + tabText.substring(m.end(), tabText.length()));
		} else {
			tag.setText(name + ": " + tag.getText());
		}
	}
	public String toString() {
		return "Viewer: " + getName();
	}
	public String[] getFilename() {
		String txt = tag.getText();
		Matcher match = TAG_FILE_PATTERN.matcher(txt);

		if (match.find()) {
			return new String[]{txt.substring(match.start(1), match.end(1)), txt.substring(match.start(2), match.end(2))};
		}
		return null;
	}
	public void loadFile() {
		if (!(component instanceof JTextPane)) {
			error("This type of viewer can't load or save files.");
			return;
		}
		OberDocument doc = (OberDocument)((JTextComponent)component).getDocument();
		File file = new File(getFilename()[1]);
		FileInputStream fin = null;
		StringBuffer str = new StringBuffer();
		byte buf[] = new byte[1024];
	
		if (file.exists()) {
			if (file.isDirectory()) {
				File files[] = file.listFiles();
						
				Arrays.sort(files);
				for (int i = 0; i < files.length; i++) {
					str.append(files[i].getName());
					if (files[i].isDirectory()) {
						str.append(File.separatorChar);
					}
					str.append('\n');
				}
			} else {
				try {
					fin = new FileInputStream(file);
					for (int count = fin.read(buf); count > 0; count = fin.read(buf)) {
						str.append(new String(buf, 0, count));
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (fin != null) {
							fin.close();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			int pos = ((JTextComponent)component).getCaretPosition();
			((JTextComponent)component).setText(str.toString());
			((JTextComponent)component).setCaretPosition(Math.min(pos, doc.getLength()));
			doc.setTrackingChanges(true);
			doc.markClean();
		}
	}
	public void storeFile() {
		if (!(component instanceof JTextComponent)) {
			error("This type of viewer can't load or save files.");
		}
		String filename[] = getFilename();
		FileOutputStream fout = null;
		Document doc = ((JTextComponent)component).getDocument();

		try {
			fout = new FileOutputStream(filename[1]);
			fout.write(doc.getText(0, doc.getLength()).getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fout != null) {
					fout.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		markClean();
	}
	public boolean isDirty() {
		return component instanceof AdaptedTextPane
			&& ((AdaptedTextPane)component).getDocument() instanceof OberDocument
			&& ((OberDocument)((AdaptedTextPane)component).getDocument()).isDirty();
	}
	public void markDirty() {
		((OberDocument)((AdaptedTextPane)component).getDocument()).markDirty();
	}
	public void markClean() {
		((OberDocument)((AdaptedTextPane)component).getDocument()).markClean();
	}
	public void repaintDragger() {
		dragger.repaint();
	}
	// TODO factor out matching so we can do it line-by-line
	// public Matcher findPattern(Pattern pat, )
	public void findFile(OberViewer viewer, JTextComponent textComponent, Point p) {
		int loc = textComponent.viewToModel(p);
		String txt = textComponent.getText();
		Matcher m = FILE_PATTERN.matcher(txt);

		while (m.find()) {
			if (m.start() <= loc && m.end() >= loc) {
				String filename = viewer.filenameFor(txt.substring(m.start(2), m.end(2)));
				findOrCreateViewerForFile(filename);
				return;
			}
		}
	}
	public void findOrCreateViewerForFile(String filename) {
		OberViewer fileViewer = topViewer().findViewerForFile(filename);
		
		if (fileViewer == null) {
			fileViewer = ober.createTextViewer();
			fileViewer.tag.setText("File: " + filename + " Get, Put, Del, Help, Split");
			topViewer().acceptViewer(fileViewer);
			fileViewer.loadFile();
		}
		fileViewer.getComponent().requestFocus();
	}
	public String filenameFor(String string) {
		String parent[] = getFilename();
		File f;
		
		if (parent == null) {
			f = new File(string);
		} else {
			f = new File(parent[1]);
			f = new File(f.isDirectory() ? f : f.getParentFile(), string);
		}
		return f.getAbsolutePath() + (f.isDirectory() ? File.separator : "");
	}
	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}
	public Object getProperty(String name) {
		return properties.get(name);
	}
}
