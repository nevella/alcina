package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = PerThreadTransaction.class, implementationType = ImplementationType.INSTANCE)
public class PerThreadTransaction {
	TransactionalSubgraphTransformManager transactionTransformManager;

	private DomainTransformListener transformListener = new DomainTransformListener() {
		@Override
		public void domainTransform(DomainTransformEvent evt)
				throws DomainTransformException {
			if (!AlcinaMemCache.get().isCached(evt.getObjectClass())) {
				return;
			}
			transactionTransformManager.consume(evt);
		}
	};

	private boolean running;

	public PerThreadTransaction() {
	}

	public void start() {
		running = true;
		transactionTransformManager = new TransactionalSubgraphTransformManager();
		TransformManager.get().addDomainTransformListener(transformListener);
	}

	public void end() {
		running = false;
		TransformManager.get().removeDomainTransformListener(transformListener);
	}

	public <V extends HasIdAndLocalId> V getListenerValue(
			CacheListener listener, V value, Object[] path) {
		Collection<? extends HasIdAndLocalId> perClassTransactional = (Collection<? extends HasIdAndLocalId>) transactionTransformManager.modified
				.getCollection(listener.getListenedClass());
		for (HasIdAndLocalId v : perClassTransactional) {
			if (listener.matches(v, path)) {
				return (V) v;// will always be transactional object
			}
		}
		if (value == null) {
			return null;
		}
		if (transactionTransformManager.deleted.contains(value)) {
			value = null;
		}
		if (transactionTransformManager.modified.contains(value)) {
			return null;// didn't match the manual check, so nup
		}
		return transactionTransformManager.getObject(value);
	}

	public <V extends HasIdAndLocalId> V ensureTransactional(V v) {
		return transactionTransformManager.getObject(v);
	}

	public boolean isRunning() {
		return this.running;
	}

	public void committing() {
		TransformManager.get().removeDomainTransformListener(transformListener);
	}

	
	public Collection<? extends Object> rawValues(Class clazz,
			DetachedEntityCache cache) {
		Set values = cache.values(clazz);
		Collection<? extends HasIdAndLocalId> perClassTransactional = (Collection<? extends HasIdAndLocalId>) transactionTransformManager.modified
				.getCollection(clazz);
		values.addAll(perClassTransactional);
		return values;
	}
}
