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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.registry.ClassDataCache.ClassDataItem;
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

	public void scan(ClassDataCache classDataCache, Collection<String> ignore,
			Registry toRegistry, String registryName) throws Exception {
		String cachePath = CommonUtils.formatJ("%s/%s-registry-cache.ser",
				getHomeDir().getPath(), registryName);
		this.toRegistry = toRegistry;
		scan(classDataCache, cachePath);
	}

	protected Class maybeNormaliseClass(Class c) {
		return c;
	}

	@Override
	protected void process(Class c, String className, 
			ClassDataItem foundItem, ClassDataCache outgoing) {
		if (!Modifier.isPublic(c.getModifiers())
				|| Modifier.isAbstract(c.getModifiers()) || c.isInterface()) {
			outgoing.add(foundItem);
			return;
		}
		c = maybeNormaliseClass(c);
		//GWT, for instance, will replace a JavaScriptObject class with the synthetic interface
		if (!Modifier.isPublic(c.getModifiers())
				|| Modifier.isAbstract(c.getModifiers()) || c.isInterface()) {
			outgoing.add(foundItem);
			return;
		}
		Multimap<Class,List<Annotation>> sca=
		 AnnotationUtils
				.getSuperclassAnnotations(c);
		 AnnotationUtils.filterAnnotations(sca,
				RegistryLocation.class, RegistryLocations.class);
		Set<RegistryLocation> uniques = Registry
				.filterForRegistryPointUniqueness(sca);
		if (uniques.isEmpty()) {
			outgoing.add(foundItem);
			return;
		}
		for (RegistryLocation rl : uniques) {
			toRegistry.register(c, rl);
		}
	}
}
