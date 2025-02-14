package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;

/**
 * This type's hiearchy is designed so that criterion-type implementation can be
 * reused (by the final, non-generic Handler subtype extending both a
 * SearchDefinition-specific super<b>class</b> and a logic-specific
 * super<b>interface</b>, with genericBounds type information computed from the
 * parameterized superclass
 * 
 * So - say -
 * {@code ArchiveCriterionHandler extends DomainTransformEventInfoCriterionHandler<ArchiveCriterion>
			implements BaseEnumCriterionHandler<DomainTransformEventInfo, BooleanEnum, ArchiveCriterion>}
 * 
 * 
 *
 * @param <SC>
 */
@Registration(DomainCriterionHandler.class)
public abstract class DomainCriterionHandler<SC extends SearchCriterion>
		implements DomainCriterionFilter<SC> {
	public Class<SC> handlesSearchCriterion() {
		return Reflections.at(getClass()).firstGenericBound();
	}

	public abstract Class<? extends SearchDefinition> handlesSearchDefinition();

	/*
	 * non-graph handlers should return 1
	 */
	public int queryCost() {
		return 0;
	}
}
