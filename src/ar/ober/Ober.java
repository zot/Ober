/*
(C) 2003 Bill Burdick

ar.ober.Ober

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/

package ar.ober;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class Ober implements PropertyChangeListener {
	protected ArrayList viewers = new ArrayList();
	protected OberViewer activeViewer;
	protected HashMap commands = new HashMap();
	protected HashMap properties = new HashMap();

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
		ArrayList cmd = new ArrayList(commands.keySet());
		StringBuffer buf = new StringBuffer("Welcome to Ober, an Oberon environment for Java.\n\n" +			"EXECUTING COMMANDS\n" +
			"To execute a command, position the mouse pointer over a word and click the third mouse " +			"button. If you do not have a three button mouse, use the second button.\n\n" +			"COMMAND ARGUMENTS\n" +			"Arguments are either words or OGNL expressions (http://www.ognl.org/) within square " +			"brackets (example: Echo [3 + 4]).  The reciever is the viewer containing the command.\n\n" +			"OGNL PROPERTIES\n" +			"focus -- The viewer with keyboard focus\n" +			"ober -- the Ober environment\n" +			"properties -- user properties for the command viewer.\n" +			"ober.properties -- global user properties.\n\n" +			"OPENING FILES\n" +
			"To open a file or a directory, type a filename and click it with the second mouse button.  If " +			"you do not have a three button mouse, control-click the filename instead.  Try one of these...\n" +
			"\tC:\\\n" +
			"\t/tmp\n\n" +			"AVAILABLE COMMANDS\n");

		Collections.sort(cmd);
		for (int i = 0; i < cmd.size(); i++) {
			buf.append(cmd.get(i));
			buf.append(((OberCommand)commands.get(cmd.get(i))).getDescription());
			buf.append('\n');
		}
		((JTextPane)tv.component).setText(buf.toString());
		sourceViewer.acceptViewer(tv);
	}
	protected void initialize() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", this);
		addCommand("Help", new OberCommand(" -- Show help on commands for a viewer.") {
			public void execute(OberContext ctx) {
				help(ctx.getSourceViewer());
			}
		});
		addCommand("Newcol", new OberCommand(" -- Create a new column.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().acceptViewer(createTrack());
			}
		});
		addCommand("Delcol", new OberCommand(" -- Delete a column.") {
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
		addCommand("New", new OberCommand(" -- Create a new column.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().acceptViewer(createTextViewer());
			}
		});
		addCommand("Del", new OberCommand(" -- Delete a viewer.") {
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
		addCommand("Quit", new OberCommand(" -- Quit.") {
			public void execute(OberContext ctx) {
				System.exit(0);
			}
		});
		addCommand("Get", new OberCommand(" -- Get the contents of a file into the source viewer and track changes.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().loadFile();
			}
		});
		addCommand("Put", new OberCommand(" -- Save the contents of the source viewer to a file and clear changes.") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().storeFile();
			}
		});
		addCommand("Echo", new OberCommand(" -- echo argument (example: Echo [3 + 4]).") {
			public void execute(OberContext ctx) {
				ctx.getSourceViewer().error(ctx.getArgumentString(1));
			}
		});
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
			OberCommand cmd = (OberCommand)commands.get(ctx.getArgumentString(0));
			
			if (cmd != null) {
				cmd.execute(ctx);
			} else {
				ctx.getSourceViewer().error("No command called '" + ctx.getArgumentString(0) + "'");
			}
		}
	}
	public void addCommand(String name, OberCommand cmd) {
		commands.put(name, cmd);
	}

	public OberViewer createMain() {
		OberViewer v = new OberViewer(OberViewer.MAIN_TYPE, this);
		JPanel panel = new JPanel();
	
		v.tagPanel.remove(v.dragger);
		panel.setLayout(new OberLayout(v, false));
		v.setComponent(panel, panel);
		v.getTag().setText("Ober: Newcol, New, Quit, Help");
		return v;
	}

	public OberViewer createTrack() {
		OberViewer v = new OberViewer(OberViewer.TRACK_TYPE, this);
		JPanel panel = new JPanel();
	
		panel.setLayout(new OberLayout(v, true));
		v.setComponent(panel, panel);
		v.getTag().setText("Track: Delcol, New, Help");
		return v;
	}

	public OberViewer createTextViewer() {
		final OberViewer v = new OberViewer(OberViewer.VIEWER_TYPE, this) {
			public void loadFile() {
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
								System.out.println("appending: " +  new String(buf, 0, count));
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
					((JTextComponent)component).setCaretPosition(pos);
				}
				trackChanges = true;
				markClean();
			}
			public void storeFile() {
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
		};
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
