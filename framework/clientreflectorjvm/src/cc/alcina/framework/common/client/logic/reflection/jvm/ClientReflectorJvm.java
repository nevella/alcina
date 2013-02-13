package cc.alcina.framework.common.client.logic.reflection.jvm;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.AnnotationUtils;
import cc.alcina.framework.entity.util.ClasspathScanner;

import com.totsp.gwittir.client.beans.annotations.Omit;

/**
 *
 */
public class ClientReflectorJvm extends ClientReflector {
	Map<Class, ClientBeanReflector> reflectors = new HashMap<Class, ClientBeanReflector>();

	public ClientReflectorJvm() {
		try {
			Map<String, Date> classes = new ClasspathScanner("*", true, true)
					.getClasses();
			new RegistryScanner() {
				protected File getHomeDir() {
					String testStr = "";
					String homeDir = (System.getenv("USERPROFILE") != null) ? System
							.getenv("USERPROFILE") : System
							.getProperty("user.home");
					File file = new File(homeDir + File.separator + ".alcina"
							+ testStr + File.separator + "/gwt-client");
					file.mkdirs();
					return file;
				};
			}.scan(classes, new ArrayList<String>(), Registry.get());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public ClientBeanReflector beanInfoForClass(Class clazz) {
		if (!hasBeanInfo(clazz)) {
			return null;
		}
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
					if (a.annotationType().getName() == Omit.class.getName()) {
						ignore = true;
					}
				}
				if (ignore) {
					continue;
				}
				List<Annotation> retained = new ArrayList<Annotation>();
				for (Annotation a : annotations) {
					if (a.annotationType().getName() == Omit.class.getName()) {
						ignore = true;
					}
					if (getAnnotation(a.annotationType(), ClientVisible.class) == null
							|| a.annotationType().getName() == RegistryLocation.class
									.getName()) {
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

	private boolean hasBeanInfo(Class clazz) {
		return (clazz.getModifiers() & Modifier.ABSTRACT) == 0
				&& (clazz.getModifiers() & Modifier.PUBLIC) > 0
				&& !clazz.isInterface()
				&& !clazz.isEnum()
				&& getAnnotation(
						clazz,
						cc.alcina.framework.common.client.logic.reflection.BeanInfo.class) != null;
	}

	// we use annotation classnames because the annotation and class may be from
	// different classloaders (gwt compiling classloader)
	private <A extends Annotation> A getAnnotation(Class from,
			Class<A> annotationClass) {
		if (!annotationLookup.containsKey(from)) {
			for (Annotation a : from.getAnnotations()) {
				annotationLookup.put(from, a.annotationType().getName(), a);
			}
		}
		return (A) annotationLookup.get(from, annotationClass.getName());
	}

	LookupMapToMap<Annotation> annotationLookup = new LookupMapToMap<Annotation>(
			2);

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
			infos.add(new PropertyInfoLite(pd.getPropertyType(), pd.getName(),
					new MethodWrapper(pd.getReadMethod()), clazz));
		}
		return infos;
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