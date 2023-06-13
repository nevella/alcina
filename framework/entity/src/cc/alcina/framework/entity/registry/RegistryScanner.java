/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.registry;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.registry.RegistryScanner.RegistryScannerMetadata;

/*
 * Considered, as per seam etc, use javaassist to avoid loading every class in
 * the app here... But the caching idea works better, because we need to check
 * method annotations etc...and simpler
 */
/**
 *
 *
 * @author Nick Reddel
 */
public class RegistryScanner extends CachingScanner<RegistryScannerMetadata> {
	public boolean commitAfterScan = true;

	/**
	 * Load cached registry data directly (when generated at build- rather than
	 * run-time)
	 */
	public void load(ClassMetadataCache<?> classDataCache) {
		outgoingCache = (ClassMetadataCache<RegistryScannerMetadata>) classDataCache;
		commit();
	}

	public void logRegistrations() {
		for (RegistryScannerMetadata metadata : outgoingCache.classData
				.values()) {
			if (!metadata.invalid) {
				for (RegistryScannerLazyRegistration registration : metadata.registrations) {
					Ax.out("%s - %s - %s - %s",
							registration.registeringClassClassName,
							registration.keys, registration.implementation,
							registration.priority);
				}
			}
		}
	}

	public void scan(ClassMetadataCache<ClassMetadata> classDataCache,
			String ignoreClassnameRegex, String registryName) throws Exception {
		String cachePath = Ax.format("%s/%s-registry-cache.ser",
				getHomeDir().getPath(), registryName);
		if (!Ax.isTest() && !Boolean.getBoolean("RegistryScanner.useCache")
				&& !GWT.isClient()) {
			new File(cachePath).delete();
		}
		this.ignoreClassnameRegex = ignoreClassnameRegex;
		scan(classDataCache, cachePath);
		if (commitAfterScan) {
			commit();
		}
	}

	private void commit() {
		for (RegistryScannerMetadata metadata : outgoingCache.classData
				.values()) {
			if (!metadata.invalid) {
				for (RegistryScannerLazyRegistration registration : metadata.registrations) {
					Registry.register().add(
							registration.registeringClassClassName,
							registration.keys, registration.implementation,
							registration.priority);
				}
			}
		}
	}

	@Override
	protected RegistryScannerMetadata createMetadata(String className,
			ClassMetadata found) {
		return new RegistryScannerMetadata(className).fromUrl(found);
	}

	protected Class maybeNormaliseClass(Class c) {
		return c;
	}

	@Override
	protected RegistryScannerMetadata process(Class clazz, String className,
			ClassMetadata found) {
		RegistryScannerMetadata out = createMetadata(className, found);
		clazz = maybeNormaliseClass(clazz);
		/*
		 * No need for abstract classes/interfaces because, although
		 * Registration annotations on those are definitely valid, they're
		 * supplied to client code via resolution of concrete classes.
		 * 
		 * Non-public ... tricky choice with beans 1x5 (and movement away from
		 * 'public') . But given any registered classes must have accessible
		 * public methods - except in the rare case that everything registered
		 * (and the consumer) is in the package - sticking with public
		 */
		if (!Modifier.isPublic(clazz.getModifiers())
				|| Modifier.isAbstract(clazz.getModifiers())
				|| clazz.isInterface()) {
		} else {
			List<Registration> registrations = Annotations
					.resolveMultiple(clazz, Registration.class);
			for (Registration registration : registrations) {
				out.register(clazz, registration);
			}
		}
		return out;
	}

	public static class RegistryScannerLazyRegistration
			implements Serializable {
		private static final long serialVersionUID = 1L;

		String registeringClassClassName;

		List<String> keys;

		Registration.Implementation implementation;

		Registration.Priority priority;

		public RegistryScannerLazyRegistration() {
		}

		public RegistryScannerLazyRegistration(Class clazz,
				Registration registration) {
			registeringClassClassName = clazz.getName();
			keys = Arrays.stream(registration.value()).map(Class::getName)
					.collect(Collectors.toList());
			implementation = registration.implementation();
			priority = registration.priority();
		}
	}

	public static class RegistryScannerMetadata
			extends ClassMetadata<RegistryScannerMetadata> {
		List<RegistryScannerLazyRegistration> registrations = new ArrayList<>();

		public RegistryScannerMetadata() {
		}

		public RegistryScannerMetadata(String className) {
			super(className);
		}

		public void register(Class clazz, Registration registration) {
			registrations.add(
					new RegistryScannerLazyRegistration(clazz, registration));
		}
	}
}
