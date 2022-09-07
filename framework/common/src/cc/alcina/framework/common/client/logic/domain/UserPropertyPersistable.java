package cc.alcina.framework.common.client.logic.domain;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.serializer.Serializers;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

public interface UserPropertyPersistable
		extends Serializable, SourcesPropertyChangeEvents, TreeSerializable {
	/*
	 * Serialize when sending to client, otherwise not
	 *
	 * Must currently be copied to implementors - FIXME - reflection
	 */
	@AlcinaTransient(unless = AlcinaTransient.TransienceContext.CLIENT)
	public UserPropertyPersistable.Support getUserPropertySupport();

	public void setUserPropertySupport(UserPropertyPersistable.Support support);

	@Bean
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
			if (persistable == null && !Serializers.isSerializing()) {
				// properties may be simple strings, not objects, in which case
				// return null
				try {
					persistable = (UserPropertyPersistable) property
							.deserialize();
					if (persistable != null) {
						ensureListeners();
					} else {
						return null;
					}
				} catch (Exception e) {
					// FIXME - mvcc.5 - devex
					e.printStackTrace();
					return null;
				}
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
			if (!Serializers.isDeserializing()) {
				property.serializeObject(persistable);
			}
			ensureListeners();
		}

		public void setProperty(UserProperty property) {
			this.property = property;
		}
	}
}
