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
package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class Registry {
	public static final String MARKER_RESOURCE = "registry.properties";

	protected HashMap<Class, Map<Class, List<Class>>> registry;

	protected HashMap<Class, Map<Class, Integer>> targetPriority;

	protected Registry() {
		super();
		registry = new HashMap<Class, Map<Class, List<Class>>>();
		targetPriority = new HashMap<Class, Map<Class, Integer>>();
	}

	private static Registry theInstance = new Registry();

	public static Registry get() {
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void register(Class registeringClass, RegistryLocation info) {
		Class registryPoint = info.registryPoint();
		Map<Class, List<Class>> pointMap = registry.get(registryPoint);
		Map<Class, Integer> pointPriority = targetPriority.get(registryPoint);
		if (pointMap == null) {
			pointMap = new HashMap<Class, List<Class>>();
			pointPriority = new HashMap<Class, Integer>();
			registry.put(registryPoint, pointMap);
			targetPriority.put(registryPoint, pointPriority);
		}
		Class targetClass = info.targetClass();
		List<Class> registered = pointMap.get(targetClass);
		if (registered == null) {
			registered = new ArrayList<Class>();
			pointMap.put(targetClass, registered);
		}
		if (registered.size() == 1 && targetClass != void.class) {
			Integer currentPriority = pointPriority.get(targetClass);
			int infoPriority = info.priority();
			if (currentPriority > infoPriority) {
				registered.clear();
			}else{
				return;
			}
		}
		registered.add(registeringClass);
		pointPriority.put(targetClass, info.priority());
	}

	public Class lookupSingle(Class registryPoint, Class targetObject) {
		return lookupSingle(registryPoint, targetObject, false);
	}

	public Class lookupSingle(Class registryPoint, Class targetObject,
			boolean errorOnNull) {
		List<Class> lookup = lookup(true, registryPoint, targetObject);
		Class result = lookup.size() > 0 ? lookup.get(0) : null;
		if (result == null && errorOnNull) {
			throw new RuntimeException(CommonUtils.format(
					"Could not find lookup - %1:%2", CommonUtils
							.classSimpleName(registryPoint), CommonUtils
							.classSimpleName(targetObject)));
		}
		return result;
	}

	public Object instantiateSingleOrNull(Class registryPoint,
			Class targetObject) {
		List<Class> lookup = lookup(true, registryPoint, targetObject, true);
		return lookup.size() > 0 ? instantiateSingle(registryPoint,
				targetObject) : null;
	}

	@SuppressWarnings("unchecked")
	public Object instantiateSingle(Class registryPoint, Class targetObject) {
		Class lookupSingle = lookupSingle(registryPoint, targetObject);
		try {
			return CommonLocator.get().classLookup().newInstance(lookupSingle);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public List<Class> lookup(Class registryPoint) {
		return lookup(false, registryPoint, void.class);
	}

	public List<Class> lookup(boolean mostSpecificTarget, Class registryPoint,
			Class targetObject) {
		return lookup(mostSpecificTarget, registryPoint, targetObject, false);
	}

	public List<Class> lookup(boolean mostSpecificTarget, Class registryPoint,
			Class targetObject, boolean notRequired) {
		// superclasschain
		List<Class> scChain = new ArrayList<Class>();
		Class c = targetObject;
		while (c != null) {
			scChain.add(c);
			c = c.getSuperclass();
		}
		if (!scChain.contains(void.class)) {
			scChain.add(void.class);
		}
		List<Class> result = new ArrayList<Class>();
		Map<Class, List<Class>> map = registry.get(registryPoint);
		if (map == null) {
			if (notRequired) {
				return new ArrayList<Class>(0);
			}
			throw new RuntimeException(CommonUtils.format(
					"Unable to locate class %1 - %2", registryPoint,
					targetObject));
		}
		for (Class sc : scChain) {
			if (map.containsKey(sc)) {
				result.addAll(map.get(sc));
				if (mostSpecificTarget && map.size() != 0) {
					break;
				}
			}
		}
		return result;
	}

	private String simpleName(Class c) {
		return c == null ? null : c.getName().contains(".") ? c.getName()
				.substring(c.getName().lastIndexOf(".") + 1) : c.getName();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Class registry:\n");
		for (Class c : registry.keySet()) {
			sb.append(simpleName(c));
			sb.append(": ");
			int x = 0;
			Map<Class, List<Class>> map = registry.get(c);
			for (Class c1 : map.keySet()) {
				if (x++ != 0) {
					sb.append(", ");
				}
				sb.append(simpleName(c1));
				sb.append("={");
				int y = 0;
				for (Class c2 : map.get(c1)) {
					if (y++ != 0) {
						sb.append(", ");
					}
					sb.append(simpleName(c2));
				}
				sb.append("}");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
