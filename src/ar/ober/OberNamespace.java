package ar.ober;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class OberNamespace implements Comparable {
	protected OberNamespace parents[];
	protected HashMap bindings = new HashMap();
	protected HashMap commands = new HashMap();
	protected OberCommand defaultCommand = null;
	protected OberCommand defaultKeyBinding = null;
	protected String name;
	
	public OberNamespace(String nsname, OberNamespace nsparents[]) {
		parents = nsparents;
		name = nsname;
	}
	public void addInheritence(ArrayList inh) {
		for (int i = parents.length; i-- > 0;) {
			parents[i].addInheritence(inh);
		}
		if (!inh.contains(this)) {
			inh.add(this);
		}
	}
	public boolean handleKey(String evt, OberContext ctx) throws Exception {
		ArrayList inh = new ArrayList();
		
		addInheritence(inh);
		return handleKeyInPath(evt, ctx, inh, inh.size() - 1);
	}
	public boolean handleKeyInPath(String evt, OberContext ctx, ArrayList inh, int index) throws Exception {
		if (bindings.get(evt) != null) {
			((OberCommand)bindings.get(evt)).execute(ctx);
		} else if (index > 0) {
			((OberNamespace)inh.get(index - 1)).handleKeyInPath(evt, ctx, inh, index - 1);
		} else {
			return defaultHandleKey(evt, ctx, inh, inh.size() - 1);
		}
		return true;
	}
	public boolean defaultHandleKey(String evt, OberContext ctx, ArrayList inh, int index) throws Exception {
		if (defaultKeyBinding != null)  {
			defaultKeyBinding.execute(ctx);
			return true;
		} else if (index > 0) {
			return ((OberNamespace)inh.get(index - 1)).defaultHandleKey(evt, ctx, inh, index - 1);
		} else {
			return false;
		}
	}
	public void executeCommand(String string, OberContext ctx) throws Exception {
		ArrayList inh = new ArrayList();
		
		addInheritence(inh);
		executeCommandInPath(string, ctx, inh, inh.size() - 1);
	}
	public void executeCommandInPath(String commandName, OberContext ctx, ArrayList inh, int index) throws Exception {
		if (commands.get(commandName) != null) {
			((OberCommand)commands.get(commandName)).execute(ctx);
		} else if (index > 0) {
			((OberNamespace)inh.get(index - 1)).executeCommandInPath(commandName, ctx, inh, index - 1);
		} else {
			executeDefaultCommand(commandName, ctx, inh, inh.size() - 1);
		}
	}
	public void executeDefaultCommand(String cmd, OberContext ctx, ArrayList inh, int index) throws Exception {
		if (defaultCommand != null)  {
			defaultCommand.execute(ctx);
		} else if (index > 0) {
			((OberNamespace)inh.get(index)).executeDefaultCommand(cmd, ctx, inh, index - 1);
		} else {
			throw new Exception("Ober command not found: " + cmd);
		}
	}
	public void executeFullyqualifiedCommand(String namespaceName, String commandName, OberContext ctx) throws Exception {
		if (commands.get(commandName) != null) {
			((OberCommand)commands.get(commandName)).execute(ctx);
		} else {
			throw new Exception("Ober command not found: " + namespaceName + "." + commandName);
		}
	}
	public void help(StringBuffer buf) {
		buf.append("\n\nNamespace: ");
		buf.append(name);
		buf.append(" parents: ");
		for (int i = 0; i < parents.length; i++) {
			if (i > 0) {
				buf.append(", ");
			}
			buf.append(parents[i].getName());
		}
		buf.append("\nCommands:");
		for (Iterator i = commands.keySet().iterator(); i.hasNext(); ) {
			String cmd = (String) i.next();

			buf.append("\n\t");
			buf.append(cmd);
			buf.append(((OberCommand)commands.get(cmd)).getDescription());
		}
		buf.append("\nKey Bindings:");
		for (Iterator i = bindings.keySet().iterator(); i.hasNext(); ) {
			String cmd = (String) i.next();

			buf.append("\n\t");
			buf.append(cmd);
			buf.append(((OberCommand)bindings.get(cmd)).getDescription());
		}
	}
	protected String getName() {
		return name;
	}
	public int compareTo(Object o) {
		ArrayList inh = new ArrayList();
		ArrayList inh2 = new ArrayList();
		
		addInheritence(inh);
		((OberNamespace)o).addInheritence(inh2);
		return (inh + " ").compareTo(inh2 + " ");
	}
	public void addCommand(String name, OberCommand cmd) {
		commands.put(name, cmd);
	}
	public void addKeybinding(String evt, OberCommand cmd) {
		bindings.put(evt, cmd);
	}
}
