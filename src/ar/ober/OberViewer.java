/*
(C) 2003 Bill Burdick

ar.ober.OberViewer

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober;

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
	public static Object BOLD;
	public static Object BOLD_RED;
	
	public static OberViewer createTextViewer(Ober o, OberViewer parent) throws InstantiationException, IllegalAccessException  {
		return createViewer(o, parent, VIEWER_TYPE);
	}
	public static OberViewer createViewer(Ober o, OberViewer parent, int type) throws InstantiationException, IllegalAccessException  {
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
	public OberViewer widestTrack() throws InstantiationException, IllegalAccessException {
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
			v.setName(name);
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
	public String[] getFilename() {
		String txt = getTagText();
		Matcher match = TAG_FILE_PATTERN.matcher(txt);

		if (match.find()) {
			return new String[]{txt.substring(match.start(1), match.end(1)), txt.substring(match.start(2), match.end(2))};
		}
		return null;
	}
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
			int pos = getCaretPosition();
			setText(str.toString());
			setCaretPosition(Math.min(pos, getDocumentLength()));
			setTrackingChanges(true);
			markClean();
		}
	}
	public void storeFile() {
		String filename[] = getFilename();
		FileOutputStream fout = null;

		try {
			fout = new FileOutputStream(filename[1]);
			fout.write(getText(0, getDocumentLength()).getBytes());
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
				String filename = viewer.filenameFor(txt.substring(m.start(2), m.end(2)));
				findOrCreateViewerForFile(filename);
				return;
			}
		}
	}
	public void findOrCreateViewerForFile(String filename) throws InstantiationException, IllegalAccessException {
		OberViewer fileViewer = topViewer().findViewerForFile(filename);
		
		if (fileViewer == null) {
			fileViewer = createTextViewer(ober, widestTrack());
			fileViewer.setTagText("File: " + filename + " Get, Put, Del, Help, Split");
			fileViewer.loadFile();
		}
		fileViewer.requestFocus();
	}
	public String filenameFor(String string) {
		String parent[] = getFilename();
		File f = new File(string);
		
		if (parent != null && !f.isAbsolute()) {
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
