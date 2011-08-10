package cc.alcina.template.j2seentities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.template.cs.misc.search.DomainTransformEventInfo;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;
import cc.alcina.template.cs.persistent.ClassRefImpl;



@Entity
@Table(name = "domain_transform_event")
@SequenceGenerator(allocationSize=1,name = "domain_transform_event_id_seq", sequenceName = "domain_transform_event_id_seq")
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = DomainTransformEventPersistent.class)
public class DomainTransformEventPersistentImpl extends
		DomainTransformEventPersistent {
	private AlcinaTemplateUser user;

	@Override
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = DomainTransformRequestPersistentImpl.class)
	public DomainTransformRequestPersistent getDomainTransformRequestPersistent() {
		return super.getDomainTransformRequestPersistent();
	}

	@Override
	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "domain_transform_event_id_seq")
	public long getId() {
		return super.getId();
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	public AlcinaTemplateUser getUser() {
		return user;
	}

	@Override
	@ManyToOne(targetEntity=ClassRefImpl.class)
	public ClassRef getObjectClassRef() {
		return super.getObjectClassRef();
	}

	@Override
	@ManyToOne(targetEntity=ClassRefImpl.class)
	public ClassRef getValueClassRef() {
		return super.getValueClassRef();
	}
	@Lob
	@Type(type="org.hibernate.type.StringClobType")
	public String getNewStringValue() {
		return super.getNewStringValue();
	}
	@Override
	public void setDomainTransformRequestPersistent(
			DomainTransformRequestPersistent DomainTransformRequestPersistent) {
		super.setDomainTransformRequestPersistent(DomainTransformRequestPersistent);
	}

	@Override
	public void setId(long id) {
		super.setId(id);
	}

	public void setUser(IUser user) {
		this.user = (AlcinaTemplateUser) user;
	}

	@Override
	public void wrap(DomainTransformEvent evt) {
		ResourceUtilities.copyBeanProperties(evt, this, null, true);
		setUser(PermissionsManager.get().getUser());
	}

	public DomainTransformEventInfo unwrap() {
		DomainTransformEventInfo evt = new DomainTransformEventInfo();
		ResourceUtilities.copyBeanProperties(this, evt, null, true);
		evt.setId(getId());
		evt.setUserId(user == null ? 0 : user.getId());
		if (getValueId()!=0){
			evt.setNewStringValue("id: "+getValueId());
		}
		evt.setServerCommitDate(getServerCommitDate()!=null?getServerCommitDate():getUtcDate());
		return evt;
	}
}
