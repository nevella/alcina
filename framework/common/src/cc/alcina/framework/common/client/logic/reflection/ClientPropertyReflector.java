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
import java.util.Map;
import java.util.function.Function;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap.UnsortedMapCreator;

/**
 * 
 * @author Nick Reddel
 * 
 * FIXME - dirndl.1 - remove most API except annotations (and minimise non-resolved calls to annotations in client code) 
 */
public class ClientPropertyReflector
		implements Comparable<ClientPropertyReflector>, PropertyReflector {
	public static final String CONTEXT_NAME_TRANSLATOR = ClientPropertyReflector.class
			.getName() + ".CONTEXT_NAME_TRANSLATOR";

	private static DelegateMapCreator annotationLookupCreator = new UnsortedMapCreator();

	public static void
			setDelegateCreator(DelegateMapCreator annotationLookupCreator) {
		ClientPropertyReflector.annotationLookupCreator = annotationLookupCreator;
	}

	private final Map<Class, Object> annotations;

	private final String propertyName;

	private Class propertyType;

	private Class definingType;

	@Override
	public Class getDefiningType() {
		return this.definingType;
	}

	public ClientPropertyReflector(Class definingType, String propertyName,
			Class propertyType, Annotation[] anns) {
		this.definingType = definingType;
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.annotations = annotationLookupCreator.createDelegateMap(0, 0);
		for (Annotation a : anns) {
			annotations.put(a.annotationType(), a);
		}
	}

	@Override
	public int compareTo(ClientPropertyReflector o) {
		if (getOrderingHint() != o.getOrderingHint()) {
			return (getOrderingHint() < o.getOrderingHint()) ? -1 : 1;
		}
		return getDisplayName().compareToIgnoreCase(o.getDisplayName());
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return (A) annotations.get(annotationClass);
	}

	public CollectionFilter getCollectionFilter() {
		if (getDisplayInfo() == null || getDisplayInfo() == null) {
			return null;
		}
		Display displayInfo = getDisplayInfo();
		Class clazz = displayInfo.filterClass();
		return (CollectionFilter) (clazz == null || clazz == Void.class ? null
				: Reflections.classLookup().newInstance(clazz));
	}

	public Display getDisplayInfo() {
		return (Display) annotations.get(Display.class);
	}

	public String getDisplayName() {
		String rawName = getDisplayInfo() == null ? getPropertyName()
				: getDisplayInfo().name();
		if (LooseContext.has(CONTEXT_NAME_TRANSLATOR)) {
			rawName = ((Function<String, String>) LooseContext
					.get(CONTEXT_NAME_TRANSLATOR)).apply(getPropertyName());
		}
		return rawName;
	}

	public int getOrderingHint() {
		return (getDisplayInfo() == null) ? 1000
				: getDisplayInfo().orderingHint();
	}

	@Override
	public String getPropertyName() {
		return this.propertyName;
	}

	@Override
	public Class getPropertyType() {
		return propertyType;
	}

	@Override
	public boolean isReadOnly() {
		return Reflections.propertyAccessor().isReadOnly(getDefiningType(),
				getPropertyName());
	}

	@Override
	public Object getPropertyValue(Object bean) {
		PropertyAccessor propertyAccessor = Reflections.propertyAccessor();
		return propertyAccessor.getPropertyValue(bean, getPropertyName());
	}

	@Override
	public void setPropertyValue(Object bean, Object newValue) {
		Reflections.propertyAccessor().setPropertyValue(bean, getPropertyName(),
				newValue);
	}

	@Override
	public String toString() {
		return Ax.format("(%s) %s", propertyType.getSimpleName(), propertyName);
	}
}
