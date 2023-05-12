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

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.beans.annotations.Omit;

import cc.alcina.framework.common.client.logic.LazyPropertyChangeSupport;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

/**
 *
 * @author Nick Reddel
 */
public class BaseSourcesPropertyChangeEvents
		implements SourcesPropertyChangeEvents {
	private transient LazyPropertyChangeSupport propertyChangeSupport;

	public void addOrRemovePropertyChangeListener(
			PropertyChangeListener listener, boolean add) {
		if (add) {
			this.propertyChangeSupport().addPropertyChangeListener(listener);
		} else {
			this.propertyChangeSupport().removePropertyChangeListener(listener);
		}
	}

	public void addOrRemovePropertyChangeListener(String propertyName,
			PropertyChangeListener listener, boolean add) {
		if (add) {
			addPropertyChangeListener(propertyName, listener);
		} else {
			removePropertyChangeListener(propertyName, listener);
		}
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport().addPropertyChangeListener(listener);
	}

	public synchronized void addPropertyChangeListener(
			PropertyEnum propertyName, PropertyChangeListener listener) {
		this.propertyChangeSupport().addPropertyChangeListener(propertyName,
				listener);
	}

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport().addPropertyChangeListener(propertyName,
				listener);
	}

	/*
	 * MVCC access - 'this' correctly refers to the version, *not*
	 * domainIdentity()
	 */
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public synchronized ListenerReference addTypedPropertyChangeListener(
			PropertyEnum propertyName, TopicListener typedListener) {
		RemovablePropertyChangeListener.Typed changeListener = new RemovablePropertyChangeListener.Typed(
				this, propertyName, typedListener);
		this.propertyChangeSupport().addPropertyChangeListener(propertyName,
				changeListener);
		return changeListener;
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

	@Override
	@Transient
	@XmlTransient
	@JsonIgnore
	@Omit
	public PropertyChangeListener[] propertyChangeListeners() {
		return this.propertyChangeSupport().getPropertyChangeListeners();
	}

	/*
	 * Given all the ways the transient deserialization mix can go wrong, this
	 * is best.
	 *
	 * I'd imagine any decent JS engine will optimise the null check fairly well
	 *
	 * MVCC access - 'this' correctly refers to the version, *not*
	 * domainIdentity()
	 */
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public LazyPropertyChangeSupport propertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new LazyPropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport().removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport().removePropertyChangeListener(propertyName,
				listener);
	}

	// optimisation for non-mutating callers
	protected boolean hasPropertyChangeSupport() {
		return propertyChangeSupport != null;
	}

	protected <V> void set(PropertyEnum propertyName, V oldValue, V newValue,
			Runnable setter) {
		set(propertyName.name(), oldValue, newValue, setter);
	}

	protected <V> void set(String propertyName, V oldValue, V newValue,
			Runnable setter) {
		if (oldValue == newValue) {
			return;
		}
		setter.run();
		if (propertyChangeSupport == null) {
			return;
		}
		propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}
}
