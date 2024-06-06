package cc.alcina.framework.common.client.logic.domain;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.serializer.Serializers;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

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

	// marker
	public interface ResetOnSerializationException {
	}

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
				/*
				 * Deserializable properties will have a legal java classname as
				 * the 'key' value
				 */
				try {
					if (Ax.blankToEmpty(property.getKey()).contains(":")) {
						// not a legal classname - e.g. a type signature record
						return null;
					}
					persistable = (UserPropertyPersistable) property
							.deserialize();
					if (persistable != null) {
						ensureListeners();
					} else {
						return null;
					}
				} catch (Exception e) {
					Ax.err("Deserializing %s :: %s :: %s", property.toLocator(),
							property.getKey(), property.getCategory());
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
			persist();
		}

		public void persist() {
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

	public abstract static class Base extends Model.Fields
			implements UserPropertyPersistable {
		private UserPropertyPersistable.Support userPropertySupport;

		@Override
		@AlcinaTransient(unless = AlcinaTransient.TransienceContext.CLIENT)
		@XmlTransient
		public UserPropertyPersistable.Support getUserPropertySupport() {
			if (this.userPropertySupport != null) {
				this.userPropertySupport.ensureListeners();
			}
			return this.userPropertySupport;
		}

		@Override
		public void setUserPropertySupport(Support userPropertySupport) {
			this.userPropertySupport = userPropertySupport;
		}
	}
}
