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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.registry.RegistryScanner.RegistryScannerMetadata;
import cc.alcina.framework.entity.util.AnnotationUtils;

/*
 * Considered, as per seam etc, use javaassist to avoid loading every class in the app here...
 * But the caching idea works better, because we need to check method annotations etc...and simpler
 */
@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class RegistryScanner extends CachingScanner<RegistryScannerMetadata> {
	public static class RegistryScannerMetadata
			extends ClassMetadata<RegistryScannerMetadata> {
		public RegistryScannerMetadata() {
		}

		public RegistryScannerMetadata(String className) {
			super(className);
		}

		public void register(Class clazz, RegistryLocation rl) {
			locations.add(new RegistryScannerLazyLocation(clazz, rl));
		}

		List<RegistryScannerLazyLocation> locations = new ArrayList<>();
	}

	public static class RegistryScannerLazyLocation {
		public RegistryScannerLazyLocation() {
		}

		public RegistryScannerLazyLocation(Class clazz, RegistryLocation rl) {
			registeringClassClassName = clazz.getName();
			registryPointClassName = rl.registryPoint().getName();
			targetClassClassName = rl.targetClass().getName();
			implementationType = rl.implementationType();
			priority = rl.priority();
		}

		String registeringClassClassName;

		String registryPointClassName;

		String targetClassClassName;

		ImplementationType implementationType;

		int priority;
	}

	private Registry toRegistry;

	public void scan(ClassMetadataCache<ClassMetadata> classDataCache,
			Collection<String> ignore, Registry toRegistry, String registryName)
			throws Exception {
		String cachePath = CommonUtils.formatJ("%s/%s-registry-cache.ser",
				getHomeDir().getPath(), registryName);
		this.toRegistry = toRegistry;
		scan(classDataCache, cachePath);
		commit();
	}

	private void commit() {
		for (RegistryScannerMetadata metadata : outgoingCache.classData
				.values()) {
			if (!metadata.invalid) {
				for (RegistryScannerLazyLocation location : metadata.locations) {
					toRegistry.register(
							toRegistry.key(location.registeringClassClassName),
							toRegistry.key(location.registryPointClassName),
							toRegistry.key(location.targetClassClassName),
							location.implementationType, location.priority);
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
			Multimap<Class, List<Annotation>> sca = AnnotationUtils
					.getSuperclassAnnotations(clazz);
			AnnotationUtils.filterAnnotations(sca, RegistryLocation.class,
					RegistryLocations.class);
			Set<RegistryLocation> uniques = Registry
					.filterForRegistryPointUniqueness(sca);
			if (uniques.isEmpty()) {
			} else {
				for (RegistryLocation rl : uniques) {
					out.register(clazz, rl);
				}
			}
		}
		return out;
	}
}
