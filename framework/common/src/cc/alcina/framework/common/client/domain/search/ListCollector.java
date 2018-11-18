package cc.alcina.framework.common.client.domain.search;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = ListCollector.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.DEFAULT_PRIORITY)
@ClientInstantiable
@Introspectable
public class ListCollector {
	public <T> Collector<T, ?, List<T>> toList() {
		return Collectors.toList();
	}
}