package cc.alcina.framework.common.client.log;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.log.TaggedLogger.TaggedLoggerHandler;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@RegistryLocation(registryPoint = TaggedLoggers.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class TaggedLoggers {
	public synchronized static TaggedLoggers get() {
		// allow for calls before registry initialised
		TaggedLoggers loggers = Registry.implOrNull(TaggedLoggers.class);
		if (loggers == null) {
			loggers = new TaggedLoggers();
			Registry.registerSingleton(TaggedLogger.class, loggers);
		}
		return loggers;
	}

	private List<TaggedLoggerRegistration> registrations = new ArrayList<TaggedLoggerRegistration>();

	private int registrationCounter = 0;

	public TaggedLogger getLogger(final Class clazz,
			final TaggedLoggerTag... tags) {
		return new TaggedLogger(this, clazz, tags);
	}

	public int getRegistrationCounter() {
		return this.registrationCounter;
	}

	public void log(String message, final Class clazz,
			final TaggedLoggerTag... tags) {
		new TaggedLogger(this, clazz, tags).log(message);
	}

	public TaggedLoggerRegistration registerInterest(Class clazz,
			TaggedLoggerHandler handler, TaggedLoggerTag... tags) {
		synchronized (this) {
			TaggedLoggerRegistration registration = new TaggedLoggerRegistration(
					clazz, tags, handler, this);
			registrations.add(registration);
			registrationCounter++;// some slight possibility of sync issues
									// here,
									// but will not hurt anything
			return registration;
		}
	}

	public void unsubscribe(TaggedLoggerRegistration taggedLoggerRegistration) {
		registrations.remove(taggedLoggerRegistration);
	}

	void updateRegistrations(final TaggedLogger taggedLogger) {
		if (taggedLogger.registrationCounter == registrationCounter) {
			return;
		}
		synchronized (this) {
			taggedLogger.registrationCounter = registrationCounter;
			taggedLogger.registrations.clear();
			CollectionFilter<TaggedLoggerRegistration> subscribesToFilter = new CollectionFilter<TaggedLoggerRegistration>() {
				@Override
				public boolean allow(TaggedLoggerRegistration o) {
					return o.subscribesTo(taggedLogger.clazz,
							taggedLogger.tags);
				}
			};
			taggedLogger.registrations.addAll(CollectionFilters
					.filter(registrations, subscribesToFilter));
		}
	}
}