package OberPlugin.views;

import java.io.IOException;

import ognl.Node;
import ognl.Ognl;
import ognl.OgnlException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.part.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;

import OberPlugin.OberPlugin;
import ar.ober.Ober;
import ar.ober.OberCommand;
import ar.ober.OberContext;
import ar.ober.OberViewer;
import ar.ober.swt.OberSwtGui;
import ar.ober.swt.OberSwtViewer;

/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class OberView extends ViewPart {
	private OberSwtViewer viewer;

	public static MenuManager oberMenu;

	public static void initializeOber(OberSwtViewer v) throws IOException {
		IPath path = OberPlugin.getDefault().getStateLocation().append("oberrc");
		oberMenu = new MenuManager("Ober", "Ober");
		((WorkbenchWindow)OberPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()).getMenuManager().add(oberMenu);
		OberPlugin.getDefault().ober.addCommand("System.AddMenu", new OberCommand(" <item> <ognl command> -- add an item to eclipse's Ober menu") {
			public void execute(final OberContext ctx) throws Exception {
				final String name = ctx.getArgumentString(1);
				final Object expr = ctx.getArgument(2);

				if (expr instanceof Node) {
					final Node node = (Node)expr;
					Action action = new Action(name) {
						public void run() {
							OberView v = ((OberView)OberPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("Ober"));
		
							try {
								Ognl.getValue(node, v);
							} catch (OgnlException e) {
								if (v != null) {
									e.printStackTrace();
								}
							}
						}
					};

					oberMenu.add(action);
				} else {
					System.err.println("Error.  arg0: " + ctx.getArgumentString(0) + ctx.getArgumentString(1) + ctx.getArgumentString(2));
				}
			}
		});
		if (path.toFile().exists()) {
			OberPlugin.getDefault().ober.loadFile(OberPlugin.getDefault().openStream(path), v);
		} else {
			System.out.println("No 'oberrc' file found in the ober plugin .metadata directory.");
		}
	}

	public static OberSwtViewer createView(Composite parent) throws IOException {
		OberSwtViewer v = new OberSwtViewer();
		boolean initialized = false;

		synchronized (OberPlugin.class) {
			initialized = OberPlugin.getDefault().ober != null;
			if (!initialized) {
				OberPlugin.getDefault().ober = new Ober(new OberSwtGui());
			}
		}
		v.ober = OberPlugin.getDefault().ober;
		v.type = OberViewer.MAIN_TYPE;
		v.createGui(parent, -1);
		v.setTagText("Ober: Newcol, New, Help");
		if (!initialized) {
			initializeOber(v);
		}
		return v;
	}

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return new String[] { "One", "Two", "Three" };
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	/**
	 * The constructor.
	 */
	public OberView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		try {
			viewer = createView(parent);
			viewer.ober.help(viewer);
		} catch (Exception e) {
			if (viewer != null) {
				viewer.error(e);
			} else {
				e.printStackTrace();
			}
		}
		/*
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(ResourcesPlugin.getWorkspace());
		*/
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.wrapper.getShell(),
			"Ober",
			message);
	}
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.setFocus();
	}
}