package cc.alcina.framework.common.client.search;

public @interface CriterionPropertyNameMapping {
	Class<? extends CriteriaGroup> criteriaGroupClass();
	Class<? extends SearchCriterion> criterionClass();
	String propertyName();
}
