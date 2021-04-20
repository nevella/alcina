package cc.alcina.framework.common.client.logic.domain;

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

	public static class Support implements Serializable {
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

		public synchronized UserPropertyPersistable getPersistable() {
			if (persistable == null) {
				persistable = property.deserialize();
				setupListener();
			}
			return this.persistable;
		}

		public UserProperty getProperty() {
			return this.property;
		}

		public void setPersistable(UserPropertyPersistable persistable) {
			Preconditions.checkState(this.persistable == null);
			this.persistable = persistable;
			property.serializeObject(persistable);
			setupListener();
		}

		private void setupListener() {
			persistable.setUserPropertySupport(this);
			persistable.addPropertyChangeListener(
					evt -> property.serializeObject(persistable));
		}
	}
}
