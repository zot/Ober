// Created on Nov 22, 2003 by Bill Burdick
package ar.ober.calc;

import java.util.ArrayList;
import java.util.regex.Matcher;

import org.mozilla.javascript.NativeArray;

// Each table has a name.
// Table cells are delimited by vertical bars.
// Single-cell tables render as a simple value (no vertical bars).
public class OberCalcTable extends OberCalcVariable {
	//	offset of name from end of previous element (or start of document for first table)
	public ArrayList rows = new ArrayList();

	public static Object parse(OberCalc calc, int nameStart, String name, String text, int start, int end) {
		OberCalcTable table = new OberCalcTable(calc, nameStart, name, text, start, end);
		
		if (!table.rows.isEmpty()) {
			if (table.rows.size() == 1) {
				OberCalcRow row = (OberCalcRow) table.rows.get(0);

				if (row.cells.size() == 1) {
					OberCalcCell cell = (OberCalcCell) row.cells.get(0);

					cell.name = table.name;
					cell.nameStart = table.nameStart;
					calc.replaceVar(table, cell, cell.value);
					cell.start = table.nameStart;
					cell.length = table.length + table.start - table.nameStart;
					return cell;
				}
				row.name = table.name;
				row.nameStart = table.nameStart;
				calc.replaceVar(table, row, row.makeJsRow(calc));
				return row;
			}
			return table;
		}
		return null;
	}
	
	public OberCalcTable(OberCalc calc, int nameStart, String name, String text, int start, int end) {
		super(nameStart, name, start, end, text);
		if (name != null) {
			calc.putVar(this, this);
		}
		parseRows(calc, text);
		if (rows.isEmpty()) {
			calc.removeVar(this);
		} else {
			calc.setVar(this, makeJsTable(calc));
		}
	}
	public Object makeJsTable(OberCalc calc) {
		NativeArray array = (NativeArray) calc.context.newArray(calc.scope, rows.size());
		
		array.setPrototype(calc.tablePrototype);
		for (int i = 0; i < rows.size(); i++) {
			array.put(i, array, ((OberCalcRow)rows.get(i)).makeJsRow(calc));
		}
		return array;
	}
	public void parseRows(OberCalc calc, String docText) {
		Matcher rowMatcher = OberCalc.ROW_PAT.matcher(source);
		int rowStart = 0;
		
		while (rowMatcher.find(rowStart)) {
			rows.add(new OberCalcRow(calc, 0, docText, start + rowMatcher.start(1), start + rowMatcher.end(), source.substring(rowStart, rowMatcher.start(1))));
			rowStart = rowMatcher.end();
		}
	}
	public void appendTo(StringBuffer buf, int level) {
		OberCalc.appendLevel(buf, level);
		buf.append("Table ");
		buf.append(name);
		for (int i = 0; i < rows.size(); i++) {
			OberCalcRow row = (OberCalcRow) rows.get(i);

			buf.append("\n");
			row.appendTo(buf, level + 1);
		}
	}
}
