package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.PropertyChangeEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;

public interface MemCacheProxy<T> extends HasIdAndLocalId {
	public static final String CONTEXT_MEMCACHE_PROXY_CONTEXT = MemCacheProxy.class
			.getName() + ".CONTEXT_MEMCACHE_PROXY_CONTEXT";

	public void beforeProjection();

	public void checkPropertyChange(PropertyChangeEvent propertyChangeEvent);

	public T nonProxy();

	public static class MemcacheProxyContext {
		public static final Supplier<MemcacheProxyContext> SUPPLIER = new Supplier<MemCacheProxy.MemcacheProxyContext>() {
			@Override
			public MemcacheProxyContext get() {
				return new MemcacheProxyContext();
			}
		};

		Map<HiliLocator, MemCacheProxy> projectionProxies = new LinkedHashMap<>();
	}
}
