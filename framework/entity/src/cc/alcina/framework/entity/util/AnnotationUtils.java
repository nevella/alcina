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
package cc.alcina.framework.entity.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class AnnotationUtils {
	@ClearOnAppRestart
	private static HashMap<Method, Set<Annotation>> superMethodAnnotationMap = new HashMap<Method, Set<Annotation>>();

	public static Set<Annotation> getSuperclassAnnotationsForMethod(Method m) {
		if (superMethodAnnotationMap.containsKey(m)) {
			return superMethodAnnotationMap.get(m);
		}
		Map<Class, Annotation> uniqueMap = new HashMap<Class, Annotation>();
		Class c = m.getDeclaringClass();
		while (c != Object.class) {
			try {
				Method m2 = c.getMethod(m.getName(), m.getParameterTypes());
				for (Annotation a : m2.getAnnotations()) {
					if (!uniqueMap.containsKey(a.annotationType())) {
						uniqueMap.put(a.annotationType(), a);
					}
				}
			} catch (Exception e) {
			}
			c = c.getSuperclass();
		}
		HashSet values = new HashSet(uniqueMap.values());
		superMethodAnnotationMap.put(m, values);
		return values;
	}

	private static HashMap<Class, Collection<Annotation>> superAnnotationMap = new HashMap<Class, Collection<Annotation>>();

	public static Collection<Annotation> getSuperclassAnnotations(Class clazz) {
		Class forClass = clazz;
		if (clazz.isInterface()) {
			throw new RuntimeException(
					"Should only check for classes, not interfaces");
		}
		if (superAnnotationMap.containsKey(clazz)) {
			return superAnnotationMap.get(clazz);
		}
		List<Annotation> values = new ArrayList<Annotation>();
		while (clazz != Object.class) {
			for (Annotation a : clazz.getAnnotations()) {
				values.add(a);
			}
			clazz = clazz.getSuperclass();
		}
		superAnnotationMap.put(forClass, values);
		return values;
	}

	public static <A extends Annotation> Collection<A> filterAnnotations(
			Collection<Annotation> ann, Class<? extends A>... filterClasses) {
		Set<A> result = new LinkedHashSet<A>();
		List<Class<? extends A>> filterList = Arrays.asList(filterClasses);
		for (Annotation a : ann) {
			if (filterList.contains(a.annotationType())) {
				result.add((A) a);
			}
		}
		return result;
	}
}
