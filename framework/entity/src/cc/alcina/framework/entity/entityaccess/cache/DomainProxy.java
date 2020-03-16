package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.PropertyChangeEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;

public interface DomainProxy<T> extends HasId {
	public static final String CONTEXT_DOMAIN_PROXY_CONTEXT = DomainProxy.class
			.getName() + ".CONTEXT_DOMAIN_PROXY_CONTEXT";

	public void beforeProjection();

	public void checkPropertyChange(PropertyChangeEvent propertyChangeEvent);

	public T nonProxy();

	public static class DomainProxyContext {
		public static final Supplier<DomainProxyContext> SUPPLIER = new Supplier<DomainProxy.DomainProxyContext>() {
			@Override
			public DomainProxyContext get() {
				return new DomainProxyContext();
			}
		};

		Map<EntityLocator, DomainProxy> projectionProxies = new LinkedHashMap<>();
	}
}
