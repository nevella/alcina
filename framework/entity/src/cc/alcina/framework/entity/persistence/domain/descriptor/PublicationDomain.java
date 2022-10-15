package cc.alcina.framework.entity.persistence.domain.descriptor;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.PublicationCounter;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.DomainStoreDescriptor;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;

@Registration.Singleton
public class PublicationDomain {
	public static PublicationDomain get() {
		return Registry.impl(PublicationDomain.class);
	}

	private Class<? extends PublicationCounter> publicationCounterImpl;

	private Class<? extends Entity> iUserImpl;

	public void configureDescriptor(DomainStoreDescriptor descriptor) {
		this.iUserImpl = (Class<? extends Entity>) PersistentImpl
				.getImplementation(IUser.class);
		this.publicationCounterImpl = (Class<? extends PublicationCounter>) PersistentImpl
				.getImplementation(PublicationCounter.class);
		descriptor.addClassDescriptor(publicationCounterImpl);
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
					.getPersistenceEventType() == DomainTransformPersistenceEventType.PREPARE_COMMIT) {
				AdjunctTransformCollation collation = event
						.getTransformPersistenceToken().getTransformCollation();
				if (collation.has(iUserImpl)) {
					event.getTransformPersistenceToken().addCascadedEvents();
					collation.ensureApplied();
					collation.query(iUserImpl).stream().forEach(qr -> {
						if (qr.hasCreateTransform()) {
							if (!qr.hasDeleteTransform()) {
								IUser iUser = qr.getEntity();
								PublicationCounter counter = PersistentImpl
										.create(PublicationCounter.class);
								counter.setUser(iUser);
							}
						} else if (qr.hasDeleteTransform()) {
							// will have been deleted from graph, so use locator
							EntityLocator locator = qr.events.get(0)
									.toObjectLocator();
							event.getTransformPersistenceToken()
									.markForPrepend(() -> Domain.stream(
											PersistentImpl.getImplementation(
													PublicationCounter.class))
											.filter(pc -> ((Entity) pc
													.getUser()).toLocator()
															.equals(locator))
											.forEach(Entity::delete));
							event.getTransformPersistenceToken()
									.addCascadedEvents();
						}
					});
				}
				event.getTransformPersistenceToken().addCascadedEvents();
			}
		}
	}
}
