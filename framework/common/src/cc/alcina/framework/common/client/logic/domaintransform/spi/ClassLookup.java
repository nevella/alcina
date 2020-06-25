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

import com.totsp.gwittir.client.beans.Method;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public interface ClassLookup {
	public String displayNameForObject(Object o);

	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass);

	public Class getClassForName(String fqn);

	public List<PropertyReflector> getPropertyReflectors(Class<?> beanClass);

	public Class getPropertyType(Class clazz, String propertyName);

	public <T> T getTemplateInstance(Class<T> clazz);

	public List<PropertyInfo> getWritableProperties(Class clazz);

	public <T> T newInstance(Class<T> clazz);

	public <T> T newInstance(Class<T> clazz, long objectId, long localId);

	default boolean handlesClass(Class clazz) {
		return true;
	}

	default boolean isPrimitive(Class<?> clazz) {
		return clazz.isPrimitive();
	}

	default <T> T newInstance(String fqn) {
		return (T) newInstance(getClassForName(fqn));
	}

	public static class PropertyInfo {
		private Class propertyType;

		private final String propertyName;

		private Method readMethod;

		private boolean serializeCollectionOnClient;

		private final Class beanType;

		private String serializeWithBeanSerialization;

		private Method writeMethod;

		public PropertyInfo(Class beanType, String propertyName) {
			this.propertyName = propertyName;
			this.beanType = beanType;
		}

		public PropertyInfo(Class propertyType, String propertyName,
				Method readMethod, Method writeMethod, Class beanType) {
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
			this.beanType = beanType;
			this.propertyType = CommonUtils.getWrapperType(propertyType);
			this.propertyName = propertyName;
			DomainProperty ann = Reflections.propertyAccessor()
					.getAnnotationForProperty(beanType, DomainProperty.class,
							propertyName);
			if (ann != null) {
				serializeCollectionOnClient = ann.serializeOnClient();
				serializeWithBeanSerialization = ann
						.serializeWithBeanSerialization();
			}
		}

		public void copy(Entity entity, Entity writeable) {
			try {
				Object value = get(entity);
				set(writeable, value);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PropertyInfo) {
				PropertyInfo o = (PropertyInfo) obj;
				return o.beanType.equals(beanType)
						&& o.propertyName.equals(getPropertyName());
			}
			return false;
		}

		public Object get(Entity entity) {
			try {
				return getReadMethod().invoke(entity, new Object[0]);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public String getPropertyName() {
			return propertyName;
		}

		public Class getPropertyType() {
			return propertyType;
		}

		public Method getReadMethod() {
			return readMethod;
		}

		public String getSerializeWithBeanSerialization() {
			return this.serializeWithBeanSerialization;
		}

		@Override
		public int hashCode() {
			return beanType.hashCode() ^ propertyName.hashCode();
		}

		public boolean hasSerializeWithBeanSerialization() {
			return Ax.notBlank(this.serializeWithBeanSerialization);
		}

		public boolean isSerializableCollection() {
			return serializeCollectionOnClient;
		}

		public void set(Entity writeable, Object value) {
			try {
				writeMethod.invoke(writeable, new Object[] { value });
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public void setSerializeWithBeanSerialization(
				String serializeWithBeanSerialization) {
			this.serializeWithBeanSerialization = serializeWithBeanSerialization;
		}

		@Override
		public String toString() {
			return Ax.format("Property: %s.%s :: %s", beanType.getSimpleName(),
					propertyName, propertyType.getSimpleName());
		}
	}
}
