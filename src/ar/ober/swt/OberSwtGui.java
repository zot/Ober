package ar.ober.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import ar.ober.OberGui;
import ar.ober.OberViewer;
import ar.ober.Ober;

public class OberSwtGui extends OberGui {
	boolean dead = false;
	Display display;

	public void dying() {
		dead = true;
	}
	public OberViewer createViewer() {
		return new OberSwtViewer();
	}
	public String eventString(Object event) {
		return swtEventString(((Event)event).stateMask, ((Event)event).character);
	}
	public String swtEventString(int modifiers, int keycode) {
		StringBuffer buf = new StringBuffer();

		switch (keycode) {
			case '\r':
			case '\b':
				break;
			default:
				if (Character.isISOControl((char)keycode)) {
					modifiers |= SWT.CONTROL;
					keycode += 'A' - 1;
				}
		}
		for (int i = 1; (modifiers & SWT.MODIFIER_MASK) != 0; i <<= 1) {
			if ((i & modifiers) != 0) {
				switch (i) {
					case SWT.ALT:
						buf.append("ALT,");
						break;
					case SWT.SHIFT:
						buf.append("SHIFT,");
						break;
					case SWT.CONTROL:
						buf.append("CONTROL,");
						break;
					case SWT.COMMAND:
						buf.append("COMMAND,");
						break;
				}
				modifiers &= ~i;
			}
		}
		switch (keycode) {
			case '\r':
				buf.append("ENTER");
				break;
			case '\b':
				buf.append("BS");
				break;
			default:
				buf.append((char)keycode);
				break;
		}
		return buf.toString();
	}
	public String eventString(int modifiers, int keycode) {
		int mods = 0;

		for (int i = 1; modifiers != 0; i <<= 1) {
			if ((i & modifiers) != 0) {
				switch (i) {
					case Ober.KMASK_CTRL:
						mods |= SWT.CONTROL;
						break;
					case Ober.KMASK_SHIFT:
						mods |= SWT.SHIFT;
						break;
				}
				modifiers &= ~i;
			}
		}
		switch (keycode) {
			case Ober.KEY_ENTER:
				keycode = '\r';
				break;
		}
		return swtEventString(mods, keycode);
	}
	public void install() {
		display = Display.getDefault();
		OberViewer.BOLD = new StyleRange(0, 0, Display.getDefault().getSystemColor(SWT.COLOR_BLACK), Display.getDefault().getSystemColor(SWT.COLOR_WHITE), SWT.BOLD);
		OberViewer.BOLD_RED = new StyleRange(0, 0, Display.getDefault().getSystemColor(SWT.COLOR_RED), Display.getDefault().getSystemColor(SWT.COLOR_WHITE), SWT.BOLD);
		OberViewer.CALC_VARIABLE = new StyleRange(0, 0, Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA), Display.getDefault().getSystemColor(SWT.COLOR_WHITE), SWT.NORMAL);
		OberViewer.CALC_NEW_VALUE = new StyleRange(0, 0, Display.getDefault().getSystemColor(SWT.COLOR_RED), Display.getDefault().getSystemColor(SWT.COLOR_WHITE), SWT.NORMAL);
		OberViewer.CALC_OLD_VALUE = new StyleRange(0, 0, Display.getDefault().getSystemColor(SWT.COLOR_BLUE), Display.getDefault().getSystemColor(SWT.COLOR_WHITE), SWT.NORMAL);
		OberViewer.PLAIN = new StyleRange(0, 0, Display.getDefault().getSystemColor(SWT.COLOR_BLACK), Display.getDefault().getSystemColor(SWT.COLOR_WHITE), SWT.NORMAL);
		synchronized (this) {
			notify();
		}
	}
	public void dispatch() {
		while (!dead)  {
			if (!display.readAndDispatch())  {
				display.sleep();
			}
		}
		display.dispose();
	}
}
