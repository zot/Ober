package ar.ober.swt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;

public class OberSwtDocument implements StyledTextContent {
	protected boolean trackingChanges = false;
	protected boolean dirty = false;
	protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	protected StyledTextContent actualContent;
	
	public OberSwtDocument(StyledTextContent content) {
		actualContent = content;
	}
	public boolean isDirty()  {
		return dirty;
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
	public boolean isTrackingChanges() {
		return trackingChanges;
	}
	public void setTrackingChanges(boolean value) {
		trackingChanges = value;
	}
	public void addPropertyChangeListener(PropertyChangeListener l)  {
		propertyChangeSupport.addPropertyChangeListener(l);
	}
	public void removePropertyChangeListener(PropertyChangeListener l)  {
		propertyChangeSupport.removePropertyChangeListener(l);
	}
	public void addTextChangeListener(TextChangeListener listener) {
		actualContent.addTextChangeListener(listener);
	}
	public boolean equals(Object obj) {
		return actualContent.equals(obj);
	}
	public int getCharCount() {
		return actualContent.getCharCount();
	}
	public String getLine(int lineIndex) {
		return actualContent.getLine(lineIndex);
	}
	public int getLineAtOffset(int offset) {
		return actualContent.getLineAtOffset(offset);
	}
	public int getLineCount() {
		return actualContent.getLineCount();
	}
	public String getLineDelimiter() {
		return actualContent.getLineDelimiter();
	}
	public int getOffsetAtLine(int lineIndex) {
		return actualContent.getOffsetAtLine(lineIndex);
	}
	public String getTextRange(int start, int length) {
		return actualContent.getTextRange(start, length);
	}
	public int hashCode() {
		return actualContent.hashCode();
	}
	public void removeTextChangeListener(TextChangeListener listener) {
		actualContent.removeTextChangeListener(listener);
	}
	public void replaceTextRange(int start, int replaceLength, String text) {
		actualContent.replaceTextRange(start, replaceLength, text);
		markDirty();
	}
	public void setText(String text) {
		actualContent.setText(text);
		markDirty();
	}
	public String toString() {
		return actualContent.toString();
	}
}
