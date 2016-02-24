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

/**
 * 
 * @author Nick Reddel
 */
public interface PropertyAccessor {
	public void setPropertyValue(Object bean, String propertyName,
			Object value);

	public Object getPropertyValue(Object bean, String propertyName);

	public IndividualPropertyAccessor cachedAccessor(Class clazz,
			String propertyName);

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName);

	public Class getPropertyType(Class objectClass, String propertyName);

	public interface IndividualPropertyAccessor {
		public Object getPropertyValue(Object value);

		public void setPropertyValue(Object bean, Object value);

		Class getPropertyType(Object bean);
	}
}
