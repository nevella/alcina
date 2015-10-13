package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import cc.alcina.framework.common.client.cache.CacheListener;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MultiIterator;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = PerThreadTransaction.class, implementationType = ImplementationType.INSTANCE)
public class PerThreadTransaction {
	protected TransactionalSubgraphTransformManager transactionTransformManager;

	private DomainTransformListener transformListener = new DomainTransformListener() {
		@Override
		public void domainTransform(DomainTransformEvent evt)
				throws DomainTransformException {
			if (!AlcinaMemCache.get().isCachedTransactional(evt.getObjectClass())) {
				return;
			}
			beforeConsume(evt);
			transactionTransformManager.consume(evt);
			afterConsume(evt);
		}
	};

	private boolean running;

	public PerThreadTransaction() {
	}

	protected void afterConsume(DomainTransformEvent evt) {
	}

	protected void beforeConsume(DomainTransformEvent evt) {
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
		// FIXME - n^2 performance - use a per-listener threaded projection
		// well - sorta fixed
		if (listener instanceof BaseProjectionHasEquivalenceHash) {
			V v = (V) ((BaseProjectionHasEquivalenceHash) listener)
					.matchesTransactional(perClassTransactional, path);
			if (v != null) {
				return v;
			}
		} else {
			for (HasIdAndLocalId v : perClassTransactional) {
				if (listener.matches(v, path)) {
					return (V) v;// will always be transactional object
				}
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

	public Set<? extends Object> immutableRawValues(Class clazz,
			DetachedEntityCache cache) {
		Set values = cache.values(clazz);
		Collection<? extends HasIdAndLocalId> perClassTransactional = (Collection<? extends HasIdAndLocalId>) transactionTransformManager.modified
				.getCollection(clazz);
		return new OptimisedBiSet(values, perClassTransactional);
	}

	static class OptimisedBiSet implements Set {
		private Set a;

		private Set bNotA;

		/* assumes size a>>b */
		public OptimisedBiSet(Set a, Collection b) {
			this.a = a;
			this.bNotA = CommonUtils.threeWaySplit(a, b).secondOnly;
		}

		@Override
		public int size() {
			return a.size() + bNotA.size();
		}

		@Override
		public boolean isEmpty() {
			return size() == 0;
		}

		@Override
		public boolean contains(Object o) {
			return a.contains(o) || bNotA.contains(o);
		}

		@Override
		public Iterator iterator() {
			return new MultiIterator(false, a.iterator(), bNotA.iterator());
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object[] toArray(Object[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(Object e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean containsAll(Collection c) {
			for (Object o : c) {
				if (!a.contains(o) && !bNotA.contains(o)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}
}
