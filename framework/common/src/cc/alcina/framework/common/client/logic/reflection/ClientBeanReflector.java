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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * Loosely follows beaninfo/propertydescriptor
 * 
 * @author nick@alcina.cc
 * 
 */
public class ClientBeanReflector {
	private final Class beanClass;

	public Class getBeanClass() {
		return this.beanClass;
	}

	public List<Class<? extends PermissibleAction>> getActions(Object userObject) {
		List<Class<? extends PermissibleAction>> result = new ArrayList<Class<? extends PermissibleAction>>();
		ObjectActions actions = getGwBeanInfo().actions();
		if (actions != null) {
			for (Action action : actions.value()) {
				Class<? extends PermissibleAction> actionClass = action
						.actionClass();
				boolean noPermissionsCheck = actionClass == CreateAction.class
						|| actionClass == EditAction.class
						|| actionClass == ViewAction.class
						|| actionClass == DeleteAction.class;
				if (noPermissionsCheck
						|| PermissionsManager.get().isPermissible(userObject,
								new AnnotatedPermissible(action.permission()))) {
					result.add(actionClass);
				}
			}
		}
		return result;
	}

	public BeanInfo getGwBeanInfo() {
		return (BeanInfo) annotations.get(BeanInfo.class);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return (A) annotations.get(annotationClass);
	}

	public Map<String, ClientPropertyReflector> getPropertyReflectors() {
		return this.propertyReflectors;
	}

	private final Map<String, ClientPropertyReflector> propertyReflectors;

	private Map<Class, Object> annotations;

	public ClientBeanReflector(Class clazz, Annotation[] anns,
			Map<String, ClientPropertyReflector> propertyReflectors) {
		this.beanClass = clazz;
		this.annotations = new HashMap<Class, Object>();
		for (Annotation a : anns) {
			annotations.put(a.annotationType(), a);
		}
		this.propertyReflectors = propertyReflectors;
	}

	public String getTypeDisplayName() {
		String tn = getGwBeanInfo().displayInfo().name();
		if (CommonUtils.isNullOrEmpty(tn)) {
			tn = CommonUtils.capitaliseFirst(CommonUtils
					.classSimpleName(beanClass));
		}
		return TextProvider.get().getUiObjectText(beanClass,
				TextProvider.DISPLAY_NAME, tn);
	}

	public String getObjectName(Object o) {
		Class<? extends Object> clazz = o.getClass();
		if (clazz != beanClass) {
			throw new WrappedRuntimeException(
					CommonUtils.formatJ(
							"Object not of correct class for reflector - %s, %s",
							clazz != null ? clazz.getName() : null,
							beanClass.getName()), SuggestedAction.NOTIFY_ERROR);
		}
		return TextProvider.get().getObjectName(o, this);
	}

	public String getDisplayNamePropertyName() {
		String dnpn = getGwBeanInfo().displayNamePropertyName();
		return (dnpn == null) ? "id" : dnpn;
	}

	/**
	 * Convenience method
	 * 
	 * @param annotationClass
	 * @param callback
	 */
	public <A extends Annotation> void iterateForPropertyWithAnnotation(
			Class<A> annotationClass, HasAnnotationCallback<A> callback) {
		for (ClientPropertyReflector propertyReflector : getPropertyReflectors()
				.values()) {
			A annotation = propertyReflector.getAnnotation(annotationClass);
			if (annotation != null) {
				callback.apply(annotation, propertyReflector);
			}
		}
	}

	public static interface HasAnnotationCallback<A extends Annotation> {
		public void apply(A annotation,
				ClientPropertyReflector propertyReflector);
	}
}
