package cc.alcina.framework.common.client.search;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;

@PermissibleChildClasses({ PersistentObjectCriterion.class })
// TODO - make flat-serializable when needed
@TypeSerialization(flatSerializable = false)
public class NotPersistentObjectCriteriaGroup
		extends CriteriaGroup<PersistentObjectCriterion> {
	static final transient long serialVersionUID = -1L;

	public NotPersistentObjectCriteriaGroup() {
		super();
		setCombinator(FilterCombinator.OR);
	}

	@Override
	public Class entityClass() {
		return ClassRef.class;
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters ewp = super.eql();
		if (ewp.eql.length() > 0) {
			ewp.eql = Ax.format(
					"(NOT (%s) AND (t.valueClassRef.id IS NULL OR NOT (%s)))",
					ewp.eql,
					ewp.eql.replace("objectClassRef", "valueClassRef"));
			// duplicate parameters
			ewp.parameters = (List) Stream
					.concat(ewp.parameters.stream(), ewp.parameters.stream())
					.collect(Collectors.toList());
		}
		return ewp;
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "Object type";
	}
}
