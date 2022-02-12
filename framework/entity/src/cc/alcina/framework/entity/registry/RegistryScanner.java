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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.registry.RegistryScanner.RegistryScannerMetadata;

/*
 * Considered, as per seam etc, use javaassist to avoid loading every class in the app here...
 * But the caching idea works better, because we need to check method annotations etc...and simpler
 */
/**
 *
 * @author Nick Reddel
 */
public class RegistryScanner extends CachingScanner<RegistryScannerMetadata> {
	public void scan(ClassMetadataCache<ClassMetadata> classDataCache,
			Collection<String> ignore, String registryName) throws Exception {
		String cachePath = Ax.format("%s/%s-registry-cache.ser",
				getHomeDir().getPath(), registryName);
		if (!ResourceUtilities.is("useCache") && !Ax.isTest()
				&& !Boolean.getBoolean("RegistryScanner.useCache")
				&& !GWT.isClient()) {
			new File(cachePath).delete();
		}
		scan(classDataCache, cachePath);
		commit();
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
		// GWT, for instance, will replace a JavaScriptObject class with the
		// synthetic interface
		if (!Modifier.isPublic(clazz.getModifiers())
				|| Modifier.isAbstract(clazz.getModifiers())
				|| clazz.isInterface()) {
		} else {
			// List<Registration> registrations = Annotations
			// .resolveMultiple(clazz, Registration.class);
			// Multimap<Class, List<Annotation>> superclassAnnotations =
			// AnnotationUtils
			// .getSuperclassAnnotations(clazz);
			// AnnotationUtils.filterAnnotations(superclassAnnotations,
			// RegistryLocation.class, RegistryLocations.class);
			// Set<RegistryLocation> uniques = RegistryOld
			// .filterForRegistryPointUniqueness(superclassAnnotations);
			// if (uniques.isEmpty()) {
			// } else {
			// for (Registration registration : uniques) {
			// out.register(clazz, registration);
			// }
			// }
			// use annotationlocation.resolve
			throw new UnsupportedOperationException();
		}
		return out;
	}

	public static class RegistryScannerLazyRegistration {
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
