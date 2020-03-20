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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.server.rpc.SerializationPolicy;

/**
 * 
 * @author dhartford AutoSyncProxy will setup policy names and Serialization
 *         Policy classes for GWT based on the remote url .gwt.rpc files
 *         automatically instead of relying on local .gwt.rpc files in
 *         classpath. This will not detect if the code being tested is different
 *         than the code deployed to the tested server.
 */
public class AutoSyncProxy implements Serializable {
	static final transient long serialVersionUID = -1L;

	private static final transient String GWT_RPC_POLICY_FILE_EXT = ".gwt.rpc";

	public static Object getProxyInstance(Class serviceIntf,
			String moduleBaseURL, String remoteServiceRelativePath,
			boolean failingProxyOnRetrieveFail) {
		return getProxyInstance(serviceIntf, moduleBaseURL,
				remoteServiceRelativePath, failingProxyOnRetrieveFail, null,
				null);
	}

	public static Object getProxyInstance(Class serviceIntf,
			String moduleBaseURL, String remoteServiceRelativePath,
			boolean failingProxyOnRetrieveFail, String policyFileName,
			String savePathForOffline) {
		AutoSyncProxy instance = getInstance();
		if (instance.proxy == null) {
			instance.proxy = (Proxy) instance.newProxyInstance(serviceIntf,
					moduleBaseURL, remoteServiceRelativePath,
					failingProxyOnRetrieveFail, policyFileName,
					savePathForOffline);
		}
		return instance.proxy;
	}

	public static void nullifyInstance() {
		_instance = null;
	}

	private static AutoSyncProxy getInstance() {
		if (_instance == null) {
			_instance = new AutoSyncProxy();
		}
		return _instance;
	}

	private final transient DefaultSessionManager DEFAULT_SESSION_MANAGER = new DefaultSessionManager();

	// this contains the service and the policyname
	private Map<String, String> policyMap = new HashMap<String, String>();
	

	// this contains the policyname and the SerializationPolicy
	private transient Map<String, SerializationPolicy> serializationPolicyMap = new HashMap<String, SerializationPolicy>();

	private static transient AutoSyncProxy _instance = null;

	private transient Proxy proxy = null;

	private Map<String, String> getRequestCache;

	public SessionManager getDefaultSessionManager() {
		// return a different Session Manager with the prepopulated values.
		return DEFAULT_SESSION_MANAGER;
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
	
	private Object newProxyInstance(Class serviceIntf, String moduleBaseURL,
			String remoteServiceRelativePath,
			boolean failingProxyOnRetrieveFail, String policyFileName,
			String savePathForOffline) {
		try {
			retrieveSerializationPolicies(moduleBaseURL, policyFileName,
					savePathForOffline);
		} catch (Exception e) {
			if (failingProxyOnRetrieveFail) {
				return Proxy
						.newProxyInstance(SyncProxy.class.getClassLoader(),
								new Class[] { serviceIntf },
								new FailAsyncCallHandler(moduleBaseURL,
										remoteServiceRelativePath, "",
										DEFAULT_SESSION_MANAGER));
			} else {
				throw new RuntimeException(e);
			}
		}
		DEFAULT_SESSION_MANAGER
				.setSerializationPolicyMap(serializationPolicyMap);
		String siName = serviceIntf.getName();
		siName = siName.replaceAll("Async\\z", "");
		return Proxy.newProxyInstance(SyncProxy.class.getClassLoader(),
				new Class[] { serviceIntf },
				new RemoteServiceInvocationHandler(moduleBaseURL,
						remoteServiceRelativePath, policyMap.get(siName),
						DEFAULT_SESSION_MANAGER));
	}

	private void retrieveSerializationPolicies(String moduleBaseURL,
			String policyFileName, String savePathForOffline) throws Exception {
		moduleBaseURL = moduleBaseURL.trim(); // remove outer trim just in case
		List<String> guessAllGwtPolicyName = null;
		if (policyFileName != null) {
			guessAllGwtPolicyName = RpcFinderUtil.guessAllGwtPolicyName(
					moduleBaseURL, policyFileName);
		} else {
			guessAllGwtPolicyName = RpcFinderUtil
					.guessAllGwtPolicyName(moduleBaseURL);
		}
		for (Iterator iterator = guessAllGwtPolicyName.iterator(); iterator
				.hasNext();) {
			String policyname = (String) iterator.next();
			String policyUrl = moduleBaseURL + "/" + policyname
					+ GWT_RPC_POLICY_FILE_EXT;
			String servicename = RpcFinderUtil.findServiceName(policyUrl);
			SerializationPolicy p = RpcFinderUtil.getSchedulePolicy(policyUrl);
			policyMap.put(servicename, policyname);
			serializationPolicyMap.put(policyname, p);
		}
		if (savePathForOffline != null) {
			if (policyMap.size() > 0) {
				this.getRequestCache=RpcFinderUtil.getRequestCache;
				writeObject(this, savePathForOffline);
			} else {
				AutoSyncProxy prior = readObject(new AutoSyncProxy(),
						savePathForOffline);
				policyMap = prior.policyMap;
				RpcFinderUtil.getRequestCache=prior.getRequestCache;
				for(String policyName:policyMap.values()){
					String policyUrl = moduleBaseURL + "/" + policyName
							+ GWT_RPC_POLICY_FILE_EXT;
					SerializationPolicy p = RpcFinderUtil.getSchedulePolicy(policyUrl);
					serializationPolicyMap.put(policyName, p);
				}
			}
		}
	}

	public <V> V readObject(V template, String path) throws Exception {
		File cacheFile = new File(path);
		if (!cacheFile.exists()) {
			return null;
		}
		ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
				new FileInputStream(cacheFile)));
		V value = (V) ois.readObject();
		ois.close();
		return value;
	}

	public void writeObject(Object obj, String path) throws Exception {
		File cacheFile = new File(path);
		cacheFile.createNewFile();
		OutputStream out = new FileOutputStream(cacheFile);
		out = new BufferedOutputStream(out);
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(obj);
		oos.close();
	}

	static class FailAsyncCallHandler extends RemoteServiceInvocationHandler {
		public FailAsyncCallHandler(String moduleBaseURL,
				String remoteServiceRelativePath,
				String serializationPolicyName, SessionManager connectionManager) {
			super(moduleBaseURL, remoteServiceRelativePath,
					serializationPolicyName, connectionManager);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			AsyncCallback callback = (AsyncCallback) args[args.length - 1];
			callback.onFailure(new StatusCodeException(0, "Offline"));
			return null;
		}
	}
}
