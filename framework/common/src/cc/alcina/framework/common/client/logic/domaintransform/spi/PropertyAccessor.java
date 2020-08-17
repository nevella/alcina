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
package cc.alcina.framework.common.client.logic.domaintransform.spi;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;

/**
 * 
 * @author Nick Reddel
 */
public interface PropertyAccessor {
	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName);

	public PropertyReflector getPropertyReflector(Class clazz,
			String propertyName);

	public Class getPropertyType(Class objectClass, String propertyName);

	public Object getPropertyValue(Object bean, String propertyName);

	public void setPropertyValue(Object bean, String propertyName,
			Object value);

	default boolean hasPropertyKey(Object left, String leftName) {
		return true;
	}

	public boolean isReadOnly(Class objectClass, String propertyName);
}
