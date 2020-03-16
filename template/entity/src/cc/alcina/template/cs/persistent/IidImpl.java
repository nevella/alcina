package cc.alcina.template.cs.persistent;

import java.io.Serializable;

import javax.persistence.CascadeType;

import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;


@Table(name="Iid")
@javax.persistence.Entity

@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = Iid.class)
public class IidImpl implements Serializable, Iid{
	long id;

	String instanceId;

	AlcinaTemplateUser rememberMeUser;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY,targetEntity=AlcinaTemplateUser.class)
	public IUser getRememberMeUser() {
		return this.rememberMeUser;
	}

	public void setRememberMeUser(IUser rememberMeUser) {
		this.rememberMeUser = (AlcinaTemplateUser) rememberMeUser;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getInstanceId() {
		return this.instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
}
