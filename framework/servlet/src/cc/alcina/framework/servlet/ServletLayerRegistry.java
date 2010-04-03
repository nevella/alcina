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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * To prevent classloader problems if we were to have the same for server/ejb
 * layer
 * 
 * @author nick@alcina.cc
 * 
 */
public class ServletLayerRegistry extends Registry {
	public ServletLayerRegistry() {
		super();
	}

	private static ServletLayerRegistry theInstance = new ServletLayerRegistry();

	public static ServletLayerRegistry get() {
		return theInstance;
	}

	@Override
	public Object instantiateSingle(Class registryPoint, Class targetObject) {
		Class lookupSingle = lookupSingle(registryPoint, targetObject);
		try {
			return lookupSingle.newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}