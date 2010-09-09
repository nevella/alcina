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
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsyncProvider;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelper;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

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

	private CommonRemoteServiceAsyncProvider commonRemoteServiceAsyncProvider;

	public CommonRemoteServiceAsyncProvider getCommonRemoteServiceAsyncProvider() {
		return this.commonRemoteServiceAsyncProvider;
	}


	private ActionLogProvider actionLogProvider;

	private CommitToStorageTransformListener commitToStorageTransformListener;

	private ClientBase clientBase;

	private GeneralProperties generalProperties;

	private DomainModelHolder domainModelHolder;

	private ClientHandshakeHelper clientHandshakeHelper;

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

	public void registerActionLogProvider(ActionLogProvider provider) {
		this.actionLogProvider = provider;
	}

	public void registerClientBase(ClientBase base) {
		this.clientBase = base;
	}

	public void registerCommonRemoteServiceAsyncProvider(
			CommonRemoteServiceAsyncProvider commonRemoteServiceAsyncProvider) {
		this.commonRemoteServiceAsyncProvider = commonRemoteServiceAsyncProvider;
	}

	private ClientNofications clientNotifications;

	private ClientInstance clientInstance;

	public void registerNotifications(ClientNofications clientNotifications) {
		this.clientNotifications = clientNotifications;
	}

	public ClientNofications notifications() {
		return clientNotifications;
	}

	public ClientInstance getClientInstance() {
		return clientInstance;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		// not needed, and heavyweight (for transforms, etc)
		clientInstance.setUser(null);
		this.clientInstance = clientInstance;
	}

	public void setCommitToStorageTransformListener(
			CommitToStorageTransformListener commitToStorageTransformListener) {
		this.commitToStorageTransformListener = commitToStorageTransformListener;
	}

	public CommitToStorageTransformListener getCommitToStorageTransformListener() {
		return commitToStorageTransformListener;
	}

	private ClientExceptionHandler exceptionHandler;

	public void registerExceptionHandler(ClientExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	public ClientExceptionHandler exceptionHandler() {
		return exceptionHandler;
	}

	public void setGeneralProperties(GeneralProperties generalProperties) {
		this.generalProperties = generalProperties;
	}

	public GeneralProperties getGeneralProperties() {
		return generalProperties;
	}

	public void setDomainModelHolder(DomainModelHolder domainModelHolder) {
		this.domainModelHolder = domainModelHolder;
	}

	public DomainModelHolder getDomainModelHolder() {
		return domainModelHolder;
	}

	public void setClientHandshakeHelper(
			ClientHandshakeHelper clientHandshakeHelper) {
		this.clientHandshakeHelper = clientHandshakeHelper;
	}

	public ClientHandshakeHelper getClientHandshakeHelper() {
		return clientHandshakeHelper;
	}
}
