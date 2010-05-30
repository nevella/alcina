package cc.alcina.template.cs.persistent;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@Table(name="classref")
@Entity
@SequenceGenerator(name = "classref_id_seq", sequenceName = "classref_id_seq")
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = ClassRef.class)
public class ClassRefImpl extends ClassRef implements Serializable {
	long id;

	

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "classref_id_seq")
	
	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	
}
