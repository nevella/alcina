package cc.alcina.template.entityaccess;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.JAXBException;

import org.hibernate.annotations.Type;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.entityaccess.WrappedObject;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.DomainBaseVersionable;

@Entity
@Table(name = "wrappedObject")
@SequenceGenerator(allocationSize=1,name = "wrappedObject_sequence", sequenceName = "wrappedObject_id_seq")
@SuppressWarnings("unchecked")
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = WrappedObject.class)
public class WrappedObjectImpl<T extends WrapperPersistable> extends
		DomainBaseVersionable implements PropertyChangeListener,
		WrappedObject<T>,HasOwner {

	private AlcinaTemplateUser user;

	@Transient
	public T getObject() {
		return getObject(getClass().getClassLoader()
				);
	}
@Transient
	public T getObject(ClassLoader classLoader) {
		if (object == null) {
			try {
				Class clazz = classLoader.loadClass(className);
				object = (T) WrappedObjectHelper.xmlDeserialize(clazz,
						getSerializedXml());
				object.setId(getId());
				object.setLocalId(getLocalId());
				object.setOwner(getUser());
				object.removePropertyChangeListener(this);
				object.addPropertyChangeListener(this);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return object;
	}

	public void setObject(T object) {
		if (this.object != null) {
			object.removePropertyChangeListener(this);
		}
		this.object = object;
		if (this.object != null) {
			object.addPropertyChangeListener(this);
		}
		xserialize();
	}

	void xserialize() {
		try {
			object.setId(getId());
			object.setLocalId(getLocalId());
			setSerializedXml(WrappedObjectHelper.xmlSerialize(object));
			setClassName(object.getClass().getName());
		} catch (JAXBException e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private long id;

	private String className;

	private String serializedXml;

	private String key;

	private T object;

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getClassName() {
		return className;
	}

	@Id
	@GeneratedValue(generator = "wrappedObject_sequence")
	public long getId() {
		return this.id;
	}

	/**
	 * Getter of the property <tt>serializedXml</tt>
	 * 
	 * @return Returns the serializedXml.
	 */
	@Lob
	@Type(type="org.hibernate.type.StringClobType")
	public String getSerializedXml() {
		return serializedXml;
	}

	/**
	 * Setter of the property <tt>className</tt>
	 * 
	 * @param className
	 *            The className to set.
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Setter of the property <tt>serializedXml</tt>
	 * 
	 * @param serializedXml
	 *            The serializedXml to set.
	 */
	public void setSerializedXml(String serializedXml) {
		this.serializedXml = serializedXml;
	}

	public void setUser(IUser user) {
		this.user = (AlcinaTemplateUser) user;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY, targetEntity = AlcinaTemplateUser.class)
	public IUser getUser() {
		return user;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		xserialize();
	}
	@Transient
	public IUser getOwner() {
		return getUser();
	}
}
