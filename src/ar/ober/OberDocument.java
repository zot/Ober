/*
 * Created on Mar 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ar.ober;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultStyledDocument;


public class OberDocument extends DefaultStyledDocument implements DocumentListener {
	protected boolean trackingChanges = false;
	protected boolean dirty = false;
	protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	public OberDocument() {
		addDocumentListener(this);
	}
	public boolean isTrackingChanges() {
		return trackingChanges;
	}
	public void setTrackingChanges(boolean value) {
		trackingChanges = value;
	}
	public void addPropertyListener(PropertyChangeListener l) {
		propertyChangeSupport.addPropertyChangeListener(l);
	}
	public void removePropertyListener(PropertyChangeListener l) {
		propertyChangeSupport.removePropertyChangeListener(l);
	}
	public void markDirty() {
		if (trackingChanges && !dirty) {
			dirty = true;
			propertyChangeSupport.firePropertyChange("dirty", false, true);
		}
	}
	public void markClean() {
		if (dirty) {
			dirty = false;
			propertyChangeSupport.firePropertyChange("dirty", true, false);
		}
	}
	public boolean isDirty() {
		return dirty;
	}
	public void insertUpdate(DocumentEvent e) {
		markDirty();
	}
	public void removeUpdate(DocumentEvent e) {
		markDirty();
	}
	public void changedUpdate(DocumentEvent e) {
		markDirty();
	}
}