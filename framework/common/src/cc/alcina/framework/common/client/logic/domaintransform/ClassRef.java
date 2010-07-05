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

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.totsp.gwittir.client.ui.Renderer;


@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */

 public abstract class ClassRef implements Serializable,HasIdAndLocalId {
	private static Map<String, ClassRef> refMap = new HashMap<String, ClassRef>();

	public static ClassRef forClass(Class clazz) {
		return forName(clazz.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ClassRef)) {
			return false;
		}
		return getRefClass() != null
				&& getRefClass().equals(((ClassRef) obj).getRefClass());
	}

	@Override
	public int hashCode() {
		return getRefClass() != null ? getRefClass().hashCode() : super
				.hashCode();
	}

	public static ClassRef forName(String className) {
		if (refMap.containsKey(className)) {
			return refMap.get(className);
		}
		return null;
	}
	public static void remove(ClassRef ref){
		refMap.remove(ref.getRefClassName());
	}
	public static void add(Collection<? extends ClassRef> refs) {
		for (ClassRef classRef : refs) {
			refMap.put(classRef.getRefClassName(), classRef);
		}
	}
	public static Set<ClassRef> all(){
		return new LinkedHashSet<ClassRef>(refMap.values());
	}
	private String refClassName;

	private transient Class refClass;
	@Transient
	public abstract long getId();

	@Transient
	@XmlTransient
	public Class getRefClass() {
		if (this.refClass == null && this.refClassName != null) {
			try {
				this.refClass = CommonLocator.get().classLookup()
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

	public abstract void setId(long id);

	public void setRefClass(Class refClass) {
		this.refClass = refClass;
		this.refClassName = (refClass == null) ? null : this.refClass.getName();//.replace('$', '.');
	}

	public void setRefClassName(String refClassName) {
		this.refClassName = refClassName;
	}
	@Transient
	/**
	 * Here for HasIdAndLocalId compatibility, but always 0 since always server-generated
	 */
	public long getLocalId(){
		return 0;
	}

	public void setLocalId(long localId){
		//noop. 
	}
	public static class ClassRefSimpleNameRenderer implements Renderer<ClassRef,String>{
public static final ClassRefSimpleNameRenderer INSTANCE = new ClassRefSimpleNameRenderer();
		public String render(ClassRef o) {
			// TODO Auto-generated method stub
			return o==null?"(undefined)":CommonUtils.simpleClassName(o.getRefClass());
		}
		
	}

}
