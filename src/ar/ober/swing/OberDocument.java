/*
 * Created on Mar 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ar.ober.swing;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultStyledDocument;

public class OberDocument extends DefaultStyledDocument implements DocumentListener {
	protected boolean trackingChanges = false;
	protected boolean dirty = false;
	protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	public static OberDocument copy(final OberDocument doc) throws IOException, ClassNotFoundException {
		final PipedOutputStream pipeOut = new PipedOutputStream();
		final PipedInputStream pipeIn = new PipedInputStream(pipeOut);

		new Thread(new Runnable() {
			public void run() {
				try {
					ObjectOutputStream output = new ObjectOutputStream(pipeOut);
					output.writeObject(doc);
					output.flush();
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		ObjectInputStream input = new ObjectInputStream(pipeIn);
		return (OberDocument)input.readObject();
	}
	
	public OberDocument() {
		addDocumentListener(this);
	}
	public boolean isTrackingChanges() {
		return trackingChanges;
	}
	public void setTrackingChanges(boolean value) {
		trackingChanges = value;
	}
	public void addPropertyChangeListener(PropertyChangeListener l) {
		propertyChangeSupport.addPropertyChangeListener(l);
	}
	public void removePropertyChangeListener(PropertyChangeListener l) {
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