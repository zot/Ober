/*
(C) 2003 Bill Burdick

ar.ober.OberViewer

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import ar.ober.Ober;
import ar.ober.OberContext;
import ar.ober.OberViewer;

public class OberSwingViewer extends OberViewer {
	public static class ViewerEventAdaptor implements MouseListener, KeyListener {
		protected ArrayList oldMouseListeners = new ArrayList();
		protected ArrayList oldKeyListeners = new ArrayList();
		protected OberSwingViewer viewer;
		
		public void setViewer(OberSwingViewer v) {
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
				JTextComponent textComponent = (JTextComponent) e.getSource();
				
				try {
					viewer.findFile(viewer, textComponent.getText(), textComponent.viewToModel(e.getPoint()));
				} catch (Exception e1) {
					viewer.error(e1);
				}
			} else if (thirdButton(e)) {
				JTextComponent txt = (JTextComponent)e.getSource();

				viewer.executeCommand(new OberContext(viewer, txt.viewToModel(e.getPoint()), txt.getText()).findArgs());
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

		public Tag(OberSwingViewer viewer) {
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

		public AdaptedTextPane(OberSwingViewer viewer) {
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
	
	public Tag tag = new Tag(this);
	protected JPanel tagPanel = new JPanel();
	protected JPanel wrapper = new JPanel();
	protected OberDragWidget dragger = new OberDragWidget(this);
	protected Component component;

	public static final int VIEWER_TYPE = 0;
	public static final int TRACK_TYPE = 1;
	public static final int MAIN_TYPE = 2;
	public static final Pattern LINE = Pattern.compile("(?m)^.*$");
	public static final String VIEWER_NAME = "^[a-zA-Z0-9_]+(?=:)";
	public static final Pattern VIEWER_NAME_PATTERN = Pattern.compile(VIEWER_NAME);
	//public static final String ARG = "(?<=^|[^a-zA-Z0-9_<>|!.:$])[a-zA-Z0-9_<>|!.:$]+(?=[^a-zA-Z0-9_<>|!.:$]|$)|\\[";
	public static final String ARG = "([-a-zA-Z0-9_<>|!.:$/]|\\\\\\[)+|\\[";
	public static final Pattern ARG_PATTERN = Pattern.compile(ARG);
	public static final String FILE = "((?:[a-zA-Z]+://(?:[a-zA-Z0-9._\\-]+(?:(?::[a-zA-Z0-9._\\-]+)?@[a-zA-Z0-9._\\-]+)?)?)?)((?:[a-zA-Z]:)?[a-zA-Z0-9./\\\\_\\-:]+)(?:[^a-zA-Z0-9./\\\\_\\-:]|$)";
	public static final Pattern FILE_PATTERN = Pattern.compile(FILE);
	public static final String TAG_FILE = "File:\\s" + FILE;
	public static final Pattern TAG_FILE_PATTERN = Pattern.compile(TAG_FILE);
	public static final Pattern COMMENT_PATTERN = Pattern.compile("\\s*#");

	public String getTagText()  {
		return tag.getText();
	}
	public boolean inTag(Object event)  {
		return ((AWTEvent)event).getSource() == tag;
	}
	public int getTagCaretPosition()  {
		return tag.getCaretPosition();
	}
	public void setTagText(String txt) {
		tag.setText(txt);
	}
	public void setTagBackground(int rgb) {
		tag.setBackground(new Color(rgb));
	}
	public int getDocumentLength()  {
		return ((JTextComponent)component).getDocument().getLength();
	}
	public int getCaretPosition()  {
		return ((JTextComponent)component).getCaretPosition();
	}
	public void requestFocus()  {
		component.requestFocus();
	}
	public void insertString(int pos, String str, Object style)  {
		if (pos == -1) {
			pos = getDocumentLength();
		}
		try {
			((JTextComponent)component).getDocument().insertString(pos, str, (AttributeSet)style);
		} catch (BadLocationException e) {
			error(e);
		}
	}
	public void setText(String txt) {
		((JTextComponent)component).setText(txt);
	}
	public String getText(int start, int len) {
		try {
			return ((JTextComponent)component).getText(start, len);
		} catch (BadLocationException e) {
			error(e);
			return null;
		}
	}
	public void setCaretPosition(int pos)  {
		((JTextComponent)component).setCaretPosition(pos);
	}
	public int getWidth()  {
		return wrapper.getWidth();
	}
	public void insertViewer(OberViewer v)  {
		layout().insert((OberSwingViewer)v, (Container)component);
		component.invalidate();
		component.doLayout();
		component.validate();
	}
	public void removeFromParent()  {
		ober.removeViewer(this);
		parentViewer.removeChild(this);
		Container c = wrapper.getParent();
		parentViewer = null;
		c.remove(wrapper);
		c.validate();
		c.repaint();
		dying();
	}
	protected void useDocument(OberViewer viewer)  {
		final OberDocument doc = (OberDocument)((JTextComponent)((OberSwingViewer)viewer).component).getDocument();
		final OberSwingViewer v = new OberSwingViewer() {
			public void dying() {
				super.dying();
				doc.removePropertyChangeListener(dragger);
			}
		};
		v.type = VIEWER_TYPE;
		v.ober = ober;
		AdaptedTextPane txt = new AdaptedTextPane(v);

		doc.addPropertyChangeListener(v.dragger);
		v.setComponent(txt, new JScrollPane(txt));
		txt.setDocument(doc);
	}
	public void detachDocument() throws IOException, ClassNotFoundException  {
		((OberDocument)((JTextComponent)component).getDocument()).removePropertyChangeListener(dragger);
		((JTextComponent)component).setDocument(OberDocument.copy((OberDocument)((JTextComponent)component).getDocument()));
		((OberDocument)((JTextComponent)component).getDocument()).addPropertyChangeListener(dragger);
	}
	public void createGui() {
		Dimension tabPref = tag.getPreferredSize();

		(type == VIEWER_TYPE ? (JComponent)wrapper : tagPanel).setBorder(BorderFactory.createLineBorder(Color.GRAY));
		ober.addViewer(this);
		tagPanel.setLayout(new BorderLayout());
		dragger.setPreferredSize(new Dimension(tabPref.height, tabPref.height));
		if (type != MAIN_TYPE)  {
			tagPanel.add(dragger, BorderLayout.WEST);
		}
		tagPanel.add(tag, BorderLayout.CENTER);
		wrapper.setLayout(new BorderLayout());
		wrapper.add(tagPanel, BorderLayout.NORTH);
		if (type == VIEWER_TYPE)  {
			AdaptedTextPane txt = new AdaptedTextPane(this);
			OberDocument doc = new OberDocument();

			txt.setFont(Font.getFont("courier"));
			txt.setDocument(doc);
			doc.addPropertyChangeListener(dragger);
			setComponent(txt, new JScrollPane(txt));
		} else  {
			JPanel panel = new JPanel();
	
			panel.setSize(200, 200);
			panel.setLayout(new OberLayout(this, type == TRACK_TYPE));
			setComponent(panel, panel);
		}
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
		tag.setBackground(new Color(Ober.VIEWER_COLOR));
		if (parentViewer == null) {
			openFrame();
		} else {
			((OberSwingViewer)parentViewer).insertViewer(this);
		}
	}
	public void openFrame() {
		final JFrame fr = new JFrame("Ober");
		
		fr.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				fr.getContentPane().remove(wrapper); // signal viewers that they are dead
				fr.dispose();
			}
		});
		fr.setBounds(50, 50, 600, 400);
		fr.getContentPane().setLayout(new BorderLayout());
		fr.getContentPane().add(wrapper, BorderLayout.CENTER);
		fr.setVisible(true);
	}
	public OberLayout layout() {
		return (OberLayout)((Container)component).getLayout();
	}
	public void setComponent(Component comp, Component compWrapper) {
		for (int i = wrapper.getComponents().length; i-- > 0; ) {
			if (wrapper.getComponent(i) != tagPanel) {
				wrapper.remove(wrapper.getComponent(i));
				break;
			}
		}
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
	public JPanel getWrapper() {
		return wrapper;
	}
	public void setParentViewer(OberSwingViewer pv) {
		if (parentViewer != null) {
			parentViewer.removeChild(this);
		}
		this.parentViewer = pv;
		if (parentViewer != null) {
			parentViewer.addChild(this);
		}
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
	public void setTrackingChanges(boolean track)  {
		((OberDocument)((AdaptedTextPane)component).getDocument()).setTrackingChanges(track);
	}
	public void repaintDragger() {
		dragger.repaint();
	}
	public void setPosition(float frac) {
		((OberSwingViewer)parentViewer).layout().setPosition(this, frac);
	}
	public void replace(int start, int end, String str, Object style) {
		JTextComponent text = (JTextComponent)component;
		
		try {
			AbstractDocument doc = (AbstractDocument) text.getDocument();

			doc.replace(start, end - start, str, (AttributeSet)style);
		} catch (BadLocationException e) {
			System.out.println("Error: ");
			e.printStackTrace();
			error(e);
		}
	}
	public void removeStyle(Object style) {
		OberDocument doc = (OberDocument)((JTextComponent)component).getDocument();
		
		for (int i = doc.getLength(); i-- > 0; ) {
			AttributeSet att = doc.getCharacterElement(i).getAttributes();

			if (att.containsAttributes((AttributeSet) style)) {
				doc.setCharacterAttributes(i, 1, (AttributeSet) PLAIN, true);
			}
		}
	}
	public void setStyle(int start, int end, Object style) {
		((OberDocument)((JTextComponent)component).getDocument()).setCharacterAttributes(start, end - start, (AttributeSet) style, true);
	}
}
