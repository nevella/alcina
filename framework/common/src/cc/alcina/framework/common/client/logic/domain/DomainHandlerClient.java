package cc.alcina.framework.common.client.logic.domain;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.domain.Domain.DomainHandler;
import cc.alcina.framework.common.client.domain.DomainListener;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class DomainHandlerClient implements DomainHandler {
	@Override
	public <V extends Entity> void async(Class<V> clazz, long objectId,
			boolean create, Consumer<V> resultConsumer) {
		if (create) {
			V obj = TransformManager.get().createProvisionalObject(clazz);
			Scheduler.get().scheduleDeferred(() -> resultConsumer.accept(obj));
			return;
		} else {
			if (objectId == 0) {
				resultConsumer.accept(null);
			} else {
				Registry.impl(DomainHandlerClientRemoteResolver.class)
						.resolve(clazz, objectId, resultConsumer);
			}
		}
	}

	@Override
	public void commitPoint() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends Entity> V detachedVersion(V v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends Entity> V find(Class clazz, long id) {
		return (V) TransformManager.get().getObject(clazz, id, 0);
	}

	@Override
	public <V extends Entity> V find(V v) {
		V find = find(v.getClass(), v.getId());
		return find == null ? v : find;
	}

	@Override
	public <V extends Entity> List<Long> ids(Class<V> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends Entity> boolean isDomainVersion(V v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends Entity> V resolveTransactional(
			DomainListener listener, V value, Object[] path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends Entity> Stream<V> stream(Class<V> clazz) {
		return values(clazz).stream();
	}

	@Override
	public <V extends Entity> V transactionalFind(Class clazz,
			long id) {
		return find(clazz, id);
	}

	@Override
	public <V extends Entity> V transactionalVersion(V v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends Entity> Collection<V> values(Class<V> clazz) {
		return TransformManager.get().getCollection(clazz);
	}

	@Override
	public <V extends Entity> V writeable(V v) {
		TransformManager.get().registerDomainObject(v);
		return v;
	}

	public interface DomainHandlerClientRemoteResolver {
		<V extends Entity> void resolve(Class<V> clazz, long objectId,
				Consumer<V> resultConsumer);
	}
}