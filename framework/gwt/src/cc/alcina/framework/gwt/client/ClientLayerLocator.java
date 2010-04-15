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

package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.actions.ActionLogProvider;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.ProvidesCommonRemoteService;

/**
 *
 * @author Nick Reddel
 */

 public class ClientLayerLocator {
	private ClientLayerLocator() {
		super();
	}

	private static ClientLayerLocator theInstance;

	public static ClientLayerLocator get() {
		if (theInstance == null) {
			theInstance = new ClientLayerLocator();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	private ProvidesCommonRemoteService pcrs;

	public void registerCommonRemoteServiceProvider(
			ProvidesCommonRemoteService pcrs) {
		this.pcrs = pcrs;
	}

	public CommonRemoteServiceAsync commonRemoteServiceAsync() {
		return pcrs.getCommonRemoteService();
	}
	private ActionLogProvider actionLogProvider;

	public void registerActionLogProvider(ActionLogProvider provider) {
		this.actionLogProvider = provider;
	}

	public ActionLogProvider actionLogProvider() {
		return actionLogProvider;
	}
	private ClientBase clientBase;

	public void registerClientBase(ClientBase base) {
		this.clientBase = base;
	}

	public ClientBase clientBase() {
		return clientBase;
	}
}
