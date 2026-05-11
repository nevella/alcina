package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

/**
 * Does not generate eql, rather used in property queries. The
 * class/propertyname is verified before use (and this criterion will often be
 * internal-only)
 */
@TypeSerialization("propertyname")
@Bean(PropertySource.FIELDS)
public class PropertyNameCriterion extends SearchCriterion {
	public Filter value = new Filter();

	@TypedProperties
	public static class Filter extends Bindable.Fields
			implements TreeSerializable {
		public Class<? extends Bindable> type;

		public String propertyName;

		public String serializedPropertyValue;
	}
}
