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
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.totsp.gwittir.client.beans.Method;

/**
 * 
 * @author Nick Reddel
 */
public interface ClassLookup {
	public Class getClassForName(String fqn);

	public <T> T newInstance(Class<T> clazz);

	public <T> T newInstance(Class<T> clazz, long objectId, long localId);

	public String displayNameForObject(Object o);

	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass);

	public List<String> getAnnotatedPropertyNames(Class clazz);

	public Class getPropertyType(Class clazz, String propertyName);

	public <T> T getTemplateInstance(Class<T> clazz);

	public List<PropertyInfoLite> getWritableProperties(Class clazz);

	public static class PropertyInfoLite {
		private Class propertyType;

		private final String propertyName;

		private Method readMethod;

		private boolean serializeCollectionOnClient;

		private final Class beanType;

		public PropertyInfoLite(Class beanType,String propertyName) {
			this.propertyName = propertyName;
			this.beanType = beanType;
		}

		public PropertyInfoLite(Class propertyType, String propertyName,
				Method readMethod, Class beanType) {
			this.readMethod = readMethod;
			this.beanType = beanType;
			this.propertyType = CommonUtils.getWrapperType(propertyType);
			this.propertyName = propertyName;
			DomainPropertyInfo ann = CommonLocator
					.get()
					.propertyAccessor()
					.getAnnotationForProperty(beanType,
							DomainPropertyInfo.class, propertyName);
			serializeCollectionOnClient = ann != null
					&& ann.serializeOnClient();
		}

		public Class getPropertyType() {
			return propertyType;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public Method getReadMethod() {
			return readMethod;
		}

		public boolean isSerializableCollection() {
			return serializeCollectionOnClient;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PropertyInfoLite) {
				PropertyInfoLite o = (PropertyInfoLite) obj;
				return o.beanType.equals(beanType)
						&& o.propertyName.equals(getPropertyName());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return beanType.hashCode() ^ propertyName.hashCode();
		}
	}
}
