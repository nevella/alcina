package cc.alcina.framework.common.client.logic.reflection.jvm;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.annotations.Introspectable;
import com.totsp.gwittir.client.beans.annotations.Omit;
import com.totsp.gwittir.client.beans.internal.JVMIntrospector.MethodWrapper;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.IgnoreIntrospectionChecks;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.registry.ClassDataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.AnnotationUtils;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;

/**
 *
 */
public class ClientReflectorJvm extends ClientReflector {
	Map<Class, ClientBeanReflector> reflectors = new HashMap<Class, ClientBeanReflector>();

	public static final String CONTEXT_MODULE_NAME = ClientReflectorJvm.class
			+ ".CONTEXT_MODULE_NAME";

	public static final String PROP_FILTER_CLASSNAME = "ClientReflectorJvm.filterClassName";

	public ClientReflectorJvm() {
		try {
			ClassDataCache classes = null;
			boolean cacheIt = ResourceUtilities.is(ClientReflectorJvm.class,
					"cacheClasspathScan");
			File cacheFile = cacheIt ? new File(ResourceUtilities
					.get(ClientReflectorJvm.class, "cacheClasspathScanFile"))
					: null;
			if (cacheIt && cacheFile.exists()) {
				try {
					classes = KryoUtils.deserializeFromFile(cacheFile,
							ClassDataCache.class);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (classes == null) {
				classes = new ServletClasspathScanner("*", true, false, null,
						Registry.MARKER_RESOURCE,
						Arrays.asList(new String[] {})).getClasses();
				if (cacheIt) {
					KryoUtils.serializeToFile(classes, cacheFile);
				}
			}
			String filterClassName = System.getProperty(PROP_FILTER_CLASSNAME);
			/*
			 * The reason for this is that gwt needs the compiled annotation
			 * classes (in say, /bin) - so we may be getting classes here that
			 * shouldn't be visible via the registry
			 * 
			 * It's a bit sad (duplicating the exclusion code of the gwt
			 * module), but the performance gains the jvm reflector gives us
			 * outweigh the (possible) crud IMO
			 */
			if (filterClassName != null) {
				CollectionFilters
						.filterInPlace(classes.classData.keySet(),
								(CollectionFilter<String>) Class
										.forName(filterClassName)
										.newInstance());
			}
			CollectionFilter<String> defaultExcludes = new CollectionFilter<String>() {
				@Override
				public boolean allow(String o) {
					if (o.contains("AlcinaBeanSerializerJvm")) {
						return false;
					}
					if (o.contains("FastUtil")) {
						return false;
					}
					return true;
				}
			};
			CollectionFilters.filterInPlace(classes.classData.keySet(),
					defaultExcludes);
			new RegistryScanner() {
				protected File getHomeDir() {
					String testStr = "";
					String homeDir = (System.getenv("USERPROFILE") != null)
							? System.getenv("USERPROFILE")
							: System.getProperty("user.home");
					String moduleName = GWT.isClient() ? GWT.getModuleName()
							: LooseContext.containsKey(CONTEXT_MODULE_NAME)
									? LooseContext.get(CONTEXT_MODULE_NAME)
									: "server";
					File file = new File(String.format(
							"%s/.alcina/gwt-client/%s", homeDir, moduleName));
					file.mkdirs();
					return file;
				};

				protected Class maybeNormaliseClass(Class c) {
					if (c.getClassLoader() != this.getClass()
							.getClassLoader()) {
						try {
							c = this.getClass().getClassLoader()
									.loadClass(c.getName());
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					}
					return c;
				}
			}.scan(classes, new ArrayList<String>(), Registry.get(),
					"client-reflector");
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
				if (m == null) {
					continue;
				}
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
					if (getAnnotation(a.annotationType(),
							ClientVisible.class) == null
							|| a.annotationType()
									.getName() == RegistryLocation.class
											.getName()) {
						continue;
					}
					retained.add(a);
				}
				propertyReflectors.put(pd.getName(),
						new ClientPropertyReflector(pd.getName(),
								pd.getPropertyType(),
								(Annotation[]) retained.toArray(
										new Annotation[retained.size()])));
			}
			reflectors.put(clazz, new ClientBeanReflector(clazz,
					clazz.getAnnotations(), propertyReflectors));
		}
		return reflectors.get(clazz);
	}

	private boolean hasBeanInfo(Class clazz) {
		return (clazz.getModifiers() & Modifier.ABSTRACT) == 0
				&& (clazz.getModifiers() & Modifier.PUBLIC) > 0
				&& !clazz.isInterface() && !clazz.isEnum()
				&& getAnnotation(clazz,
						cc.alcina.framework.common.client.logic.reflection.Bean.class) != null;
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

	UnsortedMultikeyMap<Annotation> annotationLookup = new UnsortedMultikeyMap<Annotation>(
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
			checkClassAnnotationsForInstantiation(clazz);
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

	@Override
	protected <T> T newInstance0(Class<T> clazz, long objectId, long localId) {
		return null;
	}

	static Set<Class> checkedClassAnnotationsForInstantiation = new LinkedHashSet<Class>();

	static Set<Class> checkedClassAnnotations = new LinkedHashSet<Class>();

	public static void checkClassAnnotationsForInstantiation(Class clazz) {
		if (checkedClassAnnotationsForInstantiation.contains(clazz)) {
			return;
		}
		checkClassAnnotations(clazz);
		if (!AnnotationUtils.hasAnnotationNamed(clazz, ClientInstantiable.class)
				&& clazz.getAnnotation(IgnoreIntrospectionChecks.class) == null
				&& clazz.getAnnotation(
						cc.alcina.framework.common.client.logic.reflection.Bean.class) == null) {
			throw new IntrospectionException(
					"not reflect-instantiable class - no clientinstantiable/beandescriptor annotation",
					clazz);
		}
		checkedClassAnnotationsForInstantiation.add(clazz);
	}

	public static void checkClassAnnotations(Class clazz) {
		if (checkedClassAnnotations.contains(clazz)) {
			return;
		}
		int mod = clazz.getModifiers();
		if (Modifier.isAbstract(mod) || clazz.isAnonymousClass()
				|| (clazz.isMemberClass() && !Modifier.isStatic(mod))) {
			throw new IntrospectionException(
					"not reflectable class - abstract or non-static", clazz);
		}
		boolean introspectable = AnnotationUtils.hasAnnotationNamed(clazz,
				ClientInstantiable.class)
				|| clazz.getAnnotation(
						cc.alcina.framework.common.client.logic.reflection.Bean.class) != null
				|| clazz.getAnnotation(Introspectable.class) != null;
		for (Class iface : getAllImplementedInterfaces(clazz)) {
			introspectable |= iface.getAnnotation(Introspectable.class) != null;
		}
		if (!introspectable && clazz
				.getAnnotation(IgnoreIntrospectionChecks.class) == null) {
			throw new IntrospectionException(
					"not reflectable class - no clientinstantiable/beandescriptor/introspectable annotation",
					clazz);
		}
		checkedClassAnnotations.add(clazz);
	}

	public static class IntrospectionException extends RuntimeException {
		public IntrospectionException(String message, Class clazz) {
			super(CommonUtils.highlightForLog("reason: %s\nclass:%s", message,
					clazz));
		}
	}

	private static List<Class> getAllImplementedInterfaces(Class clazz) {
		List<Class> result = new ArrayList<Class>();
		while (clazz != null) {
			result.addAll(Arrays.asList(clazz.getInterfaces()));
			clazz = clazz.getSuperclass();
		}
		return result;
	}

	@Override
	protected void initialiseNewInstance(Class clazz) {
		// could log, i guess
	}
}