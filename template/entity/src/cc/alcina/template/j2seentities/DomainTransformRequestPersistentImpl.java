package cc.alcina.template.j2seentities;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;

import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.template.cs.persistent.ClientInstanceImpl;



@javax.persistence.Entity
@Table(name = "domain_transform_request")
@SequenceGenerator(allocationSize=1,name = "domain_transform_request_id_seq", sequenceName = "domain_transform_request_id_seq")
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = DomainTransformRequestPersistent.class)
public class DomainTransformRequestPersistentImpl extends
		DomainTransformRequestPersistent {
	@Override
	public void wrap(DomainTransformRequest dtr) {
		ResourceUtilities.copyBeanProperties(dtr, this,null,true);
	}

	@Override
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY, targetEntity = ClientInstanceImpl.class)
	public ClientInstance getClientInstance() {
		return super.getClientInstance();
	}
	@Override
	public void setClientInstance(ClientInstance clientInstance) {
		super.setClientInstance(clientInstance);
	}
	@Override
	@OneToMany(cascade = { CascadeType.MERGE, CascadeType.REMOVE,
			CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "domainTransformRequestPersistent",targetEntity=DomainTransformEventPersistentImpl.class)
	public List<DomainTransformEvent> getEvents() {
		return super.getEvents();
	}
	@Override
	public void setEvents(List<DomainTransformEvent> events) {
		super.setEvents(events);
	}
	@Override
	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "domain_transform_request_id_seq")
	public long getId() {
		return super.getId();
	}
	@Override
	public void setId(long id) {
		super.setId(id);
	}
}
