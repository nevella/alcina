package cc.alcina.framework.common.client.logic.reflection.jvm;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.annotations.Introspectable;
import com.totsp.gwittir.client.beans.annotations.Omit;

import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.IgnoreIntrospectionChecks;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.AnnotationUtils;
import cc.alcina.framework.entity.util.CachingConcurrentMap;
import cc.alcina.framework.entity.util.JvmPropertyReflector;
import cc.alcina.framework.entity.util.MethodWrapper;

/**
 *
 * FIXME - mvcc.meta - registry - should *not* populate if class not visible
 * (due to module filter) (e,g.
 * cc.alcina.framework.common.client.logic.reflection.AnnotationLocation.DefaultResolver)
 */
public class ClientReflectorJvm extends ClientReflector {
	public static final String CONTEXT_MODULE_NAME = ClientReflectorJvm.class
			+ ".CONTEXT_MODULE_NAME";

	public static final String PROP_FILTER_CLASSNAME = "ClientReflectorJvm.filterClassName";

	// String seems better perf than class (jdk8)
	static Set<String> checkedClassAnnotationsForInstantiation = new LinkedHashSet<>();

	static Set<String> checkedClassAnnotationsForForName = new LinkedHashSet<>();

	static Set<String> checkedClassAnnotations = new LinkedHashSet<>();

	public static boolean canIntrospect(Class clazz) {
		if (checkedClassAnnotations.contains(clazz.getName())) {
			return true;
		}
		return checkClassAnnotationsGenerateException(clazz, false,
				false).canIntrospect;
	}

	public static void checkClassAnnotations(Class clazz) {
		if (checkedClassAnnotations.contains(clazz.getName())) {
			return;
		}
		IntrospectionCheckResult checkResult = checkClassAnnotationsGenerateException(
				clazz, false, true);
		if (!checkResult.canIntrospect) {
			throw checkResult.exception;
		}
		checkedClassAnnotations.add(clazz.getName());
	}

	public static void checkClassAnnotationsForForName(Class clazz) {
		if (checkedClassAnnotationsForForName.contains(clazz.getName())) {
			return;
		}
		IntrospectionCheckResult checkResult = checkClassAnnotationsGenerateException(
				clazz, true, true);
		if (!checkResult.canIntrospect) {
			throw checkResult.exception;
		}
		if (!AnnotationUtils.hasAnnotationNamed(clazz, ClientInstantiable.class)
				&& clazz.getAnnotation(IgnoreIntrospectionChecks.class) == null
				&& clazz.getAnnotation(
						cc.alcina.framework.common.client.logic.reflection.Bean.class) == null
				&& !clazz.getName().startsWith("java.lang")
				&& !CommonUtils.COLLECTION_CLASS_NAMES
						.contains(clazz.getCanonicalName())) {
			throw new IntrospectionException(
					"not reflect-instantiable class - no clientinstantiable/beandescriptor annotation",
					clazz);
		}
		checkedClassAnnotationsForForName.add(clazz.getName());
	}

	public static void checkClassAnnotationsForInstantiation(Class clazz) {
		if (checkedClassAnnotationsForInstantiation.contains(clazz.getName())) {
			return;
		}
		checkClassAnnotations(clazz);
		if (!AnnotationUtils.hasAnnotationNamed(clazz, ClientInstantiable.class)
				&& clazz.getAnnotation(IgnoreIntrospectionChecks.class) == null
				&& clazz.getAnnotation(
						cc.alcina.framework.common.client.logic.reflection.Bean.class) == null
				&& !clazz.getName().startsWith("java.lang")
				&& !CommonUtils.COLLECTION_CLASS_NAMES
						.contains(clazz.getCanonicalName())) {
			throw new IntrospectionException(
					"not reflect-instantiable class - no clientinstantiable/beandescriptor annotation",
					clazz);
		}
		checkedClassAnnotationsForInstantiation.add(clazz.getName());
	}

