package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;

@BeanInfo(displayNamePropertyName = "displayName")
@PermissibleChildClasses({ PersistentObjectCriterion.class })
public class PersistentObjectCriteriaGroup extends
		CriteriaGroup<PersistentObjectCriterion> {
	static final transient long serialVersionUID = -1L;

	public PersistentObjectCriteriaGroup() {
		super();
	}

	@Override
	public Class getEntityClass() {
		return ClassRef.class;
	}

	@Override
	public String getDisplayName() {
		return "Object type";
	}

	@Override
	public CriteriaGroup clone() throws CloneNotSupportedException {
		return new PersistentObjectCriteriaGroup().deepCopyFrom(this);
	}
}
