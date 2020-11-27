package cc.alcina.framework.entity.persistence.cache.descriptor;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.PublicationCounter;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.DomainStoreDescriptor;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;

@RegistryLocation(registryPoint = DomainDescriptorPublication.class, implementationType = ImplementationType.SINGLETON)
public class DomainDescriptorPublication {
	public static DomainDescriptorPublication get() {
		return Registry.impl(DomainDescriptorPublication.class);
	}

	private Class<? extends PublicationCounter> publicationImpl;

	private Class<? extends Entity> iUserImpl;

	public void configureDescriptor(DomainStoreDescriptor descriptor) {
		this.iUserImpl = (Class<? extends Entity>) AlcinaPersistentEntityImpl
				.getImplementation(IUser.class);
		this.publicationImpl = (Class<? extends PublicationCounter>) AlcinaPersistentEntityImpl
				.getImplementation(PublicationCounter.class);
		descriptor.addClassDescriptor(publicationImpl);
	}

	public void onWarmupComplete(DomainStore domainStore) {
		domainStore.getPersistenceEvents()
				.addDomainTransformPersistenceListener(
						new UserCreationListener());
	}

	private class UserCreationListener
			implements DomainTransformPersistenceListener {
		public UserCreationListener() {
		}

		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent event) {
			if (event
					.getPersistenceEventType() == DomainTransformPersistenceEventType.PRE_COMMIT) {
				AdjunctTransformCollation collation = event
						.getTransformPersistenceToken().getTransformCollation();
				if (collation.has(iUserImpl) && collation.query(iUserImpl)
						.withFilter(
								DomainTransformEvent::provideIsCreationTransform)
						.stream().count() > 0) {
					collation.ensureApplied();
					collation.query(iUserImpl).withFilter(
							DomainTransformEvent::provideIsCreationTransform)
							.stream().forEach(qr -> {
								IUser iUser = qr.getObject();
								PublicationCounter counter = AlcinaPersistentEntityImpl
										.create(PublicationCounter.class);
								counter.setUser(iUser);
							});
				}
			}
		}
	}
}