	// FIXME - metagen - clean all this up (unify annotation reflection/property
	// introspection/instantiation)
	private static IntrospectionCheckResult
			checkClassAnnotationsGenerateException(Class clazz,
					boolean allowAbstract, boolean generateExceptions) {
		IntrospectionCheckResult result = new IntrospectionCheckResult();
		int mod = clazz.getModifiers();
		if (!CommonUtils.isEnumOrEnumSubclass(clazz)) {
			if ((Modifier.isAbstract(mod) && !allowAbstract)
					|| clazz.isAnonymousClass()
					|| (clazz.isMemberClass() && !Modifier.isStatic(mod))) {
				if (generateExceptions) {
					result.exception = new IntrospectionException(
							"not reflectable class - abstract or non-static",
							clazz);
				}
				return result;
			}
		}
		boolean introspectable = AnnotationUtils.hasAnnotationNamed(clazz,
				ClientInstantiable.class)
				|| clazz.getAnnotation(
						cc.alcina.framework.common.client.logic.reflection.Bean.class) != null
				|| clazz.getAnnotation(Introspectable.class) != null;
		if (clazz.getName().startsWith("java.lang")) {
			introspectable = true;
		}
		if (CommonUtils.COLLECTION_CLASS_NAMES
				.contains(clazz.getCanonicalName())) {
			introspectable = true;
		}
		for (Class iface : getAllImplementedInterfaces(clazz)) {
			introspectable |= iface.getAnnotation(Introspectable.class) != null;
		}
		if (!introspectable && clazz
				.getAnnotation(IgnoreIntrospectionChecks.class) == null) {
			if (generateExceptions) {
				result.exception = new IntrospectionException(
						"not reflectable class - no clientinstantiable/beandescriptor/introspectable annotation",
						clazz);
			}
			return result;
		}
		result.canIntrospect = true;
		return result;
	}

	private static List<Class> getAllImplementedInterfaces(Class clazz) {
		List<Class> result = new ArrayList<Class>();
		while (clazz != null) {
			result.addAll(Arrays.asList(clazz.getInterfaces()));
			clazz = clazz.getSuperclass();
		}
		return result;
	}

	Map<Class, ClientBeanReflector> reflectors = new HashMap<Class, ClientBeanReflector>();

	UnsortedMultikeyMap<Annotation> annotationLookup = new UnsortedMultikeyMap<Annotation>(
			2);

	private Predicate<String> test;

	private CachingConcurrentMap<Class, Map<String, PropertyReflector>> classPropertyReflectorLookup = new CachingConcurrentMap<>(
			clazz -> SEUtilities.getPropertyDescriptorsSortedByField(clazz)
					.stream()
					// FIXME - mvcc.adjunct - generalise ignored properties
					.filter(pd -> !(pd.getName().equals("class")
							|| pd.getName().equals("propertyChangeListeners")))
					.map(pd -> JvmPropertyReflector.get(clazz, pd))
					.collect(AlcinaCollectors
							.toKeyMap(PropertyReflector::getPropertyName)),
			100);

	private Map<String, Class> forName = new LinkedHashMap<>();

