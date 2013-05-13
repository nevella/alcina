package cc.alcina.framework.jvmclient.reflection;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.util.AnnotationUtils;

import com.totsp.gwittir.client.beans.annotations.Omit;

/**
 *
 */
public class ClientReflectorJvm extends ClientReflector {
	Map<Class, ClientBeanReflector> reflectors = new HashMap<Class, ClientBeanReflector>();

	public ClientReflectorJvm() {
	}

	public ClientBeanReflector beanInfoForClass(Class clazz) {
		if (!reflectors.containsKey(clazz)) {
			Map<String, ClientPropertyReflector> propertyReflectors = new HashMap<String, ClientPropertyReflector>();
			BeanInfo beanInfo = null;
			try {
				beanInfo = Introspector.getBeanInfo(clazz);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getName().equals("class")
						|| pd.getName().equals("propertyChangeListeners")) {
					continue;
				}
				Method m = pd.getReadMethod();
				Collection<Annotation> annotations = AnnotationUtils
						.getSuperclassAnnotationsForMethod(m);
				int aCount = 0;
				boolean ignore = false;
				for (Annotation a : annotations) {
					if (a.annotationType() == Omit.class) {
						ignore = true;
					}
				}
				if (ignore) {
					continue;
				}
				List<Annotation> retained = new ArrayList<Annotation>();
				for (Annotation a : annotations) {
					if (a.annotationType() == Omit.class) {
						ignore = true;
					}
					if (!a.annotationType().isAnnotationPresent(
							ClientVisible.class)
							|| a.annotationType() == RegistryLocation.class) {
						continue;
					}
					retained.add(a);
				}
				propertyReflectors.put(
						pd.getName(),
						new ClientPropertyReflector(pd.getName(), pd
								.getPropertyType(), (Annotation[]) retained
								.toArray(new Annotation[retained.size()])));
			}
			reflectors.put(clazz,
					new ClientBeanReflector(clazz, clazz.getAnnotations(),
							propertyReflectors));
		}
		return reflectors.get(clazz);
	}

	public Class getClassForName(String fqn) {
		try {
			return Class.forName(fqn);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public boolean isDefined() {
		return true;
	}

	@Override
	public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
		try {
			T newInstance = clazz.newInstance();
			if (localId != 0) {
				HasIdAndLocalId hili = (HasIdAndLocalId) newInstance;
				hili.setLocalId(localId);
			}
			return newInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public List<PropertyInfoLite> getWritableProperties(Class clazz) {
		BeanInfo beanInfo = null;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
		List<PropertyInfoLite> infos = new ArrayList<PropertyInfoLite>();
		for (PropertyDescriptor pd : pds) {
			if (pd.getName().equals("class")
					|| pd.getName().equals("propertyChangeListeners")
					|| pd.getWriteMethod() == null) {
				continue;
			}
			Field field = getField(clazz, pd.getName());
			if (field != null && Modifier.isTransient(field.getModifiers())) {
				continue;
			}
			infos.add(new PropertyInfoLite(pd.getPropertyType(), pd.getName(),
					new MethodWrapper(pd.getReadMethod()), clazz));
		}
		return infos;
	}

	public static Field getField(Class<?> clazz, String fieldName) {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			Class<?> superClass = clazz.getSuperclass();
			if (superClass == null) {
				return null;
			} else {
				return getField(superClass, fieldName);
			}
		}
	}

	class MethodWrapper implements com.totsp.gwittir.client.beans.Method {
		private final Method method;

		public MethodWrapper(Method method) {
			this.method = method;
		}

		@Override
		public String getName() {
			return method.getName();
		}

		@Override
		public Object invoke(Object target, Object[] args) throws Exception {
			return method.invoke(target, args);
		}
	}
}