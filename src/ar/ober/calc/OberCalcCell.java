// Created on Nov 22, 2003 by Bill Burdick
package ar.ober.calc;

public class OberCalcCell extends OberCalcVariable {
	public String value;
	public boolean leftAligned;
	public int rightPad;
	
	public final static int LEFT = 0;
	public final static int RIGHT = 1;
	public final static int CENTER = 2;

	public OberCalcCell(OberCalc calc, int nameStart, String name, String value, String docText, int start, int end) {
		super(nameStart, name, start, end, docText);
		this.value = value;
		if (this.name != null) {
			calc.putVar(this, this.value);
		}
		if (!Character.isSpaceChar(docText.charAt(start))) {
			leftAligned = true;
		} else {
			int pos = end;

			leftAligned = false;
			while (pos-- > start && Character.isSpaceChar(docText.charAt(pos))) {}
			rightPad = end - pos - 1;
		}
	}
	public void appendTo(StringBuffer buf, int level) {
		OberCalc.appendLevel(buf, level);
		buf.append("Cell (");
		if (name != null) {
			buf.append(name);
			buf.append(" = ");
		}
		buf.append(value);
		buf.append(")");
	}
	public void pad(StringBuffer buf, char c, int count) {
		while (count-- > 0) {
			buf.append(c);
		}
	}
	public String format(String value) {
		if (name != null) {
			value = name + " = " + value;
		}
		if (value.length() < length) {
			StringBuffer buf = new StringBuffer();
			if (leftAligned) {
					buf.append(value);
					pad(buf, ' ', length - value.length());
			} else {
					pad(buf, ' ', length - value.length() - rightPad);
					buf.append(value);
					pad(buf, ' ', Math.min(rightPad, length - value.length()));
			}
			value = buf.toString();
		}
		return value;
	}
	public String toString() {
		return value;
	}
}
