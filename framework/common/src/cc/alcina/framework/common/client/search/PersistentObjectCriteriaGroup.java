package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

/**
 * <p>
 * Unlike NotPersistentObjectCriteriaGroup, does *not* filter by value refs -
 * only object refs. More useful that way (since results are just the class, and
 * the value mods are *almost* always an association
 * 
 * @author nick@alcina.cc
 *
 */
@PermissibleChildClasses({ PersistentObjectCriterion.class })
// TODO - make flat-serializable when needed
@TypeSerialization(flatSerializable = false)
public class PersistentObjectCriteriaGroup
		extends CriteriaGroup<PersistentObjectCriterion> {
	public PersistentObjectCriteriaGroup() {
		super();
		setCombinator(FilterCombinator.OR);
	}

	@Override
	public Class entityClass() {
		return ClassRef.class;
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "Object type";
	}
}
