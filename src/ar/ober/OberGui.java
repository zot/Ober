package ar.ober;

public abstract class OberGui {
	protected Ober ober;

	public abstract OberViewer createViewer();
	public abstract void install();
	public abstract void dispatch();
	public abstract void dying();
	public abstract String eventString(Object event);
	public abstract String eventString(int modifiers, int keycode);

	public void setOber(Ober o) {
		ober = o;
	}
}
