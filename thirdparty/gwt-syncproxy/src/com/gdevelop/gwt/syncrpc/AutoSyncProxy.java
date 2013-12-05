/*
 * Copyright www.gdevelop.com.
 * 
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
package com.gdevelop.gwt.syncrpc;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

/**
 * 
 * @author dhartford AutoSyncProxy will setup policy names and Serialization
 *         Policy classes for GWT based on the remote url .gwt.rpc files
 *         automatically instead of relying on local .gwt.rpc files in
 *         classpath. This will not detect if the code being tested is different
 *         than the code deployed to the tested server.
 */
public class AutoSyncProxy {
	private static final String GWT_RPC_POLICY_FILE_EXT = ".gwt.rpc";

	private final DefaultSessionManager DEFAULT_SESSION_MANAGER = new DefaultSessionManager();

	public SessionManager getDefaultSessionManager() {
		// return a different Session Manager with the prepopulated values.
		return DEFAULT_SESSION_MANAGER;
	}

	// this contains the service and the policyname
	private Map<String, String> POLICY_MAP = new HashMap<String, String>();

	// this contains the policyname and the SerializationPolicy
	private Map<String, SerializationPolicy> SERIALIZATIONPOLICY_MAP = new HashMap<String, SerializationPolicy>();

	private static AutoSyncProxy _instance = null;

	private Proxy proxy = null;

	private static AutoSyncProxy getInstance() {
		if (_instance == null) {
			_instance = new AutoSyncProxy();
		}
		return _instance;
	}
	public static void nullifyInstance() {
		_instance = null;
	}

	public static Object getProxyInstance(Class serviceIntf, String moduleBaseURL, String remoteServiceRelativePath, boolean failingProxyOnRetrieveFail) {
		return getProxyInstance(serviceIntf, moduleBaseURL, remoteServiceRelativePath, failingProxyOnRetrieveFail, null);
	}

	public static Object getProxyInstance(Class serviceIntf, 
			String moduleBaseURL, 
			String remoteServiceRelativePath, 
			boolean failingProxyOnRetrieveFail, 
			String policyFileName) 
	{
		AutoSyncProxy instance = getInstance();
		if (instance.proxy == null) {
			instance.proxy = (Proxy)instance.newProxyInstance(serviceIntf, moduleBaseURL, remoteServiceRelativePath, failingProxyOnRetrieveFail, policyFileName);
		}
		return instance.proxy;
	}

	/**
	 * Create a new Proxy for the specified <code>serviceIntf</code>
	 * 
	 * @param serviceIntf
	 *            The remote service interface
	 * @param moduleBaseURL
	 *            Base URL
	 * @param remoteServiceRelativePath
	 *            The remote service servlet relative path
	 * @return A new proxy object which implements the service interface
	 *         serviceIntf
	 */
	@SuppressWarnings("unchecked")
	private Object newProxyInstance(Class serviceIntf,
			String moduleBaseURL, String remoteServiceRelativePath,
			boolean failingProxyOnRetrieveFail, String policyFileName) {
		try {
			retrieveSerializationPolicies(moduleBaseURL, policyFileName);
		} catch (RuntimeException e) {
			if (failingProxyOnRetrieveFail) {
				return Proxy
				.newProxyInstance(SyncProxy.class.getClassLoader(),
						new Class[] { serviceIntf },
						new FailAsyncCallHandler());
			} else {
				throw e;
			}
		}
		DEFAULT_SESSION_MANAGER
		.setSerializationPolicyMap(SERIALIZATIONPOLICY_MAP);
		String siName = serviceIntf.getName();
		siName = siName.replaceAll("Async\\z", "");
		return Proxy.newProxyInstance(SyncProxy.class.getClassLoader(),
				new Class[] { serviceIntf },
				new RemoteServiceInvocationHandler(moduleBaseURL,
						remoteServiceRelativePath, POLICY_MAP.get(siName),
						DEFAULT_SESSION_MANAGER));
	}

	static class FailAsyncCallHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
		throws Throwable {
			AsyncCallback callback = (AsyncCallback) args[args.length - 1];
			callback.onFailure(new StatusCodeException(0, "Offline"));
			return null;
		}
	}

	private void retrieveSerializationPolicies(String moduleBaseURL, String policyFileName) {
		moduleBaseURL = moduleBaseURL.trim(); // remove outer trim just in case
		List<String> guessAllGwtPolicyName = null;
		if (policyFileName != null) {
			guessAllGwtPolicyName = RpcFinderUtil.guessAllGwtPolicyName(moduleBaseURL, policyFileName);
		} else {
			guessAllGwtPolicyName = RpcFinderUtil.guessAllGwtPolicyName(moduleBaseURL);
		}
		for (Iterator iterator = guessAllGwtPolicyName.iterator(); iterator.hasNext();) {
			String policyname = (String) iterator.next();
			String policyUrl = moduleBaseURL + "/" + policyname
			+ GWT_RPC_POLICY_FILE_EXT;
			String servicename = RpcFinderUtil.findServiceName(policyUrl);
			SerializationPolicy p = RpcFinderUtil.getSchedulePolicy(policyUrl);
			POLICY_MAP.put(servicename, policyname);
			SERIALIZATIONPOLICY_MAP.put(policyname, p);
		}
	}
}
