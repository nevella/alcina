package cc.alcina.framework.common.client.search;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(value = { ElementType.TYPE })
@Inherited
public @interface CriterionPropertyNameMapping {
	Class<? extends CriteriaGroup> criteriaGroupClass();

	Class<? extends SearchCriterion> criterionClass();

	String propertyName();
}
