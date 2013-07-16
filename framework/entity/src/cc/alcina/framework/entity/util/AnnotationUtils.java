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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

	private static HashMap<Class, Set<Annotation>> superAnnotationMap = new HashMap<Class, Set<Annotation>>();

	

	

	public static Set<Annotation> getSuperclassAnnotations(Class clazz) {
		if (superAnnotationMap.containsKey(clazz)) {
			return superAnnotationMap.get(clazz);
		}
		Map<Class, Annotation> uniqueMap = new HashMap<Class, Annotation>();
		while (clazz != Object.class) {
			try {
				for (Annotation a : clazz.getAnnotations()) {
					if (!uniqueMap.containsKey(a.annotationType())) {
						uniqueMap.put(a.annotationType(),
								a);
					}
				}
			} catch (Exception e) {
				break;
			}
			clazz = clazz.getSuperclass();
		}
		HashSet values = new HashSet(uniqueMap.values());
		superAnnotationMap.put(clazz, values);
		return values;
	}

	public static <A extends Annotation> Set<A> filterAnnotations(
			Set<Annotation> ann, Class<? extends A> filterClass) {
		Set<A> result = new LinkedHashSet<A>();
		for (Annotation a : ann) {
			if (a.annotationType() == filterClass) {
				result.add((A) a);
			}
		}
		return result;
	}
	
}
