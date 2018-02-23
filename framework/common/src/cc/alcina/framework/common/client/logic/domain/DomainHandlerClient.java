package cc.alcina.framework.common.client.logic.domain;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.cache.CacheListener;
import cc.alcina.framework.common.client.cache.Domain.DomainHandler;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class DomainHandlerClient implements DomainHandler {
	public interface DomainHandlerClientRemoteResolver {
		<V extends HasIdAndLocalId> void resolve(Class<V> clazz, long objectId,
				Consumer<V> resultConsumer);
	}

	@Override
	public <V extends HasIdAndLocalId> void async(Class<V> clazz, long objectId,
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
	public <V extends HasIdAndLocalId> V find(Class clazz, long id) {
		return (V) TransformManager.get().getObject(clazz, id, 0);
	}

	@Override
	public <V extends HasIdAndLocalId> V find(V v) {
		V find = find(v.getClass(), v.getId());
		return find == null ? v : find;
	}

	@Override
	public <V extends HasIdAndLocalId> Collection<V> list(Class<V> clazz) {
		return TransformManager.get().getCollection(clazz);
	}

	@Override
	public <V extends HasIdAndLocalId> V resolveTransactional(
			CacheListener listener, V value, Object[] path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz) {
		return list(clazz).stream();
	}

	@Override
	public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
			long id) {
		return find(clazz, id);
	}

	@Override
	public <V extends HasIdAndLocalId> V writeable(V v) {
		TransformManager.get().registerDomainObject(v);
		return v;
	}

	@Override
	public void commitPoint() {
		throw new UnsupportedOperationException();
	}
}