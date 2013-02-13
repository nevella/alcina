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
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
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
public class RegistryScanner extends CachingScanner {
	private Registry toRegistry;

	public void scan(Collection<String> classNames, Collection<String> ignore,
			Registry toRegistry) throws Exception {
		Map<String, Date> classes = new HashMap<String, Date>();
		for (String clazzName : classNames) {
			classes.put(clazzName, null);
		}
		scan(classes, ignore, toRegistry);
	}

	public void scan(Map<String, Date> classes, Collection<String> ignore,
			Registry toRegistry) throws Exception {
		String cachePath = getHomeDir().getPath() + File.separator
				+ toRegistry.getClass().getSimpleName() + "-cache.ser";
		this.toRegistry = toRegistry;
		scan(classes, cachePath);
	}

	@Override
	protected void process(Class c, String className, Date modDate,
			Map<String, Date> outgoingIgnoreMap) {
		if ((!Modifier.isPublic(c.getModifiers()))
				|| (Modifier.isAbstract(c.getModifiers()) && !c.isInterface())) {
			outgoingIgnoreMap.put(className, modDate);
			return;
		}
		if(c.getName().contains("ServletLayerUpdater")){
			int j=3;
		}
		c = maybeNormaliseClass(c);
		{
			RegistryLocations rls = (RegistryLocations) c
					.getAnnotation(RegistryLocations.class);
			RegistryLocation rl = (RegistryLocation) c
					.getAnnotation(RegistryLocation.class);
			if (rl == null && rls == null) {
				outgoingIgnoreMap.put(className, modDate);
				return;
			}
		}
		Set<Annotation> sca = AnnotationUtils.getSuperclassAnnotations(c);
		Set<RegistryLocation> rls = AnnotationUtils.filterAnnotations(sca,
				RegistryLocation.class);
		Set<RegistryLocations> rlsSet = AnnotationUtils.filterAnnotations(sca,
				RegistryLocations.class);
		for (RegistryLocations rlcs : rlsSet) {
			for (RegistryLocation rl : rlcs.value()) {
				rls.add(rl);
			}
		}
		for (RegistryLocation rl : rls) {
			toRegistry.register(c, rl);
		}
	}

	protected Class maybeNormaliseClass(Class c) {
		return c;
	}
}
