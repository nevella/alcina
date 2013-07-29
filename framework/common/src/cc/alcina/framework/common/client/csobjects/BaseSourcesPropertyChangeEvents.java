/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.csobjects;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 * 
 * @author Nick Reddel
 */
public class BaseSourcesPropertyChangeEvents implements
		SourcesPropertyChangeEvents {
	private transient MutablePropertyChangeSupport propertyChangeSupport;

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport().addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport().addPropertyChangeListener(propertyName,
				listener);
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		this.propertyChangeSupport().firePropertyChange(evt);
	}

	public void firePropertyChange(String propertyName, boolean oldValue,
			boolean newValue) {
		this.propertyChangeSupport().firePropertyChange(propertyName, oldValue,
				newValue);
	}

	public void firePropertyChange(String propertyName, int oldValue,
			int newValue) {
		this.propertyChangeSupport().firePropertyChange(propertyName, oldValue,
				newValue);
	}

	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		this.propertyChangeSupport().firePropertyChange(propertyName, oldValue,
				newValue);
	}

	@Transient
	@XmlTransient
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return this.propertyChangeSupport().getPropertyChangeListeners();
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport().removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport().removePropertyChangeListener(propertyName,
				listener);
	}

	/*
	 * Given all the ways the transient deserialization mix can go wrong, this
	 * is best.
	 * 
	 * I'd imagine any decent JS engine will optimise the null check fairly well
	 */
	protected MutablePropertyChangeSupport propertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new MutablePropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}
}
