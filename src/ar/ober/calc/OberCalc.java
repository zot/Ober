// Created on Nov 22, 2003 by Bill Burdick
package ar.ober.calc;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import ar.javascript.JsExpr;
import ar.javascript.JsString;

/*
 * Implement spreadsheet-like behavior.  Variables in the text label cells (single cell
 * tables are shown as simple values).  Calc executes javascript code in curlies is
 * executed sequentially and replaces the values in the text.
 * 
 * Example:
 * 
 * 	a =
 * 		|  row 1 |  1  |  2  |  3  |
 * 		|  row 2 |  4  |  5  |  e = 6  |
 * 
 * 	-- patterns
 * 	variable ::=(<word>)=[\n ]*(<value>|<row>(\n<row>)*)
 * 	word ::=[ \t]*[a-zA-Z._][a-zA-Z._0-9]*[ \t]*
 * 	value ::=<assignment>|[ \t]*[0-9.]+|[^\n]*[ \t]*
 * 	assignment ::=[ \t]*<word>[ \t]*=<value>
 * 	row ::=[ \t]*<cell>+\|
 * 	cell ::=\|<value>
 * 
 * 	b = | 5  |  7  |  g = 9  |
 * 
 * 	c = 21
 * 
 * 	d = 7
 * 
 * 	f = 18
 * 
 *  	{
 * 		b = sumColumns(a);
 * 		c = sumRows(b);
 * 		d[0] = b[1];
 * 		f = e * 3;
 * 	}
 * 
 * RUNNING A CALCULATION
 * 
 * 1) Creates a transitory OberCalc object and parses the text into it,
 * 	populating a scope object with tables and cells
 * 2) run the calculation
 * 3) render the tables back into the text
 */

public class OberCalc {
	public ArrayList values = new ArrayList();
	public ArrayList vars = new ArrayList();
	public ArrayList exprs = new ArrayList();
	public Scriptable scope;
	public Context context;
	public String text;
	public OberCalcResults results;
	public Scriptable tablePrototype;
	public Scriptable rowPrototype;

	public final static String OPT_SPACE = "[ \t]*";
	public final static String WORD = "[a-zA-Z._][a-zA-Z._0-9]*";
	public final static String SINGLE_VALUE = "[---a-zA-Z._0-9]+";
	public final static String VALUE = "(" + OPT_SPACE + WORD + OPT_SPACE + "=)?" + OPT_SPACE + "([^\n|{]+)" + OPT_SPACE;
	public final static String CELL = "\\|" + VALUE;
	public final static String ROW = "((" + CELL + ")+\\|)";
	public final static String TABLE = "(" + WORD + ")" + OPT_SPACE + "=[ \t\n]*((" + SINGLE_VALUE + ")|" + ROW +  "([ \t]*[\n][ \t\n]*" + ROW + ")*)";
	public final static String ITEM = "\\{|" + TABLE;
	public final static Pattern ITEM_PAT = Pattern.compile(ITEM);
	public final static Pattern WORD_PAT = Pattern.compile(WORD);
	public final static Pattern ROW_PAT = Pattern.compile(ROW);
	public final static Pattern CELL_PAT = Pattern.compile(CELL);
	public final static Pattern SINGLE_VALUE_PAT = Pattern.compile(SINGLE_VALUE);
	public final static String JS_CODE = "OberCalc.js";
	public final static String TABLE_PROTOTYPE_NAME = "OberTablePrototype";
	public final static String ROW_PROTOTYPE_NAME = "OberRowPrototype";

	public static void appendLevel(StringBuffer buf, int level) {
		for (int i = 0; i < level; i++) {
			buf.append('\t');
		}
	}
	public static void main(String args[]) throws Exception {
		String str = "Hello, here is a file.\n" +
		"table: a = |4|\n" +
		"table: b = |1|3| |5|7|\n" +
		"table: c = \n\n" +
		" \t |a|b|\n" +
		"|c|x = dddddddd|\n" +
		"{\n" +
		"\tx = 10;\n" +
		"}\n" +
		"table: d = 5\n" +
		"{d = 15;}";
		final StringBuffer buf = new StringBuffer();
		OberCalc calc = new OberCalc(str, new OberCalcResults() {
			public void replace(int start, int end, String str) {
				buf.delete(start, end);
				buf.insert(start, str);
			}
			public void noChange(int start, int end) {}
			public void variable(int start, int end) {}
		});
		
		calc.appendTo(buf);
		System.out.println("\n=== PARSING ===\n");
		System.out.println(buf.toString());
		System.out.println("=== FILE ===\n");
		System.out.println(str);
		System.out.println("\n=== Calculating ===\n");
		buf.setLength(0);
		buf.append(str);
		System.out.println(calc.calc());
	}
	
