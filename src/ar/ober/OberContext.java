/*
(C) 2003 Bill Burdick

ar.ober.OberContext

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;

import ar.javascript.JsExpr;

public class OberContext {
	public int cmdStart = 0;
	public int nextPosition = 0;
	public String doc;
	public ArrayList args = new ArrayList();
	public OberViewer sourceViewer;
	
	public OberContext(OberViewer viewer, int pos, String str) {
		sourceViewer = viewer;
		doc = str;
		nextPosition = cmdStart = pos >= str.length() ? str.length() - 1 : pos;
	}
	public OberContext findArgs()  {
		return findArgs(cmdStart);
	}
	public OberContext findArgs(int loc) {
		Matcher m = OberViewer.LINE.matcher(doc);

		args.clear();
		while (m.find()) {
			if (m.start() <= loc && m.end() >= loc) {
				Object arg;
				int count[] = {0};

				nextPosition = m.start();
				do {
					arg = fetchArg(count);
				} while (nextPosition <= loc);
				if (nextPosition - count[0] <= loc) {
					args.add(arg);
					cmdStart = nextPosition - count[0];
				}
				return this;
			}
		}
		nextPosition = -1;
		return this;
	}
	public Object fetchArg(int count[]) {
		if (nextPosition > -1) {
			Matcher m = OberViewer.ARG_PATTERN.matcher(doc);

			if (m.find(nextPosition)) {
				if (doc.charAt(m.start()) == '[') {
					try {
						Object expr = new JsExpr(doc.substring(m.start() + 1));
						count[0] += 2;
						nextPosition = m.start() + count[0];
						return expr;
					} catch (Exception e) {
						StringWriter buf = new StringWriter();
									
						buf.write("Error parsing arguments after '");
						buf.write(doc.substring(0, nextPosition + m.start()));
						buf.write("' error:\n");
						e.printStackTrace(new PrintWriter(buf));
						sourceViewer.error(buf.toString());
					}
				} else {
					nextPosition = m.end();
					count[0] = m.end() - m.start();
					return doc.substring(m.start(), m.end());
				}
			}
			nextPosition = -1;
		}
		return null;
	}
	public String toString() {
		return "Command in " + sourceViewer + ": " + args.toString();
	}
	public ArrayList getArguments() {
		return args;
	}
	public Object getArgument(int i) {
		while (args.size() <= i) {
			int count[] = {0};
			Object arg = fetchArg(count);
			
			if (arg == null) {
				return null;
			}
			args.add(arg);
		}
		return args.get(i);
	}
	public String getArgumentString(int i) {
		Object arg = getArgument(i);
		
		if (arg instanceof JsExpr) {
			try {
				JsExpr expr = (JsExpr) arg;

				sourceViewer.ober.addProperties(expr);
				expr.put("ober", sourceViewer.ober);
				return String.valueOf(expr.getValue(this));
			} catch (Exception e) {
				sourceViewer.error(e);
			}
		}
		return arg == null ? null : arg.toString();
	}
	public Object getArgumentObject(int i) {
		Object arg = getArgument(i);
		
		if (arg instanceof JsExpr) {
			try {
				return ((JsExpr)arg).getValue(this);
			} catch (Exception e) {
				sourceViewer.error(e);
			}
		}
		return arg;
	}
	public OberViewer getSourceViewer() {
		return sourceViewer;
	}
	public void beginningOfLine() {
		Matcher m = OberViewer.LINE.matcher(doc);
		int oldStart = cmdStart;

		args.clear();
		nextPosition = cmdStart = 0;
		while (m.find() && m.start() <= oldStart) {
			nextPosition = cmdStart = m.start();
		}
	}
	public void setStart(int pos) {
		args.clear();
		nextPosition = cmdStart = pos;
	}
	// move nextPosition to the beginning of the next line
	public void nextLine() {
		Matcher m = OberViewer.LINE.matcher(doc);

		nextPosition++;
		args.clear();
		if (nextPosition >= doc.length()) {
			nextPosition = -1;
		} else if (!m.find(nextPosition)) {
			if (nextPosition < doc.length() - 1 && doc.endsWith("\n")) {
				nextPosition = doc.length() - 1;
			} else {
				nextPosition = -1;
			}
		} else {
			cmdStart = nextPosition = m.start();
		}
	}
	public boolean isComment() {
		return OberViewer.COMMENT_PATTERN.matcher(doc.substring(cmdStart)).lookingAt();
	}
	public String getCommandString() {
		return doc.substring(cmdStart, nextPosition);
	}
}
