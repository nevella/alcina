package cc.alcina.template.cs.persistent;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;

import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;

import com.google.gwt.user.client.rpc.GwtTransient;

@Table(name = "client_instance")
@javax.persistence.Entity
@SequenceGenerator(allocationSize = 1, name = "client_instance_id_seq", sequenceName = "client_instance_id_seq")
@RegistryLocations({
	@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = ClientInstance.class),
	@RegistryLocation(registryPoint = ClientInstance.class) })
public class ClientInstanceImpl extends ClientInstance implements Serializable {
	@GwtTransient
	private AlcinaTemplateUser user;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY, targetEntity = AlcinaTemplateUser.class)
	@JoinColumn(name = "user_id")
	public IUser getUser() {
		return user;
	}

	public void setUser(IUser user) {
		this.user = (AlcinaTemplateUser) user;
	}

	@Override
	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(generator = "client_instance_id_seq")
	public long getId() {
		return super.getId();
	}

	@Override
	public void setId(long id) {
		super.setId(id);
	}

	@Override
	public ClientInstanceImpl clone() {
		ClientInstanceImpl instance = new ClientInstanceImpl();
		instance.user = user;
		copyPropertiesTo(instance);
		return instance;
	}
}
