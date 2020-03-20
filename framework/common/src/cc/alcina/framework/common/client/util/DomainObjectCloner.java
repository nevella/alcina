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
package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;


/**
 *
 * @author Nick Reddel
 */
public class DomainObjectCloner extends CloneHelper {
	public static final Set<String> IGNORE_FOR_DOMAIN_OBJECT_CLONING = new HashSet<String>(
			Arrays.asList(new String[] { "id", "localId",
					"lastModificationDate", "lastModificationUser",
					"creationDate", "creationUser", "versionNumber",
					"propertyChangeListeners" }));

	private List provisionalObjects = new ArrayList();

	private boolean createProvisionalObjects;

	public List getProvisionalObjects() {
		return this.provisionalObjects;
	}

	public boolean isCreateProvisionalObjects() {
		return this.createProvisionalObjects;
	}

	public void setCreateProvisionalObjects(boolean createProvisionalObjects) {
		this.createProvisionalObjects = createProvisionalObjects;
	}

	@Override
	protected boolean deepProperty(Object o, String propertyName) {
		Class c = o.getClass();
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(c);
		if (bi == null) {
			return false;
		}
		ClientPropertyReflector pr = bi.getPropertyReflectors()
				.get(propertyName);
		if (pr == null) {
			return false;
		}
		DomainProperty dpi = pr.getAnnotation(DomainProperty.class);
		return dpi == null ? false : dpi.cloneForDuplication();
	}

	protected boolean ignore(Class clazz, String propertyName, Object obj) {
		if (IGNORE_FOR_DOMAIN_OBJECT_CLONING.contains(propertyName)) {
			return true;
		}
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(clazz);
		if (bi == null) {
			return true;
		}
		ClientPropertyReflector pr = bi.getPropertyReflectors()
				.get(propertyName);
		if (pr == null) {
			return true;
		}
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		PropertyPermissions pp = pr.getAnnotation(PropertyPermissions.class);
		return !PermissionsManager.get().checkEffectivePropertyPermission(op,
				pp, obj, false);
	}

	protected <T> T newInstance(T o) {
		Class clazz = o.getClass();
		if (o instanceof Entity) {
			if (createProvisionalObjects) {
				Entity obj = TransformManager.get()
						.createProvisionalObject(clazz);
				provisionalObjects.add(obj);
				return (T) obj;
			} else {
				Entity obj = TransformManager.get()
						.createDomainObject(clazz);
				return (T) obj;
			}
		} else {
			return super.newInstance(o);
		}
	}
}
