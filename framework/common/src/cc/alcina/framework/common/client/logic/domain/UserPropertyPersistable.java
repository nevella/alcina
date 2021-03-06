package cc.alcina.framework.common.client.logic.domain;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;

public interface UserPropertyPersistable
		extends Serializable, SourcesPropertyChangeEvents, TreeSerializable {
	@AlcinaTransient
	/*
	 * repeat the annotation in the implementation method
	 */
	public UserPropertyPersistable.Support getUserPropertySupport();

	public void setUserPropertySupport(UserPropertyPersistable.Support support);

	public static class Support
			implements Serializable, PropertyChangeListener {
		private UserPropertyPersistable persistable;

		private UserProperty property;

		/*
		 * For serialization
		 */
		public Support() {
		}

		public Support(UserProperty property) {
			this.property = property;
		}

		public void ensureListeners() {
			persistable.setUserPropertySupport(this);
			persistable.removePropertyChangeListener(this);
			persistable.addPropertyChangeListener(this);
		}

		public synchronized UserPropertyPersistable getPersistable() {
			if (persistable == null) {
				persistable = (UserPropertyPersistable) property.deserialize();
				ensureListeners();
			}
			return this.persistable;
		}

		public UserProperty getProperty() {
			return this.property;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			property.serializeObject(persistable);
		}

		public void setPersistable(UserPropertyPersistable persistable) {
			Preconditions.checkState(this.persistable == null);
			this.persistable = persistable;
			property.serializeObject(persistable);
			ensureListeners();
		}
	}
}
