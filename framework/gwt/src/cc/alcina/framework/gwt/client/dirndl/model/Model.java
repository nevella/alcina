package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Arrays;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.Bean;

/**
 * Thoughts on binding :: particularly in the case of UI bindings, a.b->c.d is
 * _sometimes_ better handled via "listen to major updates on a" - this
 * simplifies the handling of "a changed" vs "a.b changed".
 * 
 * This is the motivation for the fireUpdated() method, see particularly
 * DirectedSingleEntityActivity
 * 
 * @author nick@alcina.cc
 *
 */
@Bean
public abstract class Model extends Bindable {
	public static final transient Object MODEL_UPDATED = new Object();

	public void fireUpdated() {
		fireUnspecifiedPropertyChange(MODEL_UPDATED);
	}

	public void unbind() {
		Arrays.stream(propertyChangeSupport().getPropertyChangeListeners())
				.filter(pcl -> pcl instanceof RemovablePropertyChangeListener)
				.forEach(pcl -> ((RemovablePropertyChangeListener) pcl)
						.unbind());
	}
}
