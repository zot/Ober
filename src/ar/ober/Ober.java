/*
(C) 2003 Bill Burdick

ar.ober.Ober

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/

package ar.ober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;

public class Ober implements PropertyChangeListener {
	protected ArrayList viewers = new ArrayList();
	protected OberViewer activeViewer;
	protected HashMap commands = new HashMap();
	protected HashMap properties = new HashMap();
	protected HashMap namespaces = new HashMap();
	
	public static final Color MAIN_COLOR = Color.WHITE;
	public static final Color TRACK_COLOR = new Color((float)0.8, (float)1, (float)1);
	public static final Color VIEWER_COLOR = new Color((float)0.8, (float)0.8, (float)0.8);

	public static void main(String args[]) {
		Ober ober = new Ober();
		OberViewer main = ober.createMain();

		ober.help(main);
		ober.createFrame(main).setVisible(true);
	}
	public Ober() {
		initialize();
	}
	public HashMap getProperties() {
		return properties;
	}
	public void help(OberViewer sourceViewer) {
		OberViewer tv = createTextViewer();
		StringBuffer buf = new StringBuffer("Welcome to Ober, an Oberon environment for Java.\n\n" +			"EXECUTING COMMANDS\n" +
			"To execute a command, position the mouse pointer over a word and click the third mouse " +			"button. If you do not have a three button mouse, use the second button.\n\n" +			"COMMAND ARGUMENTS\n" +			"Arguments are either words or OGNL expressions (http://www.ognl.org/) within square " +			"brackets (example: Echo [3 + 4]).  The reciever is the viewer containing the command.\n\n" +			"OGNL PROPERTIES\n" +			"focus -- The viewer with keyboard focus\n" +			"ober -- the Ober environment\n" +			"properties -- user properties for the command viewer.\n" +			"ober.properties -- global user properties.\n\n" +			"OPENING FILES\n" +
			"To open a file or a directory, type a filename and click it with the second mouse button.  If " +			"you do not have a three button mouse, control-click the filename instead.  Try one of these...\n" +
			"\tC:\\\n" +
			"\t/tmp\n\n" +			"AVAILABLE COMMANDS");

		ArrayList spaces = new ArrayList();
		for (Iterator i = namespaces.values().iterator(); i.hasNext(); ) {
			ArrayList inh = new ArrayList((ArrayList)i.next());
			ArrayList inhRev = new ArrayList();
			
			for (int j = inh.size(); j-- > 0; ) {
				inhRev.add(inh.get(j));
			}
			spaces.add(inhRev);
		}
		Collections.sort(spaces, new Comparator() {
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}
			public boolean equals(Object obj) {
				return true;
			}
		});
		ArrayList cmd = new ArrayList(commands.keySet());
		Collections.sort(cmd);
		int namespace = 0;
		String prevName = null;

		for (int i = 0; i < cmd.size(); i++) {
			String cmdSpace = ((String)cmd.get(i)).split("\\.")[0];

			while (spaces.size() > namespace) {
				ArrayList inh = (ArrayList)spaces.get(namespace);
				String name = (String) inh.get(inh.size() - 1);

				if (namespace == 0 || !prevName.equals(cmdSpace)) {
					ArrayList space = (ArrayList) namespaces.get(name);

					buf.append("\nNamespace: ");
					buf.append(space.get(0));
					buf.append(" path: ");
					for (int j = 1; j < space.size(); j++) {
						if (j > 1) {
							buf.append(", ");
						}
						buf.append(space.get(j));
					}
					buf.append('\n');
					prevName = name;
					namespace++;
				} else {
					break;
				}
			}
			buf.append("   ");
			buf.append(cmd.get(i));
			buf.append(((OberCommand)commands.get(cmd.get(i))).getDescription());
			buf.append('\n');
		}
		((JTextPane)tv.component).setText(buf.toString());
		sourceViewer.topViewer().acceptViewer(tv);
	}
	protected void initialize() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", this);
		addNamespace("System", new String[]{});
		addNamespace("Text", new String[]{});
		addNamespace("File", new String[]{"Text"});
		addNamespace("Errors", new String[]{"Text"});
		addCommand("System.Help", new OberCommand(" -- Show help on commands for a viewer.") {
			public void execute(OberContext ctx) {
				help(ctx.getSourceViewer());
			}
		});
		addCommand("System.Newcol", new OberCommand(" -- Create a new column.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().acceptViewer(createTrack());
			}
		});
		addCommand("System.Delcol", new OberCommand(" -- Delete a column.") {
			public void execute(OberContext ctx) {
				OberViewer track = ctx.getSourceViewer();

				switch (track.getType()) {
					case OberViewer.MAIN_TYPE:
						track.error("No track selected");
						break;
					case OberViewer.VIEWER_TYPE:
						track = track.getParentViewer();
					case OberViewer.TRACK_TYPE:
						Container c = track.getWrapper().getParent();
						track.setParentViewer(null);
						c.remove(track.getWrapper());
						c.validate();
						c.repaint();
						break;
				}
			}
		});
		addCommand("System.New", new OberCommand(" -- Create a new column.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().acceptViewer(createTextViewer());
			}
		});
		addCommand("System.Del", new OberCommand(" -- Delete a viewer.") {
			public void execute(OberContext ctx) {
				OberViewer viewer = ctx.getSourceViewer();

				switch (viewer.getType()) {
					case OberViewer.MAIN_TYPE:
					case OberViewer.TRACK_TYPE:
						viewer.error("No viewer selected");
						break;
					case OberViewer.VIEWER_TYPE:
						Container c = viewer.getWrapper().getParent();
						c.remove(viewer.getWrapper());
						c.validate();
						c.repaint();
						viewer.setParentViewer(null);
						break;
				}
			}
		});
		addCommand("System.Quit", new OberCommand(" -- Quit.") {
			public void execute(OberContext ctx) {
				System.exit(0);
			}
		});
		addCommand("Text.Get", new OberCommand(" -- Get the contents of a file into the source viewer and track changes.") {
			public void execute(OberContext ctx) {
				if (ctx.getSourceViewer().getComponent() instanceof JTextComponent) {
					ctx.getSourceViewer().loadFile();
				} else {
					ctx.getSourceViewer().error("You can only get a file into a text viewer.");
				}
			}
		});
		addCommand("Text.Put", new OberCommand(" -- Save the contents of the source viewer to a file and clear changes.") {
			public void execute(OberContext ctx) {
				if (ctx.getSourceViewer().getComponent() instanceof JTextComponent) {
					ctx.getSourceViewer().storeFile();
				} else {
					ctx.getSourceViewer().error("You can only put a file from a text viewer.");
				}
			}
		});
		addCommand("System.Echo", new OberCommand(" -- echo argument (example: Echo [3 + 4]).") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().error(ctx.getArgumentString(1));
			}
		});
		addCommand("Text.Clear", new OberCommand(" -- set the viewer's contents to empty.") {
			public void execute(OberContext ctx) {
				if (ctx.getSourceViewer().getComponent() instanceof JTextComponent) {
					((JTextComponent)ctx.getSourceViewer().getComponent()).setText("");
				} else {
					ctx.getSourceViewer().error("You can only clear a text viewer.");
				}
			}
		});
	}
	public void addNamespace(String space, String[] path) {
		ArrayList l = new ArrayList();
		
		l.add(space);
		for (int i = 0; i < path.length; i++) {
			if (!l.contains(path[i])) {
				l.add(path[i]);
			}
		}
		if (!l.contains("System")) {
			l.add("System");
		}
		namespaces.put(space, l);
	}
	public OberViewer getFocus() {
		return activeViewer;
	}
	public JFrame createFrame(final OberViewer viewer) {
		final JFrame fr = new JFrame("Ober");
		
		fr.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				fr.getContentPane().remove(viewer.getWrapper()); // signal viewers that they are dead
				fr.dispose();
			}
		});
		fr.setBounds(50, 50, 300, 300);
		fr.getContentPane().setLayout(new BorderLayout());
		fr.getContentPane().add(viewer.getWrapper(), BorderLayout.CENTER);
		return fr;
	}
	public void propertyChange(PropertyChangeEvent evt) {
		for (int i = 0; i < viewers.size(); i++) {
			((OberViewer)viewers.get(i)).newFocus((Component)evt.getNewValue());
		}
	}
	public void uninstall() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(this);
	}
	public void addViewer(OberViewer viewer) {
		viewers.add(viewer);
	}
	public void removeViewer(OberViewer viewer) {
		viewers.remove(viewer);
		if (activeViewer != null  && (activeViewer.topViewer() == null || activeViewer.topViewer() == viewer)) {
			deactivated(activeViewer);
		}
	}
	public void deactivated(OberViewer viewer) {
		if (activeViewer == viewer) {
			activeViewer = null;
		}
	}
	public void activated(OberViewer viewer) {
		activeViewer = viewer;
	}
	public OberViewer getActiveViewer() {
		return activeViewer;
	}
	public void executeCommand(OberContext ctx) {
		if (!ctx.getArguments().isEmpty()) {
			String prefix = ctx.getSourceViewer().getName();
			String cmdName = ctx.getArgumentString(0);
			OberCommand cmd = getCommand(cmdName);
				
			if (prefix != null) {
				ArrayList path = (ArrayList)namespaces.get(prefix);

				if (path == null && cmd == null) {
					cmd = getCommand("System." + cmdName);
				} else {
					for (int i = 0; cmd == null && i < path.size(); i++) {
						cmd = getCommand(path.get(i) + "." + cmdName);
					}
				}
			}
			if (cmd != null) {
				cmd.execute(ctx);
			} else {
				ctx.getSourceViewer().error("No command called '" + cmdName + "'");
			}
		}
	}
	public OberCommand getCommand(String cmdName) {
		return (OberCommand)commands.get(cmdName);
	}
	public void addCommand(String name, OberCommand cmd) {
		String parts[] = name.split("\\.");
		
		if (parts.length != 2) {
			throw new RuntimeException("Illegal command name format.  Expected: <name space>.<command name>, but got: " + name);
		}
		if (namespaces.get(parts[0]) == null) {
			throw new RuntimeException("Illegal namespace: " + parts[0]);
		}
		commands.put(name, cmd);
	}

	public OberViewer createMain() {
		OberViewer v = new OberViewer(OberViewer.MAIN_TYPE, this);
		JPanel panel = new JPanel();
	
		v.tagPanel.remove(v.dragger);
		panel.setLayout(new OberLayout(v, false));
		v.setComponent(panel, panel);
		v.getTag().setText("Ober: Newcol, New, Quit, Help");
		v.getTag().setBackground(MAIN_COLOR);
		return v;
	}

	public OberViewer createTrack() {
		OberViewer v = new OberViewer(OberViewer.TRACK_TYPE, this);
		JPanel panel = new JPanel();
	
		panel.setLayout(new OberLayout(v, true));
		v.setComponent(panel, panel);
		v.getTag().setText("Track: Delcol, New, Help");
		v.getTag().setBackground(TRACK_COLOR);
		return v;
	}

	public OberViewer createTextViewer() {
		final OberViewer v = new OberViewer(OberViewer.VIEWER_TYPE, this);
		OberViewer.AdaptedTextPane txt = new OberViewer.AdaptedTextPane(v);
	
		v.setComponent(txt, new JScrollPane(txt));
		v.getTag().setText("Viewer: Del, Help");
		txt.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				v.markDirty();
			}
		});
		return v;
	}
}
