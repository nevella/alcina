package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;

@BeanInfo(displayNamePropertyName = "displayName")
@PermissibleChildClasses( { PersistentObjectCriterion.class })
public class PersistentObjectCriteriaGroup extends
		CriteriaGroup<PersistentObjectCriterion> {
	public PersistentObjectCriteriaGroup() {
		super();
		setEntityClass(ClassRef.class);
		setDisplayName("Object type");
	}
}