	public ClientReflectorJvm() {
		try {
			LooseContext.pushWithKey(KryoUtils.CONTEXT_OVERRIDE_CLASSLOADER,
					ClassMetadata.class.getClassLoader());
			ResourceUtilities.ensureFromSystemProperties();
			ClassMetadataCache classes = new CachingClasspathScanner("*", true,
					false, null, Registry.MARKER_RESOURCE,
					Arrays.asList(new String[] {})).getClasses();
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
				test = (Predicate<String>) Class.forName(filterClassName)
						.newInstance();
				classes.classData.keySet().removeIf(test.negate());
			}
			Predicate<String> defaultExcludes = new Predicate<String>() {
				@Override
				public boolean test(String o) {
					if (o.contains("AlcinaBeanSerializerJvm")) {
						return false;
					}
					if (o.contains("FastUtil")) {
						return false;
					}
					if (o.contains(
							"DomainTransformCommitPositionProvider_EventsQueue")) {
						return false;
					}
					return true;
				}
			};
			classes.classData.keySet().removeIf(defaultExcludes.negate());
			// FIXME - 2023 - (requires some introspection info) - ignore result
			// if registrylocation has @NonClientRegistryPointType
			new RegistryScanner() {
				@Override
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

				@Override
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
		} catch (Throwable e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	public ClientBeanReflector beanInfoForClass(Class clazz) {
		BeaninfoClassResolver bea = Registry.impl(BeaninfoClassResolver.class);
		clazz = bea.resolveForBeanInfo(clazz);
		if (!hasBeanInfo(clazz)) {
			return null;
		}
		if (test != null && !test.test(clazz.getName())) {
			GWT.log(Ax.format(
					"Warn: accessing filtered (reflection) class:\n%s",
					clazz.getName()));
		}
		if (!reflectors.containsKey(clazz)) {
			Map<String, ClientPropertyReflector> propertyReflectors = new HashMap<String, ClientPropertyReflector>();
			for (PropertyDescriptor pd : SEUtilities
					.getPropertyDescriptorsSortedByField(clazz)) {
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
						new ClientPropertyReflector(clazz, pd.getName(),
								pd.getPropertyType(),
								(Annotation[]) retained.toArray(
										new Annotation[retained.size()])));
			}
			reflectors.put(clazz, new ClientBeanReflector(clazz,
					clazz.getAnnotations(), propertyReflectors));
		}
		return reflectors.get(clazz);
	}

	@Override
	public Class getClassForName(String name) {
		return forName.computeIfAbsent(name, fqn -> {
			try {
				Class<?> clazz = Class.forName(fqn);
				checkClassAnnotationsForForName(clazz);
				return clazz;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		});
	}

	@Override
	public List<Class> getInterfaces(Class clazz) {
		return Arrays.asList(clazz.getInterfaces());
	}

	@Override
	public Map<String, PropertyReflector>
			getPropertyReflectors(Class<?> beanClass) {
		return classPropertyReflectorLookup.get(beanClass);
	}

	@Override
	public List<PropertyInfo> getWritableProperties(Class clazz) {
		List<PropertyInfo> infos = new ArrayList<>();
		for (PropertyDescriptor pd : SEUtilities
				.getPropertyDescriptorsSortedByName(clazz)) {
			if (pd.getName().equals("class")
					|| pd.getName().equals("propertyChangeListeners")
					|| pd.getWriteMethod() == null) {
				continue;
			}
			infos.add(new PropertyInfo(pd.getPropertyType(), pd.getName(),
					new MethodWrapper(pd.getReadMethod()),
					new MethodWrapper(pd.getWriteMethod()), clazz));
		}
		return infos;
	}

	@Override
	public boolean isAssignableFrom(Class from, Class to) {
		// tricky - because we might want to check superclasses. Real test is
		// reusing clientreflectiongen in these checks
		// FIXME - dirndl 1.3
		// checkClassAnnotationsForForName(from);
		return from.isAssignableFrom(to);
	}

	@Override
	public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
		try {
			if (test != null && !test.test(clazz.getName())) {
				GWT.log(Ax.format(
						"Warn: accessing filtered (reflection) class:\n%s",
						clazz.getName()));
			}
			checkClassAnnotationsForInstantiation(clazz);
			T newInstance = clazz.newInstance();
			if (localId != 0) {
				Entity entity = (Entity) newInstance;
				entity.setLocalId(localId);
			}
			return newInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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

	private boolean hasBeanInfo(Class clazz) {
		return (clazz.getModifiers() & Modifier.ABSTRACT) == 0
				&& (clazz.getModifiers() & Modifier.PUBLIC) > 0
				&& !clazz.isInterface() && !clazz.isEnum()
				&& getAnnotation(clazz,
						cc.alcina.framework.common.client.logic.reflection.Bean.class) != null;
	}

	@Override
	protected void initialiseNewInstance(Class clazz) {
		// could log, i guess
	}

	@Override
	protected <T> T newInstance0(Class<T> clazz, long objectId, long localId) {
		return null;
	}

	public static class IntrospectionException extends RuntimeException {
		public IntrospectionException(String message, Class clazz) {
			super(CommonUtils.highlightForLog("reason: %s\nclass:%s", message,
					clazz));
		}
	}

	private static class IntrospectionCheckResult {
		boolean canIntrospect;

		IntrospectionException exception;
	}
}