package cc.alcina.framework.entity.persistence.domain.segment;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@TypeSerialization("property")
@Bean(PropertySource.FIELDS)
public class DomainSegmentProperty {
	public String name;

	public String value;
}