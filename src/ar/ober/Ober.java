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
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;

import ognl.Node;
import ognl.Ognl;

public class Ober implements PropertyChangeListener {
	protected ArrayList viewers = new ArrayList();
	protected OberViewer activeViewer;
	protected HashMap commands = new HashMap();
	protected HashMap properties = new HashMap();
	protected HashMap namespaces = new HashMap();
	protected HashMap keybindings = new HashMap();
	protected HashSet boundKeys = new HashSet();
	
	public static final Color MAIN_COLOR = Color.WHITE;
	public static final Color TRACK_COLOR = new Color((float)0.8, (float)1, (float)1);
	public static final Color VIEWER_COLOR = new Color((float)0.8, (float)0.8, (float)0.8);
	public static final String VERSION = "0.9.4";
	
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
		StringBuffer buf = new StringBuffer("Welcome to Ober, version " + VERSION + ", an Oberon environment for Java.\n\n" +			"EXECUTING COMMANDS\n" +
			"To execute a command, position the mouse pointer over a word and click the third mouse " +			"button. If you do not have a three button mouse, use the second button.\n\n" +			"COMMAND ARGUMENTS\n" +			"Arguments are either words or OGNL expressions (http://www.ognl.org/) within square " +			"brackets (example: Echo [3 + 4]).  The reciever is the viewer containing the command.\n\n" +			"OGNL PROPERTIES\n" +			"focus -- The viewer with keyboard focus\n" +			"ober -- the Ober environment\n" +			"properties -- user properties for the command viewer.\n" +			"ober.properties -- global user properties.\n\n" +			"OPENING FILES\n" +
			"To open a file or a directory, type a filename and click it with the second mouse button.  If " +			"you do not have a three button mouse, control-click the filename instead.  Try one of these...\n" +
			"\tC:\\\n" +
			"\t/tmp\n\n" +			"AVAILABLE COMMANDS");

