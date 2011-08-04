package cc.alcina.framework.jvmclient.reflection;

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
	public ClientReflectorJvm() {
	}

	Map<Class, ClientBeanReflector> reflectors = new HashMap<Class, ClientBeanReflector>();

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
}