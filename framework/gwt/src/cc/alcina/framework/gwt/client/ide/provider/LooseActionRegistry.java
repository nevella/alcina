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
package cc.alcina.framework.gwt.client.ide.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionHandler.LooseTargetedActionHandler;

/**
 * 
 * @author Nick Reddel
 */
public class LooseActionRegistry {
	public static LooseActionRegistry get() {
		LooseActionRegistry singleton = Registry
				.checkSingleton(LooseActionRegistry.class);
		if (singleton == null) {
			singleton = new LooseActionRegistry();
			Registry.registerSingleton(LooseActionRegistry.class, singleton);
		}
		return singleton;
	}

	private Map<String, LooseActionHandler> actionHandlers;

	private LooseActionRegistry() {
		super();
		actionHandlers = new HashMap<String, LooseActionHandler>();
		loadFromRegistry();
	}

	public LooseActionHandler getHandler(String name) {
		LooseActionHandler handler = actionHandlers.get(name);
		if (handler == null) {
			// handle reflection/code splitting
			loadFromRegistry();
		}
		return handler;
	}

	public void performForTargetActionAndObject(PermissibleAction action,
			Object target) {
		LooseTargetedActionHandler handler = (LooseTargetedActionHandler) Registry
				.get().instantiateSingle(action.getClass(), target.getClass());
		handler.performAction(target);
	}

	public void registerHandler(LooseActionHandler wp) {
		actionHandlers.put(wp.getName(), wp);
	}

	void loadFromRegistry() {
		List<LooseActionHandler> handlers = Registry
				.impls(LooseActionHandler.class);
		for (LooseActionHandler handler : handlers) {
			registerHandler(handler);
		}
	}
}
