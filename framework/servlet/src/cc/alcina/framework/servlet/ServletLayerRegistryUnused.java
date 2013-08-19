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
package cc.alcina.framework.servlet;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * To prevent classloader problems if we were to have the same for server/ejb
 * layer
 * 
 * We duplicate all the static methods from Registry to get the correct get()
 * 
 * now i understand more...we can do something with thread.getcontextclassloader
 * and probably drop this
 * 
 * @author nick@alcina.cc
 * 
 */
@Deprecated
public class ServletLayerRegistryUnused extends Registry {
	public ServletLayerRegistryUnused() {
		super();
	}

	private static ServletLayerRegistryUnused theInstance = new ServletLayerRegistryUnused();

	public static ServletLayerRegistryUnused get() {
		return theInstance;
	}

	public static <V> V impl(Class<V> registryPoint) {
		return get().impl0(registryPoint, void.class, false);
	}

	public static <V> V impl(Class<V> registryPoint, Class targetObjectClass) {
		return get().impl0(registryPoint, targetObjectClass, false);
	}

	public static <V> V impl(Class<V> registryPoint, Class targetObjectClass,
			boolean allowNull) {
		return get().impl0(registryPoint, targetObjectClass, allowNull);
	}

	public static <V> List<V> impls(Class<V> registryPoint) {
		return impls(registryPoint, void.class);
	}

	public static <V> List<V> impls(Class<V> registryPoint, Class targetClass) {
		return get().impls0(registryPoint, targetClass);
	}

	public static <T> T singleton(Class<T> clazz) {
		return get().singleton0(clazz);
	}
}