package cc.alcina.framework.entity.gwt.headless;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ClientBundle.Source;
import com.google.gwt.resources.client.ResourcePrototype;
import com.google.gwt.resources.client.TextResource;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.Io.ReadOp;
import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

// FIXME - romcom - finer cache invalidation based on dep resources
public class ClientBundleFactory implements RegistryFactory<ClientBundle> {
	static Configuration.Key cacheEnabled = Configuration.key("cacheEnabled");

	@Override
	public ClientBundle impl() {
		Class<? extends ClientBundle> registrationClass = Reflections
				.at(getClass()).annotation(Registration.class).value()[0];
		return (ClientBundle) Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { registrationClass },
				new ClientBundleHandler(registrationClass));
	}

	ConcurrentHashMap<Method, ResourcePrototype> cache = new ConcurrentHashMap<>();

	class ClientBundleHandler implements InvocationHandler {
		Class<? extends ClientBundle> bundleClass;

		ClientBundleHandler(Class<? extends ClientBundle> bundleClass) {
			this.bundleClass = bundleClass;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (cacheEnabled.is()) {
				return cache.computeIfAbsent(method, this::computeResource);
			} else {
				return computeResource(method);
			}
		}

		ResourcePrototype computeResource(Method method) {
			Source source = method.getAnnotation(Source.class);
			ReadOp readOp = Io.read().relativeTo(bundleClass)
					.resource(source.value()[0]);
			Class<?> returnType = method.getReturnType();
			if (returnType == SimpleCssResource.class) {
				return new SimpleCssResourceImpl(readOp.asString());
			} else if (returnType == TextResource.class) {
				return new TextResourceImpl(readOp.asString());
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	/*
	 * This impl doesn't try to do any of the interpolation that
	 * SimpleCssResourceGenerator has does - it assumes images are all base64
	 * rather than classpath resources )sass please)
	 */
	class SimpleCssResourceImpl implements SimpleCssResource {
		String text;

		public SimpleCssResourceImpl(String text) {
			this.text = text;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getText() {
			return text;
		}
	}

	class TextResourceImpl implements TextResource {
		String text;

		public TextResourceImpl(String text) {
			this.text = text;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getText() {
			return text;
		}
	}
}
