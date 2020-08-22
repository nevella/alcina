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
import java.util.Set;

import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;

/**
 * Not thread-safe - but then again, should only be used by one thread
 * 
 * @author nick@alcina.cc
 * 
 */
public class CloneHelper {
	private Map createdMap = new IdentityHashMap();

	// optimisation, part. for GWT
	private Object[] args = new Object[1];

	/*
	 * note, there won't be any property change listeners on the cloned object,
	 * so invoking the mutator won't cause args to be reused (i.e. this to be
	 * called)
	 */
	public void copyBeanProperties(Object source, Object target,
			Set<String> excludeProperties){
		try {
			Property[] prs = Reflections.beanDescriptorProvider()
					.getDescriptor(target).getProperties();
			for (Property pr : prs) {
				if (pr.getMutatorMethod() == null
						|| pr.getAccessorMethod() == null) {
					continue;
				}
				Object val = pr.getAccessorMethod().invoke(source,
						CommonUtils.EMPTY_OBJECT_ARRAY);
				if (val != null) {
					if (excludeProperties != null
							&& excludeProperties.contains(pr.getName())) {
						continue;
					}
					if (val instanceof Collection) {
						val = CommonUtils
								.shallowCollectionClone((Collection) val);
					}
					args[0] = val;
					pr.getMutatorMethod().invoke(target, args);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		
	}

	// TODO - wrap exceptions
	public <T> T deepBeanClone(T o) throws Exception {
		if (createdMap.containsKey(o)) {
			return (T) createdMap.get(o);
		}
		T ret = newInstance(o);
		createdMap.put(o, ret);
		Property[] prs = Reflections.beanDescriptorProvider().getDescriptor(ret)
				.getProperties();
		for (Property pr : prs) {
			if (pr.getMutatorMethod() == null) {
				continue;
			}
			AlcinaTransient alcinaTransient = Reflections.propertyAccessor()
					.getAnnotationForProperty(ret.getClass(),
							AlcinaTransient.class, pr.getName());
			if (alcinaTransient != null) {
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
					args[0] = deepProperty(o, pr.getName())
							? deepObjectClone(val)
							: shallowishObjectClone(val);
					pr.getMutatorMethod().invoke(ret, args);
				}
			}
		}
		DeepBeanClonePostHandler postHandler = Registry
				.impl(DeepBeanClonePostHandler.class, o.getClass(), true);
		if (postHandler != null) {
			postHandler.postClone(ret);
		}
		return ret;
	}

	public <T extends Collection> T deepCollectionClone(T coll)
			throws Exception {
		T c = null;
		if (coll instanceof ArrayList) {
			c = (T) new ArrayList();
		} else if (coll instanceof LinkedHashSet) {
			c = (T) new LinkedHashSet();
		} else if (coll instanceof HashSet) {
			c = (T) new HashSet();
		} else if (coll instanceof LiSet) {
			c = (T) new LiSet();
		} else if (coll instanceof LightSet) {
			c = (T) new LightSet();
		} else {
			throw new RuntimeException(
					"Can't clone - " + coll.getClass().getName());
		}
		for (Object o2 : coll) {
			c.add(deepObjectClone(o2));
		}
		return c;
	}

	public <T> T shallowishBeanClone(T o) {
		try {
			T ret = newInstance(o);
			copyBeanProperties(o, ret, null);
			return ret;
		} catch (Exception e) {
			throw new WrappedRuntimeException(
					"Unable to clone: " + o.getClass(), e,
					SuggestedAction.NOTIFY_WARNING);
		}
	}

	private Object deepObjectClone(Object o) throws Exception {
		if (o instanceof Date) {
			return ((Date) o).clone();
		} else if (CommonUtils.isStandardJavaClass(o.getClass())
				|| o instanceof Enum) {
			return o;
		} else if (o instanceof Collection) {
			return deepCollectionClone((Collection) o);
		} else if (GwittirUtils.isIntrospectable(o.getClass())) {
			return deepBeanClone(o);
		} else {
			return o;
		}
	}

	protected boolean deepProperty(Object o, String propertyName) {
		return true;
	}

	protected boolean ignore(Class clazz, String name, Object obj) {
		return false;
	}

	protected <T> T newInstance(T o) {
		Class<? extends Object> clazz = o.getClass();
		return (T) Reflections.classLookup().newInstance(clazz);
	}

	protected Object shallowishObjectClone(Object o) {
		if (o instanceof Collection) {
			return CommonUtils.shallowCollectionClone((Collection) o);
		}
		return o;
	}
}
