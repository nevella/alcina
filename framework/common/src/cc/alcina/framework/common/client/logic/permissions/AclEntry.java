package cc.alcina.framework.common.client.logic.permissions;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.gwt.client.entity.EntityAction;

@MappedSuperclass
/**
 *
 * <p>
 * Simple entity permissions should use the {@link #entityAction} field, more
 * complex the (probably subclassed) Data object
 *
 * <p>
 * Rules are resolved in priority (high to low) then reverse creation order, the
 * first match 'winning'
 *
 * @author nick@alcina.cc
 *
 */
public abstract class AclEntry extends VersionableEntity<AclEntry> {
	private EntityAction entityAction;

	private Data data;

	private String dataSerialized;

	private boolean negate;

	private int priority;

	@Transient
	public abstract Acl getAcl();

	@Transient
	@DomainProperty(serialize = true)
	@AlcinaTransient
	public Data getData() {
		data = TransformManager.resolveMaybeDeserialize(data,
				this.dataSerialized, null);
		return this.data;
	}

	@Lob
	@Transient
	public String getDataSerialized() {
		return this.dataSerialized;
	}

	public EntityAction getEntityAction() {
		return this.entityAction;
	}

	public int getPriority() {
		return this.priority;
	}

	public boolean isNegate() {
		return this.negate;
	}

	/*
	 * Returns the user or group associated with the entry (see
	 * java.security.Subject)
	 */
	public abstract AclSubject provideSubject();

	public void setData(Data data) {
		Object old_data = this.data;
		this.data = data;
		propertyChangeSupport().firePropertyChange("data", old_data, data);
	}

	public void setDataSerialized(String dataSerialized) {
		String old_dataSerialized = this.dataSerialized;
		this.dataSerialized = dataSerialized;
		propertyChangeSupport().firePropertyChange("dataSerialized",
				old_dataSerialized, dataSerialized);
	}

	public void setEntityAction(EntityAction entityAction) {
		var old_entityAction = this.entityAction;
		this.entityAction = entityAction;
		propertyChangeSupport().firePropertyChange("entityAction",
				old_entityAction, entityAction);
	}

	public void setNegate(boolean negate) {
		boolean old_negate = this.negate;
		this.negate = negate;
		propertyChangeSupport().firePropertyChange("negate", old_negate,
				negate);
	}

	public void setPriority(int priority) {
		int old_priority = this.priority;
		this.priority = priority;
		propertyChangeSupport().firePropertyChange("priority", old_priority,
				priority);
	}

	/*
	 * Non-relational, possibly structured data associated with the persistent
	 * object
	 */
	public static class Data extends Bindable {
		private CustomEntityAction customEntityAction;

		public CustomEntityAction getCustomEntityAction() {
			return this.customEntityAction;
		}

		public void
				setCustomEntityAction(CustomEntityAction customEntityAction) {
			this.customEntityAction = customEntityAction;
		}
	}
}
