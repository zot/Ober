/*
(C) 2003 Bill Burdick

ar.ober.OberContext

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober;

import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;

import javax.swing.text.JTextComponent;

import ognl.Node;
import ognl.Ognl;
import ognl.OgnlException;
import ognl.ParseException;

import ar.ognl.OgnlScript;

public class OberContext {
	protected JTextComponent textComponent;
	protected int cmdStart = 0;
	protected int nextPosition = 0;
	protected String doc;
	protected ArrayList args = new ArrayList();
	protected OberViewer sourceViewer;
	
	public OberContext(MouseEvent e, JTextComponent component, OberViewer viewer) {
		textComponent = component;
		sourceViewer = viewer;
		findArgs(component.viewToModel(e.getPoint()));
	}
	public OberContext(JTextComponent component, OberViewer viewer, int pos) {
		textComponent = component;
		sourceViewer = viewer;
		doc = component.getText();
		cmdStart = pos;
	}
	public void findArgs(int loc) {
		doc = textComponent.getText();
		Matcher m = OberViewer.LINE.matcher(doc);

		while (m.find()) {
			if (m.start() <= loc && m.end() >= loc) {
				Object arg;
				int count[] = {0};

				nextPosition = m.start();
				do {
					arg = fetchArg(count);
				} while (nextPosition <= loc);
				if (nextPosition - count[0] < loc) {
					args.add(arg);
					return;
				}
			}
		}
		nextPosition = -1;
	}
	public Object fetchArg(int count[]) {
		if (nextPosition > -1) {
			Matcher m = OberViewer.ARG_PATTERN.matcher(doc);

			if (m.find(nextPosition)) {
				if (doc.charAt(m.start()) == '[') {
					try {
						Object expr = OgnlScript.parseExpr(doc.substring(m.start() + 1), count);
						count[0] += 2;
						nextPosition = m.start() + count[0];
						return expr;
					} catch (ParseException e) {
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
		
		if (arg instanceof Node) {
			try {
				return Ognl.getValue(arg, sourceViewer).toString();
			} catch (OgnlException e) {
				sourceViewer.error(e);
			}
		}
		return (String)arg;
	}
	public OberViewer getSourceViewer() {
		return sourceViewer;
	}
	protected void beginningOfLine() {
		Matcher m = OberViewer.LINE.matcher(doc);

		while (m.find()) {
			if (m.start() <= cmdStart && m.end() >= cmdStart) {
				Object arg;
				int count[] = {0};

				cmdStart = m.start();
				nextPosition = cmdStart;
				args.add(fetchArg(count));
				return;
			}
		}
	}
}
