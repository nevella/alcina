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
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.RemoteServiceProvider;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelper;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

import com.google.gwt.core.client.GWT;

/**
 * 
 * @author Nick Reddel
 */
public class ClientLayerLocator {
	private static ClientLayerLocator theInstance;

	public static ClientLayerLocator get() {
		if (theInstance == null) {
			theInstance = new ClientLayerLocator();
		}
		return theInstance;
	}
	private boolean usesRootLayoutPanel;

	private RemoteServiceProvider<? extends CommonRemoteServiceAsync> commonRemoteServiceAsyncProvider;

	private ActionLogProvider actionLogProvider;

	private CommitToStorageTransformListener commitToStorageTransformListener;

	private ClientBase clientBase;


	private DomainModelHolder domainModelHolder;

	private ClientHandshakeHelper clientHandshakeHelper;

	private TimerWrapperProvider timerWrapperProvider;
	
	private ClientNotifications clientNotifications;

	private ClientInstance clientInstance;

	private ClientExceptionHandler exceptionHandler;
	

	private ClientLayerLocator() {
		super();
	}

	public ActionLogProvider actionLogProvider() {
		return actionLogProvider;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public ClientBase clientBase() {
		return clientBase;
	}

	public CommonRemoteServiceAsync commonRemoteServiceAsyncInstance() {
		return commonRemoteServiceAsyncProvider.getServiceInstance();
	}

	public ClientExceptionHandler exceptionHandler() {
		return exceptionHandler;
	}

	public ClientHandshakeHelper getClientHandshakeHelper() {
		return clientHandshakeHelper;
	}

	public ClientInstance getClientInstance() {
		return clientInstance;
	}

	public CommitToStorageTransformListener getCommitToStorageTransformListener() {
		return commitToStorageTransformListener;
	}

	public RemoteServiceProvider<? extends CommonRemoteServiceAsync> getCommonRemoteServiceAsyncProvider() {
		return this.commonRemoteServiceAsyncProvider;
	}

	public DomainModelHolder getDomainModelHolder() {
		return domainModelHolder;
	}

	public GeneralProperties getGeneralProperties() {
		return Registry.impl(GeneralProperties.class);
	}

	public ClientNotifications notifications() {
		return clientNotifications;
	}

	public void registerActionLogProvider(ActionLogProvider provider) {
		this.actionLogProvider = provider;
	}

	public void registerClientBase(ClientBase base) {
		this.clientBase = base;
		Registry.get().registerSingleton(base,ClientBase.class);
	}

	public void registerCommonRemoteServiceAsyncProvider(
			RemoteServiceProvider<? extends CommonRemoteServiceAsync> commonRemoteServiceAsyncProvider) {
		this.commonRemoteServiceAsyncProvider = commonRemoteServiceAsyncProvider;
		Registry.get().registerSingleton(commonRemoteServiceAsyncProvider,RemoteServiceProvider.class);
	}

	public void registerExceptionHandler(ClientExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		Registry.get().registerSingleton(exceptionHandler,ClientExceptionHandler.class);
		GWT.setUncaughtExceptionHandler(exceptionHandler);
	}

	public void registerNotifications(ClientNotifications clientNotifications) {
		this.clientNotifications = clientNotifications;
		Registry.get().registerSingleton(clientNotifications,ClientNotifications.class);
	}

	public void registerTimerWrapperProvider(TimerWrapperProvider timerWrapperProvider) {
		this.timerWrapperProvider = timerWrapperProvider;
	}

	public void setClientHandshakeHelper(
			ClientHandshakeHelper clientHandshakeHelper) {
		this.clientHandshakeHelper = clientHandshakeHelper;
		Registry.get().registerSingleton(clientHandshakeHelper,ClientHandshakeHelper.class);
	}

	public void setClientInstance(ClientInstance clientInstance) {
		if (clientInstance.getUser() != null) {
			PermissionsManager.get().setUser(clientInstance.getUser());
		}
		// not needed, and heavyweight (for transforms, etc)
		clientInstance.setUser(null);
		this.clientInstance = clientInstance;
	}

	public void setCommitToStorageTransformListener(
			CommitToStorageTransformListener commitToStorageTransformListener) {
		this.commitToStorageTransformListener = commitToStorageTransformListener;
	}

	public void setDomainModelHolder(DomainModelHolder domainModelHolder) {
		this.domainModelHolder = domainModelHolder;
	}


	public TimerWrapperProvider timerWrapperProvider() {
		return timerWrapperProvider;
	}

	public boolean isUsesRootLayoutPanel() {
		return this.usesRootLayoutPanel;
	}

	public void setUsesRootLayoutPanel(boolean usesRootLayoutPanel) {
		this.usesRootLayoutPanel = usesRootLayoutPanel;
	}
}
