package cc.alcina.framework.common.client.logic.reflection.jvm;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector2;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.Property;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.util.AnnotationUtils;

import com.google.gwt.core.client.GwtScriptOnly;
import com.totsp.gwittir.client.beans.annotations.Omit;

/**
 * never actually used, but means we don't have to do weird things for
 * hosted-mode version
 */
@GwtScriptOnly
public class ClientReflectorJvm extends ClientReflector2 {
	Map<Class, ClientBeanReflector> reflectors = new HashMap<Class, ClientBeanReflector>();

	public ClientReflectorJvm() {
	}

	public ClientBeanReflector beanInfoForClass(Class clazz) {
		return null;
	}

	public Class forName(String fqn) {
		return null;
	}

	@Override
	protected <T> T newInstance0(Class<T> clazz, long objectId, long localId) {
		// not called
		return null;
	}

	@Override
	public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
		return null;
	}

	public List<PropertyInfo> getWritableProperties(Class clazz) {
		return null;
	}

	public static void checkClassAnnotations(Class clazz) {
		return;
	}

	@Override
	public Map<String, Property>
			getPropertyReflectors(Class<?> beanClass) {
		return null;
	}

	class MethodWrapper implements com.totsp.gwittir.client.beans.Method {
		public MethodWrapper(Method method) {
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public Object invoke(Object target, Object[] args) throws Exception {
			return null;
		}

		@Override
		public Class getDeclaringClass() {
			return null;
		}
	}

	@Override
	protected void initialiseNewInstance(Class clazz) {
	}
}