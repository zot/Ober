package ar.ognl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlParser;
import ognl.ParseException;

/**
 * @author Bill Burdick, Applied Reasoning
 *
 * (C) Applied Reasoning, Feb 11, 2003
 * 
 * This code is distributed under the Artistic License
 * 
 * This is object evaluates OGNL expressions embedded in strings.
 * 
 * The OGNL expressions must be enclosed in square brackets ('[' and ']').
 * 
 * see the main method for an example.
 * 
 */
public class OgnlScript {
	ArrayList objs = new ArrayList();

	public static void main(String args[]) throws OgnlException, ParseException {
		Object root = new Object() {
			public String name = "fred";
			public int age = 3;
			public String friends[] = {"mary", "jed"};
		};
		String expr = "Name: [name], Age: [age], years to go: [21\n -\n \n\nage].  Friend #1 = [friends[0]], friend #2 = [friends[1]]!";

		System.out.println("'" + expr + "' evaluates to: " + new OgnlScript(expr).getValue(root));
	}
	
	public OgnlScript(String script) throws ParseException {
		int stringStart = 0;
		int i;
		StringBuffer buf = new StringBuffer();
		int count[] = {0};

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
					count[0] = 0;
					objs.add(parseExpr(script.substring(i), count));
					stringStart = i + count[0] + 1;
					i = stringStart - 1;
					break;
			}
		}
		if (stringStart < i) {
			objs.add(script.substring(stringStart));
		}
	}
	public static Object parseExpr(String string, int count[]) throws ParseException {
		Object expr;
		OgnlParser parser = new OgnlParser(new StringReader(string));
		int pos = 0;

		try {
			expr = parser.topLevelExpression();
		} catch (ParseException ex) {
			pos = parser.token.endColumn;
			if (parser.token.endLine > 1) {
				String str[] = string.split("\n");
				
				pos = 0;
				for (int i = 0; i < parser.token.endLine - 1; i++) {
					pos += str[i].length() + 1;
				}
				pos += parser.token.endColumn;
			}
			if (parser.token.next.toString().equals("]")) {
				int i = parser.token.next.beginColumn;

				parser = new OgnlParser(new StringReader(string.substring(0,  pos)));
				expr = parser.topLevelExpression();
				pos = i - 1;
			} else {
				throw ex;
			}
		}
		count[0] = pos > 0 ? pos : parser.token.endColumn;
		return expr;
	}
	public String getValue(Object root) throws OgnlException {
		StringWriter str = new StringWriter();
		
		try {
			getValue(Ognl.createDefaultContext(root), root, null, str);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str.toString();
	} 
	public String getValue(OgnlContext oc, Object root) throws OgnlException {
		StringWriter str = new StringWriter();
		
		try {
			getValue(oc, root, null, str);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str.toString();
	}
	public void getValue(Map context, Object root, Class resultType, Writer writer) throws OgnlException, IOException {
		for (int i = 0; i < objs.size(); i++) {
			if (objs.get(i) instanceof String) {
				writer.write((String)objs.get(i));
			} else {
				writer.write(Ognl.getValue(objs.get(i), context, root, resultType).toString());
			}
		}
	}
}
