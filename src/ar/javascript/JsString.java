package ar.javascript;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

public class JsString {
	public ArrayList objs = new ArrayList();
	public Scriptable parentScope;

	public static class Duh {
		public String name = "fred";
		public int age = 3;
		public String friends[] = {"mary", "jed"};
	}
	public static void main(String args[]) throws Exception {
		Duh root = new Duh();
		String expr = "Name: [name ], Age: [age], years to go: [21\n -\n \n\nage].\nFriend #1 = [friends[0]], friend #2 = [friends[1]]!";

		//System.getSecurityManager().
		//new ReflectPermission("suppressAccessChecks");
		System.out.println("'" + expr + "' evaluates to: " + new JsString(expr).getValue(root));
	}

	public JsString(String script) throws IOException {
		int stringStart = 0;
		int i;
		StringBuffer buf = new StringBuffer();
		int count[] = {0};

		Context context = Context.enter(new Context());
		try {
			setupContext(context);
			parentScope = context.initStandardObjects(null);
			for (i = 0; i < script.length(); i++) {
				switch (script.charAt(i)) {
					case '\\':
						i++;
						break;
					case '[':
						if (stringStart < i) {
							objs.add(script.substring(stringStart, i));
						}
						i++;
						JsExpr expr = new JsExpr(parentScope, context, script.substring(i), ']');
						objs.add(expr);
						stringStart = i + expr.source.length() + 1;
						i = stringStart - 1;
						break;
				}
			}
		} finally {
			Context.exit();
		}
		if (stringStart < i) {
			objs.add(script.substring(stringStart));
		}
	}
	public String getValue(Object root) throws Exception {
		StringWriter str = new StringWriter();
		
		getValue(root, str);
		return str.toString();
	} 
	public void getValue(Object root, Writer writer) throws Exception {
		Context context = Context.enter(new Context());

		try {
			Scriptable rootObj = Context.toObject(root, parentScope);
			for (int i = 0; i < objs.size(); i++) {
				if (objs.get(i) instanceof String) {
					writer.write((String)objs.get(i));
				} else {
					writer.write(Context.toString(((JsExpr)objs.get(i)).getValue(context, rootObj)));
				}
			}
		} finally {
			Context.exit();
		}
	}

	public static void setupContext(Context context) {
		context.setErrorReporter(new ErrorReporter() {
			public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {}
			public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
				throw new EcmaError(null, sourceName, line, lineOffset, lineSource);
			}
			private String generateErrorMessage(String message, String sourceName, int line) {
				StringBuffer buf = new StringBuffer(message);
				buf.append(" (");
				if (sourceName != null) {
					buf.append(sourceName);
					buf.append("; ");
				}
				if (line > 0) {
					buf.append("line ");
					buf.append(line);
				}
				buf.append(')');
				return buf.toString();
			}
			public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
				StringBuffer buf = new StringBuffer(message);
				buf.append(" (");
				if (sourceName != null) {
					buf.append(sourceName);
					buf.append("; ");
				}
				if (line > 0) {
					buf.append("line ");
					buf.append(line);
				}
				buf.append(')');
				return new EvaluatorException(buf.toString());
			}
		});
	}
}