		ArrayList spaces = new ArrayList();
		for (Iterator i = namespaces.values().iterator(); i.hasNext(); ) {
			ArrayList inh = new ArrayList((ArrayList)i.next());
			StringBuffer inhRev = new StringBuffer();
			
			for (int j = inh.size(); j-- > 0; ) {
				if (j < inh.size() - 1) {
					inhRev.append(",");
				}
				inhRev.append(inh.get(j));
			}
			spaces.add(inhRev.toString());
		}
		Collections.sort(spaces);
		ArrayList cmd = new ArrayList(commands.keySet());
		Collections.sort(cmd);
		for (int space = 0; space < spaces.size(); space++) {
			boolean started = false;
			String inhString = (String)spaces.get(space);
			String spaceName = inhString.substring(inhString.lastIndexOf(',') + 1);
			String spaceNameDot = spaceName + ".";
			ArrayList inh = (ArrayList) namespaces.get(spaceName);

			buf.append("\nNamespace: ");
			buf.append(spaceName);
			buf.append(" path: ");
			for (int j = 1; j < inh.size(); j++) {
				if (j > 1) {
					buf.append(", ");
				}
				buf.append(inh.get(j));
			}
			buf.append('\n');
			for (int i = 0; i < cmd.size(); i++) {
				String command = (String) cmd.get(i);
				if (!command.startsWith(spaceNameDot)) {
					if (!started) {
						continue;
					} else {
						break;
					}
				}
				started = true;
				buf.append("   ");
				buf.append(command);
				buf.append(((OberCommand)commands.get(command)).getDescription());
				buf.append('\n');
			}
		}
		buf.append("\nKey Bindings:\n");
		ArrayList keys = new ArrayList(keybindings.keySet());
		Collections.sort(keys);
		for (int i = 0; i < keys.size(); i++) {
			buf.append("   ");
			buf.append(keys.get(i));
			buf.append(((OberCommand)keybindings.get(keys.get(i))).getDescription());
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
		bindKey("System", KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_ENTER, new OberCommand(" -- execute command on line.") {
			public void execute(OberContext ctx) {
				ctx.beginningOfLine();
				executeCommand(ctx);
			}
		});
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
		addCommand("System.Split", new OberCommand(" -- Create another viewer on the same document.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().acceptViewer(createTextViewer((OberDocument)((JTextComponent)ctx.getSourceViewer().getComponent()).getDocument()));
			}
		});
		addCommand("System.Detach", new OberCommand(" -- Create another viewer on the same document.") {
			public void execute(OberContext ctx) throws Exception {
				JTextComponent comp = (JTextComponent)ctx.getSourceViewer().getComponent();
				
				((OberDocument)comp.getDocument()).removePropertyChangeListener(ctx.getSourceViewer());
				comp.setDocument(OberDocument.copy((OberDocument)comp.getDocument()));
				((OberDocument)comp.getDocument()).addPropertyChangeListener(ctx.getSourceViewer());
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
		addCommand("System.Echo", new OberCommand(" <arg> -- echo argument (example: Echo [3 + 4]).") {
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
		addCommand("System.Exec", new OberCommand(" <java expr> -- execute a java expression enclosed in square brackets (this really just retrieves the first argument as a string and discards it).") {
			public void execute(OberContext ctx) {
				ctx.getArgumentString(1);
			}
		});
		addCommand("System.Load", new OberCommand(" <filename> -- run commands in a file; each command should be on a separate line.") {
			public void execute(OberContext ctx) throws Exception {
				loadFile(ctx.getSourceViewer().filenameFor(ctx.getArgumentString(1)), ctx.getSourceViewer());
			}
		});
		addCommand("System.View", new OberCommand(" <filename> -- view a file, creating a new viewer if necessary.") {
			public void execute(OberContext ctx) throws Exception {
				ctx.getSourceViewer().findOrCreateViewerForFile(ctx.getArgumentString(1));
			}
		});
		addCommand("System.Define", new OberCommand(" <OGNL expr> -- define a command as an OGNL expression.") {
			public void execute(OberContext ctx) throws Exception {
				final Node node = (Node) ctx.getArgument(3);
				
				if (node != null) {
					addCommand(ctx.getArgumentString(1), new OberCommand(ctx.getArgumentString(2)) {
						public void execute(OberContext ctx2) throws Exception {
							Ognl.getValue(node, ctx2);
						}
					});
				}
			}
		});
	}
	public void loadFile(String filename, OberViewer viewer) throws IOException {
		FileInputStream in = new FileInputStream(filename);
		try {
			byte buf[] = new byte[1024];
			StringBuffer str = new StringBuffer();
			OberContext ctx;
			
			for (int count = in.read(buf); count > -1; count = in.read(buf)) {
				str.append(new String(buf));
			}
			ctx = new OberContext(viewer, 0, str.toString());
			for (;;) {
				if (!ctx.isComment()) {
					if (ctx.getArgumentString(0) == null) {
						break;
					}
					executeCommand(ctx);
				}
				ctx.nextLine();
			}
		} finally {
			in.close();
		}
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
		if (ctx.getArgumentString(0) != null) {
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
				try {
					cmd.execute(ctx);
				} catch (Exception ex) {
					ctx.getSourceViewer().error("Error executing command: " + ctx.getCommandString());
					ctx.getSourceViewer().error(ex);
				}
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
		try {
			File file = new File(System.getProperty("user.home", System.getProperty("user.dir")), ".oberrc");

			System.out.println("Loading file: " + file);
			if (file.exists()) {
				loadFile(file.getAbsolutePath(), v);
			}
		} catch (Exception ex) {
			v.error(ex);
		}
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
		return createTextViewer(new OberDocument());
	}
	public OberViewer cloneTextViewer(OberViewer viewer) {
		return createTextViewer((OberDocument)((JTextComponent)viewer.getComponent()).getDocument());
	}
	public OberViewer createTextViewer(final OberDocument doc) {
		final OberViewer v = new OberViewer(OberViewer.VIEWER_TYPE, this) {
			public void dying() {
				super.dying();
				doc.removePropertyChangeListener(this);
			}
		};
		OberViewer.AdaptedTextPane txt = new OberViewer.AdaptedTextPane(v);

		doc.addPropertyChangeListener(v);
		v.setComponent(txt, new JScrollPane(txt));
		v.getTag().setText("Viewer: Del, Help, Split, Detach");
		txt.setDocument(doc);
		return v;
	}
	public boolean handleKey(OberViewer viewer, KeyEvent e) {
		if (boundKeys.contains(new Integer(e.getKeyCode()))) {
			String evt = eventString(e.getModifiersEx(), e.getKeyCode());
			String prefix = viewer.getName();
			OberCommand cmd = null; 
				
			if (prefix != null) {
				ArrayList path = (ArrayList)namespaces.get(prefix);

				if (path == null && cmd == null) {
					cmd = (OberCommand)keybindings.get("System." + evt);
				} else {
					for (int i = 0; cmd == null && i < path.size(); i++) {
						cmd = (OberCommand)keybindings.get(path.get(i) + "." + evt);
					}
				}
			}
			if (cmd != null) {
				try {
					cmd.execute(new OberContext((JTextComponent)e.getSource(), viewer, ((JTextComponent)e.getSource()).getCaretPosition()));
				} catch (Exception ex) {
					viewer.error(ex);
				}
				return true;
			}
		}
		return false;
	}
	public String eventString(int modifiersEx, int keyCode) {
		StringBuffer evt = new StringBuffer(KeyEvent.getModifiersExText(modifiersEx));
		
		if (evt.length() > 0) {
			evt.append(",");
		}
		evt.append(KeyEvent.getKeyText(keyCode));
		return evt.toString();
	}
	public void bindKey(String mode, int modifiersEx, int keyCode, OberCommand cmd) {
		boundKeys.add(new Integer(keyCode));
		keybindings.put(mode + "." + eventString(modifiersEx, keyCode), cmd);
	}
}
