package cc.alcina.framework.common.client.reflection.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;

//poulates ForName, Reflections
public class ModuleReflectorJvm extends ModuleReflector {
	public static final String CONTEXT_MODULE_NAME = ModuleReflectorJvm.class
			.getName() + ".CONTEXT_MODULE_NAME";

	public static final String PROP_FILTER_CLASSNAME = "ClientReflectorJvm.filterClassName";

	private Predicate<String> test;

	public ModuleReflectorJvm() {
	}

	@Override
	public void register() {
		registerRegistrations();
	}

	@Override
	protected void registerForNames(Map<String, Class> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void registerReflectorSuppliers(
			Map<Class, Supplier<ClassReflector>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void registerRegistrations() {
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
						.getDeclaredConstructor().newInstance();
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
}
