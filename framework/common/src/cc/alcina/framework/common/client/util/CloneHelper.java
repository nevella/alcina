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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;

import com.totsp.gwittir.client.beans.Property;

/**
 * Not thread-safe - but then again, should only be used by one thread
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public class CloneHelper {
	private Map createdMap = new IdentityHashMap();

	public <T extends Collection> T deepCollectionClone(T coll)
			throws Exception {
		T c = null;
		if (coll instanceof ArrayList) {
			c = (T) new ArrayList();
		} else if (coll instanceof LinkedHashSet) {
			c = (T) new LinkedHashSet();
		} else if (coll instanceof HashSet) {
			c = (T) new HashSet();
		}
		for (Object o2 : coll) {
			c.add(deepObjectClone(o2));
		}
		return c;
	}

	private Object deepObjectClone(Object o) throws Exception {
		if (o instanceof Date) {
			return ((Date) o).clone();
		} else if (CommonUtils.isStandardJavaClass(o.getClass())
				|| o.getClass().isEnum()) {
			return o;
		} else if (o instanceof Collection) {
			return deepCollectionClone((Collection) o);
		} else if (GwittirUtils.isIntrospectable(o.getClass())) {
			return deepBeanClone(o);
		} else {
			return o;
		}
	}

	public <T> T deepBeanClone(T o) throws Exception {
		if (createdMap.containsKey(o)) {
			return (T) createdMap.get(o);
		}
		T ret = newInstance(o);
		createdMap.put(o, ret);
		Property[] prs = GwittirBridge.get().getDescriptor(ret).getProperties();
		for (Property pr : prs) {
			if (pr.getMutatorMethod() == null) {
				continue;
			}
			Object[] args = new Object[1];
			Object val = pr.getAccessorMethod().invoke(o,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			if (createdMap.containsKey(val)) {
				val = createdMap.get(val);
			}
			if (val != null) {
				if (!ignore(o.getClass(), pr.getName(), o)) {
					args[0] = deepProperty(o, pr.getName()) ? deepObjectClone(val)
							: shallowishObjectClone(val);
					pr.getMutatorMethod().invoke(ret, args);
				}
			}
		}
		return ret;
	}

	protected boolean ignore(Class clazz, String name, Object obj) {
		return false;
	}

	protected <T> T newInstance(T o) {
		return (T) ClientReflector.get().newInstance(o.getClass(), 0);
	}

	protected boolean deepProperty(Object o, String propertyName) {
		return true;
	}

	protected Object shallowishObjectClone(Object o) {
		if (o instanceof Collection) {
			return CommonUtils.shallowCollectionClone((Collection) o);
		}
		return o;
	}

	// optimisation, part. for GWT
	private Object[] args = new Object[1];

	/*
	 * note, there won't be any property change listeners on the cloned object,
	 * so invoking the mutator won't cause args to be reused (i.e. this to be
	 * called)
	 */
	public void copyBeanProperties(Object source, Object target)
			throws Exception {
		Property[] prs = GwittirBridge.get().getDescriptor(target)
				.getProperties();
		for (Property pr : prs) {
			if (pr.getMutatorMethod() == null) {
				continue;
			}
			Object val = pr.getAccessorMethod().invoke(source,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			if (val != null) {
				if (val instanceof Collection) {
					val = CommonUtils.shallowCollectionClone((Collection) val);
				}
				args[0] = val;
				pr.getMutatorMethod().invoke(target, args);
			}
		}
	}

	public <T> T shallowishBeanClone(T o) {
		try {
			T ret = newInstance(o);
			copyBeanProperties(o, ret);
			return ret;
		} catch (Exception e) {
			throw new WrappedRuntimeException("Unable to clone: "
					+ o.getClass(), e, SuggestedAction.NOTIFY_WARNING);
		}
	}
}
