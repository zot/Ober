package ar.javascript;

import java.io.IOException;
import java.io.StringReader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

public class JsExpr {
	public Scriptable parentScope;
	public Script script;
	public String source;

	public JsExpr(String str) throws IOException {
		Context context = Context.enter(new Context());

		try {
			JsString.setupContext(context);
			parentScope = context.initStandardObjects(null);
			parse(context, str, ']');
		} finally {
			Context.exit();
		}
	}
	public JsExpr(Scriptable scope, Context context, String string, char terminator) throws IOException {
		parentScope = scope;
		parse(context, string, terminator);
	}
	public void parse(Context context, String string, char terminator) throws IOException {
		try {
			script = context.compileReader(parentScope, new StringReader(string), "input", 1, null);
			source = string;
		} catch (EcmaError er) {
			int pos = er.getColumnNumber();

			if (er.getLineNumber() > 1) {
				String str[] = string.split("\n");
			
				pos = 0;
				for (int i = 0; i < er.getLineNumber() - 1; i++) {
					pos += str[i].length() + 1;
				}
				pos += er.getColumnNumber();
			}
			if (string.charAt(pos - 1) == terminator) {
				source = string.substring(0, pos - 1) + ";";
				script = context.compileReader(parentScope, new StringReader(source), "input", 1, null);
			} else {
				throw er;
			}
		}
	}
	public Object getValue(Object root) throws Exception {
		Context context = Context.enter(new Context());

		try {
			return getValue(context, root);
		} finally {
			Context.exit();
		}
	}
	public Object getValue(Context context, Object root) throws Exception {
		return script.exec(context, Context.toObject(root, parentScope));
	}
	public void put(String string, Object value) {
		parentScope.put(string, parentScope, value);
	}
}
