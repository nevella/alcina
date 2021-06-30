package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;

@Bean
@PermissibleChildClasses({ PersistentObjectCriterion.class })
// TODO - make flat-serializable when needed
@TypeSerialization(notSerializable = true)
public class PersistentObjectCriteriaGroup
		extends CriteriaGroup<PersistentObjectCriterion> {
	static final transient long serialVersionUID = -1L;

	public PersistentObjectCriteriaGroup() {
		super();
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "Object type";
	}

	@Override
	public Class entityClass() {
		return ClassRef.class;
	}
}
