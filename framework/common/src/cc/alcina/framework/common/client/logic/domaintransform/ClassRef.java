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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

@MappedSuperclass
/**
 * @author Nick Reddel
 */

@Registration(ClearStaticFieldsOnAppShutdown.class)
public abstract class ClassRef extends Entity implements TreeSerializable {
	private static Map<String, ClassRef> refMap = new HashMap<String, ClassRef>();

	private static Map<Long, ClassRef> idMap = new HashMap<Long, ClassRef>();

	public static void add(Collection<? extends ClassRef> refs) {
		for (ClassRef classRef : refs) {
			refMap.put(classRef.getRefClassName(), classRef);
			idMap.put(classRef.getId(), classRef);
		}
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
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

	@Transient
	@XmlTransient
	@AlcinaTransient
	public Class getRefClass() {
		if (this.refClass == null && this.refClassName != null) {
			try {
				this.refClass = Reflections.forName(this.refClassName);
			} catch (Exception e) {
				// Ax.simpleExceptionOut(e);
			}
		}
		return this.refClass;
	}

	public String getRefClassName() {
		return refClassName;
	}

	@Override
	public int hashCode() {
		return refClassName == null ? 0 : refClassName.hashCode();
	}

	public boolean notInVm() {
		if (this.refClass == null && this.refClassName != null) {
			try {
				this.refClass = Reflections.forName(this.refClassName);
			} catch (Exception e) {
				return true;
			}
		}
		return false;
	}

	public boolean provideExists() {
		return getRefClass() != null;
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

	@Override
	public String toString() {
		return Ax.format("Classref - id: %s className: %s", getId(),
				getRefClassName());
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
