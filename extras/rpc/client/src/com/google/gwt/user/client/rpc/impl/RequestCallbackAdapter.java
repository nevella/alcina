/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.RpcTokenExceptionHandler;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.StatusCodeException;

/**
 * Adapter from a {@link RequestCallback} interface to an {@link AsyncCallback}
 * interface.
 * 
 * For internal use only.
 * 
 * @param <T>
 *            the type parameter for the {@link AsyncCallback}
 */
public class RequestCallbackAdapter<T> implements RequestCallback {
	/**
	 * {@link AsyncCallback} to notify or success or failure.
	 */
	private  AsyncCallback<T> callback;
	
	public AsyncCallback<T> getCallback() {
		return this.callback;
	}

	public void setCallback(AsyncCallback<T> callback) {
		this.callback = callback;
	}


	/**
	 * Used for stats recording.
	 */
	private final String methodName;

	/**
	 * Used for stats recording.
	 */
	private final RpcStatsContext statsContext;

	/**
	 * Instance which will read the expected return type out of the
	 * {@link SerializationStreamReader}.
	 */
	private final ResponseReader responseReader;

	/**
	 * {@link RpcTokenExceptionHandler} to notify of token exceptions.
	 */
	private final RpcTokenExceptionHandler tokenExceptionHandler;

	/**
	 * {@link SerializationStreamFactory} for creating
	 * {@link SerializationStreamReader}s.
	 */
	private final SerializationStreamFactory streamFactory;

	public RequestCallbackAdapter(SerializationStreamFactory streamFactory,
			String methodName, RpcStatsContext statsContext,
			AsyncCallback<T> callback, ResponseReader responseReader) {
		this(streamFactory, methodName, statsContext, callback, null,
				responseReader);
	}

	public RequestCallbackAdapter(SerializationStreamFactory streamFactory,
			String methodName, RpcStatsContext statsContext,
			AsyncCallback<T> callback,
			RpcTokenExceptionHandler tokenExceptionHandler,
			ResponseReader responseReader) {
		assert (streamFactory != null);
		assert (callback != null);
		assert (responseReader != null);
		this.streamFactory = streamFactory;
		this.callback = callback;
		this.methodName = methodName;
		this.statsContext = statsContext;
		this.responseReader = responseReader;
		this.tokenExceptionHandler = tokenExceptionHandler;
	}

	public void onError(Request request, Throwable exception) {
		callback.onFailure(exception);
	}

	@SuppressWarnings(value = { "unchecked" })
	public void onResponseReceived(Request request, Response response) {
		T result = null;
		Throwable caught = null;
		final PostDeserializationCallback postDeserializationCallback = new PostDeserializationCallback();
		try {
			String encodedResponse = response.getText();
			int statusCode = response.getStatusCode();
			boolean toss = statsContext.isStatsAvailable()
					&& statsContext.stats(statsContext.bytesStat(methodName,
							encodedResponse.length(), "responseReceived"));
			if (statusCode != Response.SC_OK) {
				caught = new StatusCodeException(statusCode, encodedResponse);
			} else if (encodedResponse == null) {
				// This can happen if the XHR is interrupted by the server dying
				caught = new InvocationException(
						"No response payload from " + methodName);
			} else if (RemoteServiceProxy.isThrownException(encodedResponse)) {
				final ClientSerializationStreamReader exceptionReader = (ClientSerializationStreamReader) streamFactory
						.createStreamReader(encodedResponse);
				postDeserializationCallback.streamReader = exceptionReader;
				AsyncCallback exceptionCallback = new AsyncCallback() {
					@Override
					public void onFailure(Throwable caught) {
						postDeserializationCallback.onFailure(caught);
					}

					@Override
					public void onSuccess(Object toss) {
						try {
							Object object = exceptionReader.readObject();
							onFailure((Throwable) object);
						} catch (Throwable e) {
							onFailure(e);
						}
					}
				};
				exceptionReader.doDeserialize(exceptionCallback);
				return;
			} else if (RemoteServiceProxy.isReturnValue(encodedResponse)) {
				// next clause
			} else {
				caught = new InvocationException(
						encodedResponse + " from " + methodName);
			}
			if (caught != null) {
			} else {
				if (RemoteServiceProxy.isReturnValue(encodedResponse)) {
					ClientSerializationStreamReader reader = (ClientSerializationStreamReader) streamFactory
							.createStreamReader(encodedResponse);
					postDeserializationCallback.streamReader = reader;
					reader.doDeserialize(postDeserializationCallback);
					return;
				}
			}
		} catch (com.google.gwt.user.client.rpc.SerializationException e) {
			caught = new IncompatibleRemoteServiceException(
					"The response could not be deserialized", e);
		} catch (Throwable e) {
			caught = e;
		}
		if (caught != null) {
			postDeserializationCallback.onFailure(caught);
		} else {
			postDeserializationCallback.onSuccess(null);
		}
	}

	/**
	 * Enumeration used to read specific return types out of a
	 * {@link SerializationStreamReader}.
	 */
	public enum ResponseReader {
		BOOLEAN {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readBoolean();
			}
		},
		BYTE {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readByte();
			}
		},
		CHAR {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readChar();
			}
		},
		DOUBLE {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readDouble();
			}
		},
		FLOAT {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readFloat();
			}
		},
		INT {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readInt();
			}
		},
		LONG {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readLong();
			}
		},
		OBJECT {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readObject();
			}
		},
		SHORT {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readShort();
			}
		},
		STRING {
			@Override
			public Object read(SerializationStreamReader streamReader)
					throws SerializationException {
				return streamReader.readString();
			}
		},
		VOID {
			@Override
			public Object read(SerializationStreamReader streamReader) {
				return null;
			}
		};
		public abstract Object read(SerializationStreamReader streamReader)
				throws SerializationException;
	}

	class PostDeserializationCallback implements AsyncCallback {
		public ClientSerializationStreamReader streamReader;

		private Object result;

		private Throwable caught;

		public PostDeserializationCallback() {
		}

		@Override
		public void onFailure(Throwable caught) {
			this.caught = caught;
			postDeserializeStats();
			if (tokenExceptionHandler != null
					&& caught instanceof RpcTokenException) {
				tokenExceptionHandler
						.onRpcTokenException((RpcTokenException) caught);
			} else {
				callback.onFailure(caught);
			}
			postCallbackStats();
		}

		@Override
		public void onSuccess(Object toss) {
			result = null;
			if (streamReader != null) {
				try {
					result = (T) responseReader.read(streamReader);
				} catch (Throwable e) {
					onFailure(e);
				}
			}
			postDeserializeStats();
			try {
				callback.onSuccess((T) result);
			} finally {
				postCallbackStats();
			}
		}

		private void postCallbackStats() {
			Object returned = (caught == null) ? result : caught;
			boolean toss = statsContext.isStatsAvailable() && statsContext
					.stats(statsContext.timeStat(methodName, returned, "end"));
		}

		private void postDeserializeStats() {
			boolean toss = statsContext.isStatsAvailable()
					&& statsContext.stats(statsContext.timeStat(methodName,
							"responseDeserialized"));
		}
	}
}
