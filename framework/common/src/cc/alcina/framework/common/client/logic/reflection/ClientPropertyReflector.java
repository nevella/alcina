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
package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.collections.CollectionFilter;

/**
 * 
 * @author Nick Reddel
 */
public class ClientPropertyReflector implements
		Comparable<ClientPropertyReflector> {
	private final Map<Class, Object> annotations;

	private final String propertyName;

	private Class propertyType;

	public String getPropertyName() {
		return this.propertyName;
	}

	public ClientPropertyReflector(String propertyName, Class propertyType,
			Annotation[] anns) {
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.annotations = new HashMap<Class, Object>();
		for (Annotation a : anns) {
			annotations.put(a.annotationType(), a);
		}
	}

	public VisualiserInfo getGwPropertyInfo() {
		return (VisualiserInfo) annotations.get(VisualiserInfo.class);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return (A) annotations.get(annotationClass);
	}

	public String getDisplayName() {
		return getGwPropertyInfo() == null ? getPropertyName()
				: getGwPropertyInfo().displayInfo().name();
	}

	public CollectionFilter getCollectionFilter() {
		if (getGwPropertyInfo() == null
				|| getGwPropertyInfo().displayInfo() == null) {
			return null;
		}
		DisplayInfo displayInfo = getGwPropertyInfo().displayInfo();
		Class clazz = displayInfo.filterClass();
		return (CollectionFilter) (clazz == null||clazz==Void.class ? null : CommonLocator.get().classLookup()
				.newInstance(clazz));
	}

	public int getOrderingHint() {
		return (getGwPropertyInfo() == null) ? 1000 : getGwPropertyInfo()
				.displayInfo().orderingHint();
	}

	public int compareTo(ClientPropertyReflector o) {
		if (getOrderingHint() != o.getOrderingHint()) {
			return (getOrderingHint() < o.getOrderingHint()) ? -1 : 1;
		}
		return getDisplayName().compareToIgnoreCase(o.getDisplayName());
	}

	public void setPropertyType(Class propertyType) {
		this.propertyType = propertyType;
	}

	public Class getPropertyType() {
		return propertyType;
	}

	public Object getPropertyValue(Object bean) {
		return CommonLocator.get().propertyAccessor()
				.getPropertyValue(bean, getPropertyName());
	}

	public void setPropertyValue(Object bean, Object newValue) {
		CommonLocator.get().propertyAccessor()
				.setPropertyValue(bean, getPropertyName(), newValue);
	}
}
