/*
(C) 2003 Bill Burdick

ar.ober.Ober

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/

package ar.ober;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import ar.ognl.OgnlScript;

import ognl.Node;
import ognl.Ognl;
import ognl.OgnlContext;

public class Ober {
	public ArrayList viewers = new ArrayList();
	protected OberViewer activeViewer;
	protected HashMap properties = new HashMap();
	protected HashMap namespaces = new HashMap();
	protected HashSet boundKeys = new HashSet();
	public OberGui gui;
	public String longListingFormat = "{\"ls\", \"-l\", \"[sourceViewer.ober.backslashify(#filename.toString())]\"}";
	
	public static final int MAIN_COLOR = 0xFFFFFF;
	public static final int TRACK_COLOR = 0xC0FFFF;
	public static final int VIEWER_COLOR = 0xC0C0C0;
	public static final int RED = 0xFF0000;
	public static final String VERSION = "0.9.5";
	public static final String ENV_OBERVAR = "OBER";
	public static final String ENV_TRUE = "TRUE";
	public static final int KEY_ENTER = -1;
	public static final int KMASK_CTRL = 1;
	public static final int KMASK_SHIFT = 2;
	
	public static Ober current;
	
	public static void main(String args[]) throws Exception {
		if (args.length != 1)  {
			System.err.println("Usage: " + Ober.class + " gui_class [file]");
			System.exit(1);
		}
		OberGui gui = (OberGui) Class.forName(args[0]).newInstance();
		Ober ober = new Ober(gui);
		OberViewer main = ober.createMain(args.length < 2 ? null : args[1]);

		ober.help(main);
		gui.dispatch();
	}
	public Ober(OberGui g) {
		gui = g;
		current = this;
		g.install();
		initialize();
	}
	public HashMap getProperties() {
		return properties;
	}
	public void executeShellCommand(final OberContext ctx)  {
		final OberViewer viewer = getActiveViewer();
		File dir;
			
		try  {
			dir = viewer.getFile();
			if (!dir.isDirectory())  {
				dir = dir.getParentFile();
			}
		} catch (Exception ex) {
			dir = new File(".");
		}
		try {
			int start = ctx.cmdStart;
			StringBuffer cmd = new StringBuffer();

			if (ctx.getArgument(0).toString().startsWith("!"))  {
				ctx.nextLine();
				int nl = ctx.nextPosition;
				int i = 0;

				if (nl == -1)  {
					nl = viewer.getDocumentLength();
				} else  {
					nl--;
				}
				ctx.findArgs(start);
				cmd.append(ctx.getArgumentString(i++).substring(1));
				while (ctx.nextPosition < nl && ctx.nextPosition != -1 && ctx.getArgument(i) != null) {
					cmd.append(' ');
					cmd.append(ctx.getArgumentString(i++));
				}
			} else {
				cmd.append(ctx.getArgumentString(0));
			}
			if (ctx.nextPosition == -1 || ctx.nextPosition == viewer.getDocumentLength())  {
				viewer.insertString(viewer.getDocumentLength(), "\n", null);
			} else  {
				viewer.insertString(viewer.getDocumentLength(), cmd.toString() + "\n", OberViewer.BOLD);
			}
			viewer.process = Runtime.getRuntime().exec(cmd.toString(), new String[] {ENV_OBERVAR, ENV_TRUE}, dir);
			viewer.insertInBackground(new InputStreamReader(viewer.process.getInputStream()), "\n> !", viewer.getCaretPosition() == viewer.getDocumentLength(), false);
		} catch (Exception e) {
			ctx.getSourceViewer().error(e);
		}
	}
	public void help(OberViewer sourceViewer) throws InstantiationException, IllegalAccessException {
		OberViewer tv = OberViewer.createTextViewer(this, sourceViewer.widestTrack());
		ArrayList spaces = new ArrayList();
		
		tv.setTagText("Help: Del, Help, Split, Detach");
		tv.setText("Welcome to Ober, version " + VERSION + ", an Oberon environment for Java.\n\n" +			"EXECUTING COMMANDS\n" +
			"To execute a command, position the mouse pointer over a word and click the third mouse " +			"button. If you do not have a three button mouse, use the second button.\n\n" +			"COMMAND ARGUMENTS\n" +			"Arguments are either words or OGNL expressions (http://www.ognl.org/) within square " +			"brackets (example: Echo [3 + 4]).  The reciever is the viewer containing the command.\n\n" +			"OGNL PROPERTIES\n" +			"focus -- The viewer with keyboard focus\n" +			"ober -- the Ober environment\n" +			"properties -- user properties for the command viewer.\n" +			"ober.properties -- global user properties.\n\n" +			"OPENING FILES\n" +
			"To open a file or a directory, type a filename and click it with the second mouse button.  If " +			"you do not have a three button mouse, control-click the filename instead.  Try one of these...\n" +
			"\tC:\\\n" +
			"\t/tmp\n\n" +			"AVAILABLE COMMANDS");
		for (Iterator i = namespaces.keySet().iterator(); i.hasNext(); ) {
			String name = (String) i.next();
			OberNamespace ns = (OberNamespace)namespaces.get(name);

			spaces.add(ns);
		}
		Collections.sort(spaces);
		for (int space = 0; space < spaces.size(); space++) {
			((OberNamespace)spaces.get(space)).help(this, tv);
		}
	}
	protected void initialize() {
		addNamespace("System", new String[]{});
		addNamespace("Text", new String[]{});
		addNamespace("File", new String[]{"Text"});
		addNamespace("Errors", new String[]{"Text"});
		bindKey("System", KMASK_CTRL, KEY_ENTER, new OberCommand(" -- execute command on line.") {
			public void execute(OberContext ctx) {
				ctx.beginningOfLine();
				executeCommand(ctx);
			}
		});
		setDefaultCommand("System", new OberCommand(" -- execute a shell command.  If the argument begins with a '!', use the text to the end of the line for the command.") {
			public void execute(OberContext ctx) throws Exception {
				executeShellCommand(ctx);
			}
		});
		addCommand("System.Abort", new OberCommand(" -- Abort background process for the active viewer.") {
			public void execute(OberContext ctx) throws InstantiationException, IllegalAccessException {
				getActiveViewer().killBackgroundThread();
			}
		});
		addCommand("System.Help", new OberCommand(" -- Show help on commands for a viewer.") {
			public void execute(OberContext ctx) throws InstantiationException, IllegalAccessException {
				help(ctx.getSourceViewer());
			}
		});
		addCommand("System.Newcol", new OberCommand(" -- Create a new column.") {
			public void execute(OberContext ctx) throws InstantiationException, IllegalAccessException {
				createTrack(ctx.getSourceViewer().topViewer());
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
						track.removeFromParent();
						break;
				}
			}
		});
		addCommand("System.New", new OberCommand(" -- Create a new column.") {
			public void execute(OberContext ctx) throws InstantiationException, IllegalAccessException {
				File current = getActiveViewer() == null ? new File("New") : getActiveViewer().getFile();
				OberViewer v = OberViewer.createTextViewer(Ober.this, ctx.getSourceViewer().widestTrack());

				if (current == null) {
					current = new File("New");
				}
				if (!current.isDirectory()) {
					current = current.getParentFile();
				}
				v.setTagText("File: " + new File(current, "New").getAbsolutePath() + " Get, Put, Del, Help, Split, Detach");
			}
		});
		addCommand("System.Split", new OberCommand(" -- Create another viewer on the same document.") {
			public void execute(OberContext ctx) throws InstantiationException, IllegalAccessException {
				OberViewer.createTextViewer(ctx.getSourceViewer(), ctx.getSourceViewer().widestTrack());
			}
		});
		addCommand("System.Detach", new OberCommand(" -- Create another viewer on the same document.") {
			public void execute(OberContext ctx) throws Exception {
				ctx.getSourceViewer().detachDocument();
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
						viewer.removeFromParent();
						break;
				}
			}
		});
		addCommand("System.Quit", new OberCommand(" -- Quit.") {
			public void execute(OberContext ctx) {
				System.exit(0);
			}
		});
		addCommand("Text.Long", new OberCommand(" -- toggle verbosity of directory listing.") {
			public void execute(OberContext ctx) {
				try {
					Object fn[] = ctx.getSourceViewer().getFilenameIndicator();
					if (fn[1] instanceof String) {
						OgnlContext oc = (OgnlContext) Ognl.createDefaultContext(ctx);
						
						oc.put("filename", ctx.getSourceViewer().getFile().getAbsolutePath());
						ctx.getSourceViewer().setFileName("[" + new OgnlScript(longListingFormat).getValue(oc, ctx) + "]");
					} else {
						ctx.getSourceViewer().setFileName(ctx.getSourceViewer().getFile().getAbsolutePath());
					}
					ctx.getSourceViewer().loadFile();
				} catch (Exception e) {
					activeViewer.error(e);
				}
			}
		});
		addCommand("Text.Get", new OberCommand(" -- Get the contents of a file into the source viewer and track changes.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().loadFile();
			}
		});
		addCommand("Text.Put", new OberCommand(" -- Save the contents of the source viewer to a file and clear changes.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().storeFile();
			}
		});
		addCommand("System.Echo", new OberCommand(" <arg> -- echo argument (example: Echo [3 + 4]).") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().error(ctx.getArgumentString(1));
			}
		});
		addCommand("Text.Clear", new OberCommand(" -- set the viewer's contents to empty.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().setText("");
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
				ctx.getSourceViewer().findOrCreateViewerForFile(getActiveViewer().filenameFor(ctx.getArgumentString(1)));
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
	public String backslashify(Object s) {
		return s.toString();
	}
	public void loadFile(File filename, OberViewer viewer) throws IOException {
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
		OberNamespace namespace = null;
		OberNamespace system = (OberNamespace) namespaces.get("System");
		
		for (int i = 0; i < path.length; i++) {
			OberNamespace ns = (OberNamespace) namespaces.get(path[i]);
			
			if (ns != null && !l.contains(path[i])) {
				l.add(ns);
			}
		}
		if (system != null && !l.contains(system)) {
			l.add(system);
		}
		namespace = new OberNamespace(space, (OberNamespace [])l.toArray(new OberNamespace[0]));
		namespaces.put(space, namespace);
	}
	public OberViewer getFocus() {
		return activeViewer;
	}
	public void addViewer(OberViewer viewer) {
		viewers.add(viewer);
	}
	public void removeViewer(OberViewer viewer) {
		viewers.remove(viewer);
		if (activeViewer != null  && (activeViewer.topViewer() == null || activeViewer.topViewer() == viewer)) {
			deactivated(activeViewer);
		}
		if (viewers.isEmpty()) {
			gui.dying();
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
			String cmdParts[] = ctx.getArgumentString(0).split("\\.");
			OberNamespace namespace = null;
			
			try {
				if (cmdParts.length == 2) {
					((OberNamespace)namespaces.get(cmdParts[0])).executeFullyqualifiedCommand(cmdParts[0], cmdParts[1], ctx);
				} else if (cmdParts.length == 1){
					String namespaceName = ctx.getSourceViewer().getName();
				
					if (namespaceName != null && (OberNamespace)namespaces.get(namespaceName) != null) {
						((OberNamespace)namespaces.get(namespaceName)).executeCommand(cmdParts[0], ctx);
					} else {
						((OberNamespace)namespaces.get("System")).executeCommand(cmdParts[0], ctx);
					}
				} else {
					ctx.getSourceViewer().error("Badly formed command: '" + ctx.getArgumentString(0) + "'");
				}
			} catch (Exception ex) {
				ctx.getSourceViewer().error(ex);
			}
		}
	}
	public void setDefaultCommand(String name, OberCommand cmd)  {
		((OberNamespace)namespaces.get(name)).defaultCommand = cmd;
	}
	public void addCommand(String name, OberCommand cmd) {
		String parts[] = name.split("\\.");
		
		if (parts.length != 2) {
			throw new RuntimeException("Illegal command name format.  Expected: <name space>.<command name>, but got: " + name);
		}
		if (namespaces.get(parts[0]) == null) {
			throw new RuntimeException("Illegal namespace: " + parts[0]);
		}
		((OberNamespace)namespaces.get(parts[0])).addCommand(parts[1], cmd);
	}

	public OberViewer createMain(String filename) throws InstantiationException, IllegalAccessException {
		OberViewer v = OberViewer.createViewer(this, null, OberViewer.MAIN_TYPE);

		v.setTagText("Ober: Newcol, New, Quit, Help");
		v.setTagBackground(MAIN_COLOR);
		try {
			File file = filename != null ? new File(filename) : new File(System.getProperty("user.home", System.getProperty("user.dir")), ".oberrc");

			if (file.exists()) {
				loadFile(file, v);
			}
		} catch (Exception ex) {
			v.error(ex);
		}
		return v;
	}
	public OberViewer createTrack(OberViewer main) throws InstantiationException, IllegalAccessException {
		OberViewer v = OberViewer.createViewer(this, main, OberViewer.TRACK_TYPE);

		v.setTagText("Track: Delcol, New, Help");
		v.setTagBackground(TRACK_COLOR);
		return v;
	}
	public boolean handleKey(OberViewer viewer, Object e) {
		String evt = gui.eventString(e);

		if (boundKeys.contains(evt))  {
			OberNamespace namespace = null;
			OberCommand cmd = null; 
				
			if (viewer.getName() != null) {
				namespace = (OberNamespace) namespaces.get(viewer.getName());
			}
			if (namespace == null) {
				namespace = (OberNamespace) namespaces.get("System");
			}
			try {
				return namespace.handleKey(evt, new OberContext(viewer, viewer.inTag(e) ? viewer.getTagCaretPosition() : viewer.getCaretPosition(), viewer.inTag(e) ? viewer.getTagText() : viewer.getText(0, viewer.getDocumentLength())));
			} catch (Exception ex) {
				viewer.error(ex);
			}
		}
		return false;
	}
	public void bindKey(String namespace, int modifiersEx, int keyCode, OberCommand cmd) {
		String evtstr = gui.eventString(modifiersEx, keyCode);

		boundKeys.add(evtstr);
		((OberNamespace)namespaces.get(namespace)).addKeybinding(evtstr, cmd);
	}
}
