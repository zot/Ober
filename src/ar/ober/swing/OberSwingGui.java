package ar.ober.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;

import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import ar.ober.Ober;
import ar.ober.OberGui;
import ar.ober.OberViewer;

public class OberSwingGui extends OberGui {
	public static PropertyChangeListener focusListener;
	public static final int MODIFIERS[] = new int[32];
	public static final HashMap eventTable = new HashMap();
	public static final HashMap awtEventTable = new HashMap();
	public static EvtKey tmpkey = new EvtKey();
	
	public static final StyleContext STYLE_CONTEXT = new StyleContext();
	
	public static class EvtKey  {
		int modifiers;
		int key;
		
		public int hashCode()  {
			return modifiers ^ key;
		}
		public boolean equals(Object obj)  {
			return obj instanceof EvtKey && modifiers == ((EvtKey)obj).modifiers && key == ((EvtKey)obj).key;
		}
	}
	
	static  {
		MODIFIERS[Ober.KMASK_CTRL] = KeyEvent.CTRL_DOWN_MASK;
		MODIFIERS[Ober.KMASK_SHIFT] = KeyEvent.SHIFT_DOWN_MASK;
	}

	public void dispatch() {}
	public void dying() {}
	public OberViewer createViewer()  {
		return new OberSwingViewer();
	}
	public String eventString(Object e)  {
		return awtEventString(((KeyEvent)e).getModifiersEx(), ((KeyEvent)e).getKeyCode());
	}
	public String eventString(int modifiersEx, int keyCode) {
		int mods = 0;
		int testBit = 1;
		Object str;

		tmpkey.modifiers = modifiersEx;
		tmpkey.key = keyCode;
		str = eventTable.get(tmpkey);
		if (str != null) {
			return (String)str;
		}
		while (modifiersEx != 0)  {
			if ((modifiersEx & testBit) != 0)  {
				mods |= MODIFIERS[testBit];
			}
			modifiersEx &= ~testBit;
			testBit <<= 1;
		}
		switch (keyCode) {
			case Ober.KEY_ENTER:
				keyCode = KeyEvent.VK_ENTER;
		}
		String result = awtEventString(mods, keyCode);
		eventTable.put(tmpkey, result);
		tmpkey = new EvtKey();
		return result;
	}
	public String awtEventString(int modifiersEx, int keyCode) {
		Object str;

		tmpkey.modifiers = modifiersEx;
		tmpkey.key = keyCode;
		str = awtEventTable.get(tmpkey);
		if (str != null) {
			return (String)str;
		}
		StringBuffer evt = new StringBuffer(KeyEvent.getModifiersExText(modifiersEx));
		if (evt.length() > 0) {
			evt.append(",");
		}
		evt.append(KeyEvent.getKeyText(keyCode));
		awtEventTable.put(tmpkey, evt.toString());
		tmpkey = new EvtKey();
		return evt.toString();
	}

	public static void uninstall() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(focusListener);
	}
	public void install() {
		OberViewer.PLAIN = STYLE_CONTEXT.addStyle("PLAIN", null);
		OberViewer.BOLD = STYLE_CONTEXT.addStyle("BOLD", null);
		OberViewer.BOLD_RED = STYLE_CONTEXT.addStyle("BOLD_RED", (Style)OberViewer.BOLD);
		OberViewer.CALC_VARIABLE = STYLE_CONTEXT.addStyle("CALC_VAR", null);
		OberViewer.CALC_NEW_VALUE = STYLE_CONTEXT.addStyle("CALC_NEW", null);
		OberViewer.CALC_OLD_VALUE = STYLE_CONTEXT.addStyle("CALC_OLD", null);
		StyleConstants.setBold((Style)OberViewer.BOLD, true);
		StyleConstants.setForeground((Style)OberViewer.BOLD_RED, new Color(Ober.RED));StyleConstants.setForeground((Style)OberViewer.BOLD_RED, new Color(Ober.RED));
		StyleConstants.setForeground((Style)OberViewer.CALC_VARIABLE, Color.MAGENTA);
		StyleConstants.setForeground((Style)OberViewer.CALC_NEW_VALUE, Color.RED);
		StyleConstants.setForeground((Style)OberViewer.CALC_OLD_VALUE, Color.BLUE);
		StyleConstants.setForeground((Style)OberViewer.PLAIN, Color.BLACK);
		focusListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				for (int i = 0; i < Ober.current.viewers.size(); i++) {
					((OberSwingViewer)Ober.current.viewers.get(i)).newFocus((Component)evt.getNewValue());
				}
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener);
	}
}
