// Created on Nov 6, 2003 by Bill Burdick
package ar.ober.test;

import java.io.IOException;
import java.util.regex.Matcher;

import ar.ober.Ober;
import ar.ober.OberContext;
import ar.ober.OberGui;
import ar.ober.OberViewer;
import junit.framework.TestCase;
import junit.textui.TestRunner;

public class OberTests extends TestCase {
	Ober ober = new Ober(mockGui());

	public static void main(String args[]) {
		TestRunner.run(OberTests.class);
	}

	public OberTests(String name) {
		super(name);
	}
	public void testRegexps() {
		Matcher m = OberViewer.LINE.matcher("!ls\n\nd\n");
		
		assertTrue(m.find());
		assertEquals(0, m.start());
		assertTrue(m.find(3));
		assertEquals(4, m.start());
		assertTrue(m.find(4));
		assertEquals(4, m.start());
		assertTrue(m.find(5));
		assertEquals(5, m.start());
	}
	public void testArgs() {
		ctx("ls", "ls", 0);
		ctx("!ls", "!ls", 0);
		ctx("ls", "ls ", 0);
		ctx("!ls", "!ls ", 0);
		ctx("ls", "ls\n", 0);
		ctx("!ls", "!ls\n", 0);
		nextLine("ls", -1);
		nextLine("ls\n", -1);
		nextLine("ls\na", 3);
	}
	public void ctx(String expectedStr, String str, int start) {
		OberContext ctx = new OberContext(mockViewer("", str), start, str);
		ctx.getArgument(0);
		ctx.findArgs(ctx.cmdStart);
		ctx.beginningOfLine();
		assertEquals(expectedStr, ctx.getArgument(0));
		assertEquals(start + expectedStr.length(), ctx.nextPosition);
	}
	public void nextLine(String str, int pos) {
		OberContext ctx = new OberContext(mockViewer("", str), 0, str);
		String cmd = ctx.getArgumentString(0);
		
		ctx.nextLine();
		assertEquals(pos, ctx.nextPosition);
	}
	public OberGui mockGui() {
		return new OberGui() {
			public OberViewer createViewer() {
				return mockViewer("", "");
			}
			public void dispatch() {}
			public void dying() {}
			public String eventString(int modifiers, int keycode) {
				return null;
			}
			public String eventString(Object event) {
				return null;
			}
			public void install() {}
		};
	}
	public OberViewer mockViewer(final String initialTagText, final String initialContents) {
		OberViewer viewer = new OberViewer() {
			String contents = initialContents;
			String tagText = initialTagText;
			boolean dirty = false;

			protected void createGui() {}
			public void detachDocument() throws IOException, ClassNotFoundException {}
			public void error(String msg) {
				throw new RuntimeException(msg);
			}
			public void error(Throwable t) {
				throw new RuntimeException(t);
			}
			public int getCaretPosition() {
				return 0;
			}
			public int getDocumentLength() {
				return contents.length();
			}
			public int getTagCaretPosition() {
				return 0;
			}
			public String getTagText() {
				return tagText;
			}
			public String getText(int start, int length) {
				return contents.substring(start, length);
			}
			public int getWidth() {
				return 0;
			}
			public void insertString(int pos, String str, Object style) {
				contents = contents.substring(0, pos - 1) + str + contents.substring(pos);
				dirty = true;
			}
			public void replace(int start, int end, String str, Object style) {
				contents = contents.substring(0, start) + str + contents.substring(end, contents.length());
			}
			public void setStyle(int start, int end, Object style) {}
			public void removeStyle(Object style) {}
			public boolean inTag(Object event) {
				return false;
			}
			public boolean isDirty() {
				return dirty;
			}
			public void markClean() {
				dirty = false;
			}
			public void markDirty() {
				dirty = true;
			}
			public void removeFromParent() {}
			public void requestFocus() {}
			public void setCaretPosition(int pos) {}
			public void setPosition(float frac) {}
			public void setTagBackground(int color) {}
			public void setTagText(String text) {
				tagText = text;
			}
			public void setText(String txt) {
				contents = txt;
			}
			public void setTrackingChanges(boolean track) {}
			protected void useDocument(OberViewer viewer) {
				contents = viewer.getText(0, viewer.getDocumentLength());
			}
		};
		
		viewer.ober = ober;
		return viewer;
	}
}
