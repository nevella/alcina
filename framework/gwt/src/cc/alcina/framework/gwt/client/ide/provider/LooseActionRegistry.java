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

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.actions.VetoableAction;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionHandler.LooseTargetedActionHandler;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class LooseActionRegistry {
	private LooseActionRegistry() {
		super();
		actionHandlers = new HashMap<String, LooseActionHandler>();
		loadFromRegistry();
	}

	private static LooseActionRegistry theInstance;

	public static LooseActionRegistry get() {
		if (theInstance == null) {
			theInstance = new LooseActionRegistry();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	private Map<String, LooseActionHandler> actionHandlers;

	public void registerHandler(LooseActionHandler wp) {
		actionHandlers.put(wp.getName(), wp);
	}

	public LooseActionHandler getHandler(String name) {
		return actionHandlers.get(name);
	}
	@SuppressWarnings("unchecked")
	public void performForTargetActionAndObject(VetoableAction action, Object target) {
		LooseTargetedActionHandler handler = (LooseTargetedActionHandler) Registry
				.get().instantiateSingle(action.getClass(),
						target.getClass());
		handler.performAction(target);
	}
	@SuppressWarnings("unchecked")
	void loadFromRegistry() {
		List<Class> handlers = Registry.get().lookup(false,
				LooseActionHandler.class, void.class, true);
		for (Class c : handlers) {
			LooseActionHandler handler = (LooseActionHandler) CommonLocator
					.get().classLookup().newInstance(c);
			registerHandler(handler);
		}
	}
}