	public OberCalc(String text, OberCalcResults results) throws Exception {
		this.text = text;
		this.results = results;
		setUp();
		try {
			parse();
			calc();
		} finally {
			tearDown();
		}
	}
	public void setUp() throws Exception {
		context = Context.enter(new Context());
		JsString.setupContext(context);
		scope = context.initStandardObjects(null);
		InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(JS_CODE));
		context.evaluateReader(scope, reader, JS_CODE, 1, null);
		reader.close();
		tablePrototype = (Scriptable) scope.get(TABLE_PROTOTYPE_NAME, scope);
		rowPrototype = (Scriptable) scope.get(ROW_PROTOTYPE_NAME, scope);
	}
	public void tearDown() {
		Context.exit();
	}
	public void parse() throws Exception {
		Matcher itemMatcher = ITEM_PAT.matcher(text);
		int start = 0;

		while (itemMatcher.find(start)) {
			if (text.charAt(itemMatcher.start()) == '{') {
				JsExpr expr = new JsExpr(scope, context, text.substring(itemMatcher.start() + 1), '}');
				
				exprs.add(expr);
				start += expr.source.length();
			} else {
				String name = null;
				if (itemMatcher.start(1) != -1) {
					name = text.substring(itemMatcher.start(1), itemMatcher.end(1));
				}
				Object var = null;
				if (itemMatcher.start(3) != -1) {
					var = new OberCalcCell(this, itemMatcher.start(1), name, text.substring(itemMatcher.start(3), itemMatcher.end(3)), text, itemMatcher.start(1), itemMatcher.end(3));
				} else {
					var = OberCalcTable.parse(this, itemMatcher.start(1), name, text, itemMatcher.start(2), itemMatcher.end());
				}
				values.add(var);
				start = itemMatcher.end();
			}
		}
	}
	public void appendTo(StringBuffer buf) {
		int off = 0;

		buf.append("Calc");
		for (int i = 0; i < values.size(); i++) {
			OberCalcVariable value = (OberCalcVariable)values.get(i);
			
			buf.append("\n");
			value.appendTo(buf, 1);
		}
		for (int i = 0; i < exprs.size(); i++) {
			String src = ((JsExpr)exprs.get(i)).source;

			buf.append("\nScript: {");
			buf.append(src.substring(0, src.length() - 1));
			buf.append("}");
		}
	}
	public String calc() throws Exception {
		int last = 0;
		int offset = 0;
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < exprs.size(); i++) {
			((JsExpr)exprs.get(i)).getValue(scope);
		}
		Context.enter(new Context());
		try {
			for (int i = 0; i < vars.size(); i++) {
				OberCalcVariable var = (OberCalcVariable) vars.get(i);
				Object obj = ScriptableObject.getProperty(scope, var.name);
				String objStr;
				
				if (obj instanceof NativeJavaObject) {
					objStr = ((NativeJavaObject)obj).unwrap().toString();
				} else {
					objStr = Context.toString(obj);
				}
				objStr = var.format(objStr);
				if (objStr.equals(var.source)) {
					results.noChange(var.start + offset, var.start + var.length + offset);
					markVar(var, offset);
				} else {
					results.replace(var.start + offset, var.start + var.length + offset, objStr);
					markVar(var, offset);
					offset += objStr.length() - var.length;
				}
			}
		} finally {
			Context.exit();
		}
		buf.append(text.substring(last, text.length()));
		return buf.toString();
	}
	public void markVar(OberCalcVariable var, int offset) {
		if (var.name != null) {
			results.variable(var.nameStart + offset, var.nameStart + var.name.length() + offset);
		}
	}
	public void putVar(OberCalcVariable var, Object value) {
		if (ScriptableObject.hasProperty(scope, var.name)) {
			throw new OberCalcException(var, "duplicate variable: " + var.name);
		}
		vars.add(var);
		setVar(var, value);
	}
	public void setVar(OberCalcVariable var, Object value) {
		ScriptableObject.putProperty(scope, var.name, value);
	}
	public void removeVar(OberCalcVariable var) {
		vars.remove(var);
		ScriptableObject.deleteProperty(scope, var.name);
	}
	public void replaceVar(OberCalcVariable oldVar, OberCalcVariable newVar, Object value) {
		vars.set(vars.indexOf(oldVar), newVar);
		setVar(newVar, value);
	}
}
