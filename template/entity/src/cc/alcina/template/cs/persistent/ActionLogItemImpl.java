package cc.alcina.template.cs.persistent;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;



@Entity
@Table(name = "actionlog", schema = "public")
@SequenceGenerator(allocationSize=1,name = "actionlog_sequence", sequenceName = "actionlog_id_seq")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@BeanInfo(displayNamePropertyName = "id")

@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = ActionLogItem.class)
public class ActionLogItemImpl extends DomainBaseVersionable implements ActionLogItem {
	Date actionDate;

	String actionLog;

	String actionClassName;
	
	 String shortDescription;

	public String getShortDescription() {
		return this.shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	transient Class<? extends RemoteAction> actionClass;

	long id;

	@Id
	@GeneratedValue(generator = "actionlog_sequence")
	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getActionDate() {
		return this.actionDate;
	}

	public void setActionDate(Date actionDate) {
		this.actionDate = actionDate;
	}
	@Lob
	@Type(type="org.hibernate.type.StringClobType")
	public String getActionLog() {
		return this.actionLog;
	}

	public void setActionLog(String actionLog) {
		this.actionLog = actionLog;
	}

	public String getActionClassName() {
		if (this.actionClassName == null && this.actionClass != null) {
			this.actionClassName = this.actionClass.getName();
		}
		return this.actionClassName;
	}

	public void setActionClassName(String actionClassName) {
		this.actionClassName = actionClassName;
	}

	@Transient
	@SuppressWarnings("unchecked")
	public Class<? extends RemoteAction> getActionClass() {
		if (this.actionClass == null && this.actionClassName != null) {
			this.actionClass = CommonLocator.get().classLookup()
					.getClassForName(this.actionClassName);
		}
		return this.actionClass;
	}

	public void setActionClass(Class<? extends RemoteAction> actionClass) {
		this.actionClass = actionClass;
	}
}
