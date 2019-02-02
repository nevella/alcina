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
package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public abstract class ClassRef implements Serializable, HasIdAndLocalId {
	private static Map<String, ClassRef> refMap = new HashMap<String, ClassRef>();

	private static Map<Long, ClassRef> idMap = new HashMap<Long, ClassRef>();

	public static void add(Collection<? extends ClassRef> refs) {
		for (ClassRef classRef : refs) {
			refMap.put(classRef.getRefClassName(), classRef);
			idMap.put(classRef.getId(), classRef);
		}
	}

	public static Set<ClassRef> all() {
		return new LinkedHashSet<ClassRef>(refMap.values());
	}

	public static ClassRef forClass(Class clazz) {
		return forName(clazz.getName());
	}

	public static ClassRef forId(long id) {
		return idMap.get(id);
	}

	public static ClassRef forName(String className) {
		return refMap.get(className);
	}

	public static void remove(ClassRef ref) {
		refMap.remove(ref.getRefClassName());
		idMap.remove(ref.getId());
	}

	private String refClassName;

	private transient Class refClass;

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ClassRef)) {
			return false;
		}
		return getRefClass() != null
				&& getRefClass().equals(((ClassRef) obj).getRefClass());
	}

	@Override
	@Transient
	public abstract long getId();

	@Override
	@Transient
	/**
	 * Here for HasIdAndLocalId compatibility, but always 0 since always
	 * server-generated
	 */
	public long getLocalId() {
		return 0;
	}

	@Transient
	@XmlTransient
	public Class getRefClass() {
		if (this.refClass == null && this.refClassName != null) {
			try {
				this.refClass = Reflections.classLookup()
						.getClassForName(this.refClassName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.refClass;
	}

	public String getRefClassName() {
		return refClassName;
	}

	@Override
	public int hashCode() {
		return refClassName.hashCode();
	}

	public boolean notInVm() {
		if (this.refClass == null && this.refClassName != null) {
			try {
				this.refClass = Reflections.classLookup()
						.getClassForName(this.refClassName);
			} catch (Exception e) {
				return true;
			}
		}
		return false;
	}

	@Override
	public abstract void setId(long id);

	@Override
	public void setLocalId(long localId) {
		// noop.
	}

	public void setRefClass(Class refClass) {
		this.refClass = refClass;
		this.refClassName = (refClass == null) ? null : this.refClass.getName();
		// .replace('$',
		// '.');
	}

	public void setRefClassName(String refClassName) {
		this.refClassName = refClassName;
	}

	public static class ClassRefSimpleNameRenderer
			implements Renderer<ClassRef, String> {
		public static final ClassRefSimpleNameRenderer INSTANCE = new ClassRefSimpleNameRenderer();

		@Override
		public String render(ClassRef o) {
			return o == null ? "(undefined)"
					: CommonUtils.simpleClassName(o.getRefClass());
		}
	}
}
