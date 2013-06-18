package cc.alcina.framework.common.client.log;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.log.TaggedLogger.TaggedLoggerHandler;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = TaggedLoggers.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class TaggedLoggers {
	private List<TaggedLoggerRegistration> registrations = new ArrayList<TaggedLoggerRegistration>();

	public TaggedLoggerRegistration registerInterest(Class clazz, TaggedLoggerHandler handler,
			Object... tags) {
		TaggedLoggerRegistration registration = new TaggedLoggerRegistration(clazz, tags,
						handler, this);
		registrations.add(registration);
		return registration;
	}

	public void unsubscribe(
			TaggedLoggerRegistration taggedLoggerRegistration) {
		registrations.remove(taggedLoggerRegistration);
	}

	public TaggedLogger getLogger(final Class clazz, final Object... tags) {
		CollectionFilter<TaggedLoggerRegistration> subscribesToFilter = new CollectionFilter<TaggedLoggerRegistration>() {
			@Override
			public boolean allow(TaggedLoggerRegistration o) {
				return o.subscribesTo(clazz, tags);
			}
		};
		List<TaggedLoggerRegistration> interested = CollectionFilters
				.filter(registrations, subscribesToFilter);
		return new TaggedLogger(interested);
	}
}