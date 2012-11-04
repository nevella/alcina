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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;

/**
 * Base on com.google.gwt.user.client.rpc.impl.RemoteServiceProxy
 */
public class RemoteServiceSyncProxy implements SerializationStreamFactory {
	private static class DummySerializationPolicy extends SerializationPolicy {
		public boolean shouldDeserializeFields(Class<?> clazz) {
			return clazz != null;
		}

		public boolean shouldSerializeFields(Class<?> clazz) {
			return clazz != null;
		}

		public void validateDeserialize(Class<?> clazz)
		throws SerializationException {
		}

		public void validateSerialize(Class<?> clazz)
		throws SerializationException {
		}
	}

	public static Map<String, String> headers = new LinkedHashMap<String, String>();

	private String moduleBaseURL;

	private String remoteServiceURL;

	private String serializationPolicyName;

	private SerializationPolicy serializationPolicy;

	private SessionManager connectionManager;

	public RemoteServiceSyncProxy(String moduleBaseURL,
			String remoteServiceRelativePath, String serializationPolicyName,
			SessionManager connectionManager) {
		this.moduleBaseURL = moduleBaseURL;
		if (remoteServiceRelativePath.startsWith("/")) {
			int idx = moduleBaseURL.indexOf("//") + 2;
			idx = moduleBaseURL.indexOf("/", idx);
			this.remoteServiceURL = moduleBaseURL.substring(0, idx)
			+ remoteServiceRelativePath;
		} else {
			this.remoteServiceURL = moduleBaseURL + remoteServiceRelativePath;
		}
		this.serializationPolicyName = serializationPolicyName;
		this.connectionManager = connectionManager;
		if (serializationPolicyName == null) {
			serializationPolicy = new DummySerializationPolicy();
		} else {
			// TODO
			if (true) {
				// serializationPolicy = new DummySerializationPolicy();
				// return;
			}
			String policyFileName = SerializationPolicyLoader
			.getSerializationPolicyFileName(serializationPolicyName);
			// if pre-loaded, use the pre-loaded version.
			if (connectionManager instanceof DefaultSessionManager) {
				// may be unnecessary check and instead modify SessionManager
				// interface
				serializationPolicy = ((DefaultSessionManager) connectionManager)
				.getSerializationPolicy(serializationPolicyName);
			}
			if (serializationPolicy == null) {
				InputStream is = getClass().getResourceAsStream(
						"/" + policyFileName);
				try {
					serializationPolicy = SerializationPolicyLoader
					.loadFromStream(is, null);
				} catch (Exception e) {
					throw new InvocationException(
							"Error while loading serialization policy "
							+ serializationPolicyName, e);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							// Ignore this error
						}
					}
				}
			}// en
		}
		unionizeWhitelists();
	}

	public SyncClientSerializationStreamReader createStreamReader(String encoded)
	throws SerializationException {
		SyncClientSerializationStreamReader reader = new SyncClientSerializationStreamReader(
				serializationPolicy);
		reader.prepareToRead(encoded);
		return reader;
	}

	public SyncClientSerializationStreamWriter createStreamWriter() {
		SyncClientSerializationStreamWriter streamWriter = new SyncClientSerializationStreamWriter(
				null, moduleBaseURL, serializationPolicyName,
				serializationPolicy);
		streamWriter.prepareToWrite();
		return streamWriter;
	}

	private void unionizeWhitelists() {
		if (serializationPolicy instanceof StandardSerializationPolicy) {
			try {
				Field f = StandardSerializationPolicy.class
				.getDeclaredField("serializationWhitelist");
				f.setAccessible(true);
				Map<Class<?>, Boolean> serializationWhitelist = (Map<Class<?>, Boolean>) f
				.get(serializationPolicy);
				f = StandardSerializationPolicy.class
				.getDeclaredField("deserializationWhitelist");
				f.setAccessible(true);
				Map<Class<?>, Boolean> deserializationWhitelist = (Map<Class<?>, Boolean>) f
				.get(serializationPolicy);
				f = StandardSerializationPolicy.class
				.getDeclaredField("typeIds");
				f.setAccessible(true);
				Map<Class<?>, String> obfuscatedTypeIds = (Map<Class<?>, String>) f
				.get(serializationPolicy);
				f = StandardSerializationPolicy.class
				.getDeclaredField("clientFields");
				f.setAccessible(true);
				Map<Class<?>, Set<String>> clientFields = (Map<Class<?>, Set<String>>) f
				.get(serializationPolicy);
				serializationWhitelist.putAll(deserializationWhitelist);
				deserializationWhitelist = serializationWhitelist;
				serializationPolicy = new StandardSerializationPolicy(
						serializationWhitelist, deserializationWhitelist,
						obfuscatedTypeIds, clientFields);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public Object doInvoke(
			RequestCallbackAdapter.ResponseReader responseReader,
			String requestData) throws Throwable {
		Map<String, String> headersCopy = new LinkedHashMap<String, String>();
		synchronized (RemoteServiceSyncProxy.class) {
			headersCopy.putAll(headers);
		}
		HttpURLConnection connection = null;
		InputStream is = null;
		int statusCode;
		// Send request
		try {
			URL url = new URL(remoteServiceURL);
			connection = connectionManager.openConnection(url);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty(RpcRequestBuilder.STRONG_NAME_HEADER,
					serializationPolicyName);
			connection.setRequestProperty("Content-Type",
			"text/x-gwt-rpc; charset=utf-8");
			connection.setRequestProperty("Content-Length",
					"" + requestData.getBytes("UTF-8").length);
			// Explicitly set these to override any system properties.
			connection.setReadTimeout(60000);
			connection.setConnectTimeout(60000);
			for (Entry<String, String> header : headersCopy.entrySet()) {
				connection.setRequestProperty(header.getKey(),
						header.getValue());
			}
			OutputStreamWriter writer = new OutputStreamWriter(
					connection.getOutputStream());
			writer.write(requestData);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new InvocationException(
					"IOException while sending RPC request", e);
		}
		// Receive and process response
		try {
			connectionManager.handleResponseHeaders(connection);
			statusCode = connection.getResponseCode();
			is = connection.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = is.read(buffer)) > 0) {
				baos.write(buffer, 0, len);
			}
			String encodedResponse = baos.toString("UTF8");
			// System.out.println("Response payload (len = " +
			// encodedResponse.length() + "): " + encodedResponse);
			if (statusCode != HttpURLConnection.HTTP_OK) {
				throw new StatusCodeException(statusCode, encodedResponse);
			} else if (encodedResponse == null) {
				// This can happen if the XHR is interrupted by the server dying
				throw new InvocationException("No response payload");
			} else if (isReturnValue(encodedResponse)) {
				encodedResponse = encodedResponse.substring(4);
				return responseReader.read(createStreamReader(encodedResponse));
			} else if (isThrownException(encodedResponse)) {
				encodedResponse = encodedResponse.substring(4);
				Throwable throwable = (Throwable) createStreamReader(
						encodedResponse).readObject();
				throw throwable;
			} else {
				throw new InvocationException("Unknown response "
						+ encodedResponse);
			}
		} catch (IOException e) {
			throw new InvocationException(
					"IOException while receiving RPC response", e);
		} catch (SerializationException e) {
			throw new InvocationException(
					"Error while deserialization response", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
			if (connection != null) {
				// connection.disconnect();
			}
		}
	}

	static boolean isReturnValue(String encodedResponse) {
		return encodedResponse.startsWith("//OK");
	}

	static boolean isThrownException(String encodedResponse) {
		return encodedResponse.startsWith("//EX");
	}
}
