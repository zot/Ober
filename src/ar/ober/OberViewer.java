/*
(C) 2003 Bill Burdick

ar.ober.OberViewer

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class OberViewer {
	public abstract boolean inTag(Object event);
	public abstract void setTagText(String text);
	public abstract void setTagBackground(int color);
	public abstract int getWidth();
	public abstract void setPosition(float frac);
	public abstract void setText(String txt);
	public abstract String getText(int start, int length);
	public abstract int getCaretPosition();
	public abstract int getDocumentLength();
	public abstract void insertString(int pos, String str, Object style);
	public abstract void replace(int start, int end, String str, Object style);
	public abstract void setStyle(int start, int end, Object style);
	public abstract void removeStyle(Object style);
	public abstract void setCaretPosition(int pos);
	public abstract void removeFromParent();
	public abstract void requestFocus();
	public abstract String getTagText();
	public abstract int getTagCaretPosition();
	public abstract boolean isDirty();
	public abstract void markClean();
	public abstract void markDirty();
	public abstract void setTrackingChanges(boolean track);
	public abstract void detachDocument() throws IOException, ClassNotFoundException;
	protected abstract void useDocument(OberViewer viewer);
	protected abstract void createGui();
	
	protected boolean active = false;
	public Ober ober;
	protected OberViewer parentViewer;
	protected ArrayList children = new ArrayList();
	public int type;
	protected HashMap properties = new HashMap();
	protected Process process;
	protected Object writeLock = new Object();
	protected int backgroundMode = WAITING;

	public static final int WAITING = 0;
	public static final int KILL = 1;
	public static final int RUNNING = 2;
	public static final int VIEWER_TYPE = 0;
	public static final int TRACK_TYPE = 1;
	public static final int MAIN_TYPE = 2;
	public static final Pattern LINE = Pattern.compile("(?m)^.*$");
	public static final String VIEWER_NAME = "^[a-zA-Z0-9_]+(?=:)";
	public static final Pattern VIEWER_NAME_PATTERN = Pattern.compile(VIEWER_NAME);
	//public static final String ARG = "(?<=^|[^a-zA-Z0-9_<>|!.:$])[a-zA-Z0-9_<>|!.:$]+(?=[^a-zA-Z0-9_<>|!.:$]|$)|\\[";
	public static final String ARG = "([-a-zA-Z0-9_<>|!.:$/]|\\\\\\[)+|\\[";
	public static final Pattern ARG_PATTERN = Pattern.compile(ARG);
	public static final String FILE = "((?:[a-zA-Z]+://(?:[a-zA-Z0-9._\\-]+(?:(?::[a-zA-Z0-9._\\-]+)?@[a-zA-Z0-9._\\-]+)?)?)?)(\\[|(?:[a-zA-Z]:)?[a-zA-Z0-9./\\\\_\\-:]+)(?:[^a-zA-Z0-9./\\\\_\\-:]|$)";
	public static final Pattern FILE_PATTERN = Pattern.compile(FILE);
	public static final String TAG_FILE = "File:\\s" + FILE;
	public static final Pattern TAG_FILE_PATTERN = Pattern.compile(TAG_FILE);
	public static final Pattern COMMENT_PATTERN = Pattern.compile("\\s*#");
	public static Object BOLD;
	public static Object BOLD_RED;
	public static Object CALC_VARIABLE;
	public static Object CALC_NEW_VALUE;
	public static Object CALC_OLD_VALUE;
	public static Object PLAIN;
	
	public static OberViewer createTextViewer(Ober o, OberViewer parent) throws InstantiationException, IllegalAccessException  {
		return createViewer(o, parent, VIEWER_TYPE);
	}
	public static OberViewer createViewer(Ober o, OberViewer parent, int type) {
		OberViewer v = o.gui.createViewer();
		
		v.ober = o;
		v.type = type;
		v.parentViewer = parent;
		v.createGui();
		return v;
	}
	public static OberViewer createTextViewer(OberViewer source, OberViewer parent) throws InstantiationException, IllegalAccessException  {
		OberViewer viewer = createViewer(source.ober, parent, VIEWER_TYPE);

		viewer.setTagText("Viewer: Del, Help, Split, Detach");
		viewer.useDocument(source);
		return viewer;
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
	public void dying() {
		ober.removeViewer(OberViewer.this);
	}
	public OberViewer widestTrack() {
		if (type != MAIN_TYPE) {
			return parentViewer.widestTrack();
		}
		int maxWidth = 0;
		int trackIndex = 0;

		if (children.isEmpty()) {
			return ober.createTrack(this);
		}
		for (int i = 0; i < children.size(); i++) {
			int wid = ((OberViewer)children.get(i)).getWidth();

			if (wid > maxWidth) {
				maxWidth = wid;
				trackIndex = i;
			}
		}
		return (OberViewer)children.get(trackIndex);
	}
	public boolean handleKey(Object e) {
		return ober.handleKey(this, e);
	}
	public void executeCommand(OberContext ctx) {
		if (!ctx.getArguments().isEmpty()) {
			ober.executeCommand(ctx);
		}
	}
	public void killBackgroundThread() {
		synchronized (writeLock) {
			if (backgroundMode != WAITING) {
				backgroundMode = KILL;
			}
		}
	}
	public void insertInBackground(Reader rdr, String prompt, boolean moveCaret, boolean clear) {
		new Thread(new BackgroundInserter(rdr, prompt, moveCaret, clear)).start();
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
	public void addChild(OberViewer viewer) {
		children.add(viewer);
	}
	public void removeChild(OberViewer viewer) {
		children.remove(viewer);
	}
	public void error(Throwable t) {
		StringWriter w = new StringWriter();
		
		t.printStackTrace(new PrintWriter(w));
		error(w.toString());
	}
	public void error(String msg) {
		System.out.println(msg);
		try {
			msg("Errors", msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public OberViewer topViewer() {
		return type == MAIN_TYPE ? this : parentViewer == null ? null : parentViewer.topViewer();
	}
	protected void msg(String name, String msg) throws InstantiationException, IllegalAccessException {
		OberViewer v = topViewer().findOrCreateViewerNamed(name);

		msg += "\n";
		v.insertString(v.getDocumentLength(), msg, null);
	}
	public OberViewer findOrCreateViewerNamed(String name) throws InstantiationException, IllegalAccessException  {
		OberViewer v = findViewerNamed(name);
		
		if (v == null)  {
			v = createTextViewer(ober, widestTrack());
			v.setTagText(name + ": Del, Help, Split, Detach");
		}
		return v;
	}
	protected OberViewer findViewerNamed(String string) {
		if (string.equals(getName())) {
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
	protected OberViewer findViewerForFile(File file) {
		File filename = getFile();

		if ("File".equals(getName()) && file.equals(filename == null ? null : filename)) {
			return this;
		}
		for (int i = 0; i < children.size(); i++) {
			OberViewer v = getChild(i).findViewerForFile(file);
			
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
		String tagText = getTagText();
		Matcher m = VIEWER_NAME_PATTERN.matcher(tagText);

		return m.find() ? tagText.substring(m.start(), m.end()) : null;
	}
	public void setName(String name) {
		String tagText = getTagText();
		Matcher m = VIEWER_NAME_PATTERN.matcher(tagText);

		if (m.find()) {
			setTagText(tagText.substring(0, m.start()) + name + tagText.substring(m.end(), tagText.length()));
		} else {
			setTagText(name + ": " + tagText);
		}
	}
	public String toString() {
		return "Viewer: " + getName();
	}
	public Reader getFileContents() {
		Object fn[] = getFilenameIndicator();
		
		try {
			if (fn == null) {
				error("No file for this viewer.");
			} else if (fn[1] instanceof String) {
				File file = getFile();
				StringBuffer str = new StringBuffer();
	
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
					return new StringReader(str.toString());
				} else {
					return new FileReader((String)fn[1]);
				}
			} else if (fn[1] instanceof ArrayList) {
				process = Runtime.getRuntime().exec((String[])((ArrayList)fn[1]).toArray(new String[0]));
				return new InputStreamReader(process.getInputStream());
			}
			error("Invalid file: " + fn[1]);
		} catch (Exception ex) {
			error(ex);
		}
		return null;
	}
	public File getFile() {
		Object fn[] = getFilenameIndicator();

		if (fn == null) {
			return null;
		}
		if (fn[1] instanceof List) {
			// ASSUME that the last element of the list is the filename for the viewer
			fn[1] = ((List)fn[1]).get(((List)fn[1]).size() - 1);
		}
		if (fn[1] instanceof String) {
			return new File((String)fn[1]);
		}
		return null;
	}
	public Object[] getFilenameIndicator() {
		String txt = getTagText();
		Matcher match = TAG_FILE_PATTERN.matcher(txt);

		if (match.find()) {
			String tag = txt.substring(match.start(1), match.end(1));
			String filename = txt.substring(match.start(2), match.end(2));

			if (filename.charAt(0) == '[') {
				return new Object[] {tag, new OberContext(this, match.start(2), txt).getArgumentObject(0)};
			}
			return new Object[] {tag, filename};
		}
		return null;
	}
	public void setFileName(String fn) {
		String txt = getTagText();
		Matcher match = TAG_FILE_PATTERN.matcher(txt);
		int len = 0;

		if (match.find()) {
			if (txt.charAt(match.start(2)) == '[') {
				OberContext ctx = new OberContext(this, match.start(2), txt);

				ctx.getArgumentObject(0);
				len = ctx.nextPosition;
			} else {
				len = match.end(2);
			}
			setTagText(txt.substring(0, match.end(1)) + fn + txt.substring(len));
		}
	}
	public void loadFile() {
		Reader rdr = getFileContents();
		if (rdr != null) {
			insertInBackground(rdr, null, false, true);
		}
	}
	public void storeFile() {
		File file = getFile();
		FileOutputStream fout = null;

		if (file == null) {
			error("This viewer doesn't have a file!");
		}
		try {
			fout = new FileOutputStream(file);
			fout.write(getText(0, getDocumentLength() - 1).getBytes());
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
	// TODO factor out matching so we can do it line-by-line
	// public Matcher findPattern(Pattern pat, )
	public void findFile(OberViewer viewer, String txt, int loc) throws InstantiationException, IllegalAccessException {
		Matcher m = FILE_PATTERN.matcher(txt);

		while (m.find()) {
			if (m.start() <= loc && m.end() >= loc) {
				findOrCreateViewerForFile(viewer.filenameFor(txt.substring(m.start(2), m.end(2))));
				return;
			}
		}
	}
	public void findOrCreateViewerForFile(File file) throws InstantiationException, IllegalAccessException {
		OberViewer fileViewer = topViewer().findViewerForFile(file);
		
		if (fileViewer == null) {
			fileViewer = createTextViewer(ober, widestTrack());
			fileViewer.setTagText("File: " + file.getAbsolutePath() + (file.isDirectory() ? "/ Long," : "") + " Get, Put, Del, Help, Split");
			fileViewer.loadFile();
		}
		fileViewer.requestFocus();
	}
	public File filenameFor(String string) {
		File parent = getFile();
		File f = new File(string);
		
		if (parent != null && !f.isAbsolute()) {
			f = new File(parent.isDirectory() ? parent : parent.getParentFile(), string);
		}
		return f;
	}
	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}
	public Object getProperty(String name) {
		return properties.get(name);
	}
	public class BackgroundInserter implements Runnable {
			int oldPos;
			boolean moveCaret;
			char buf[] = new char[1024];
			int count;
			Reader rdr;
			String prompt;
			boolean clear;

			public BackgroundInserter(Reader rdr, String prompt, boolean moveC, boolean clear) {
				moveCaret = moveC;
				this.rdr = rdr;
				this.prompt = prompt;
				this.clear = clear;
			}
			public void initialize() {
				oldPos = getCaretPosition();
				if (oldPos == getDocumentLength() && getDocumentLength() > 0) {
					moveCaret = true;
				}
				if (clear) {
					setText("");
				}
			}
			public void insertSnippet(String snippet) {
				insertString(-1, snippet, null);
			}
			public void finish() {
				if (prompt != null) {
					insertString(-1, prompt, OberViewer.BOLD);
				}
				if (moveCaret)  {
					setCaretPosition(getDocumentLength());
				} else {
					setCaretPosition(oldPos);
				}
			}
			public void run() {
				Thread t = Thread.currentThread();
				
				try {
					synchronized (writeLock) {
						while (backgroundMode != WAITING) {
							writeLock.wait();
						}
						backgroundMode = RUNNING;
					}
					initialize();
					while ((count = rdr.read(buf)) != -1)  {
						synchronized (writeLock) {
							if (backgroundMode == KILL) {
								if (process != null) {
									process.destroy();
								}
								throw new Exception("Background insert aborted.");
							}
						}
						insertSnippet(new String(buf, 0, count));
					}
					finish();
				} catch (Exception e) {
					error(e);
				} finally  {
					try {
						rdr.close();
					} catch (IOException e) {
						error(e);
					}
					synchronized (writeLock) {
						backgroundMode = WAITING;
					}
				}
			}
	}
}
