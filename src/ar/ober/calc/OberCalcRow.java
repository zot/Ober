// Created on Nov 22, 2003 by Bill Burdick
package ar.ober.calc;

import java.util.ArrayList;
import java.util.regex.Matcher;

import org.mozilla.javascript.NativeArray;

public class OberCalcRow extends OberCalcVariable {
//	offset of row's start from end of previous element (or start of table for first row)
	public ArrayList cells = new ArrayList();
	public String leadingWhitespace;

	public OberCalcRow(OberCalc calc, int nameStart, String docText, int start, int end, String leadingWhitespace) {
		super(nameStart, null, start, end, docText);
		this.leadingWhitespace = leadingWhitespace;
		parseCells(calc, docText, start, end);
	}
	public void parseCells(OberCalc calc, String docText, int start, int end) {
		String cellText = docText.substring(start, end);
		Matcher cellMatcher = OberCalc.CELL_PAT.matcher(cellText);
		int offset = start;
		
		start = 0;
		while (cellMatcher.find(start)) {
			String name = null;
			String value = null;
			
			if (cellMatcher.start(1) != -1) {
				name = cellText.substring(cellMatcher.start(1), cellMatcher.end(1)).trim();
			}
			if (cellMatcher.start(2) != -1) {
				value = cellText.substring(cellMatcher.start(2), cellMatcher.end(2));
			}
			cells.add(new OberCalcCell(calc, offset + cellMatcher.start(1), name, value, docText, offset + cellMatcher.start() + 1, offset + cellMatcher.end()));
			start = cellMatcher.end();
		}
	}
	public Object makeJsRow(OberCalc calc) {
		NativeArray array = (NativeArray) calc.context.newArray(calc.scope, cells.size());
		
		array.setPrototype(calc.rowPrototype);
		array.put("realRow", array, this);
		for (int i = 0; i < cells.size(); i++) {
			array.put(i, array, ((OberCalcCell)cells.get(i)).value);
		}
		return array;
	}
	public void appendTo(StringBuffer buf, int level) {
		OberCalc.appendLevel(buf, level);
		buf.append("Row (");
		for (int i = 0; i < cells.size(); i++) {
			OberCalcCell cell = (OberCalcCell) cells.get(i);

			if (i > 0) {
				buf.append(", ");
			}
			cell.appendTo(buf, 0);
		}
		buf.append(")");
	}
	public Object formatValue(int i, Object value) {
		if (i < cells.size()) {
			OberCalcCell cell = (OberCalcCell) cells.get(i);
			if (cell != null) {
				return cell.format(value.toString());
			}
		}
		return value.toString();
	}
}
