package cc.alcina.framework.common.client.remote;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class ReflectiveRemoteServiceAsync {
	protected <T> void call(String methodName, Class[] methodArgumentTypes,
			AsyncCallback callback, Object... methodArguments) {
		try {
			ReflectiveRemoteServicePayload payload = new ReflectiveRemoteServicePayload(
					getClass(), methodName, methodArgumentTypes,
					methodArguments);
			String serializedPayload = ReflectiveSerializer.serialize(payload);
			// FIXME - 2021 - this can just use an http call - just need to
			// integrate Alcina header insertion/handling
			Registry.impl(ReflectiveRpcRemoteServiceAsync.class)
					.callRpc(serializedPayload, new AsyncCallback<String>() {
						@Override
						public void onFailure(Throwable caught) {
							callback.onFailure(caught);
						}

						@Override
						public void onSuccess(String result) {
							T t = null;
							try {
								t = ReflectiveSerializer.deserialize(result);
							} catch (Exception e) {
								onFailure(e);
								return;
							}
							callback.onSuccess(t);
						}
					});
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	public static class ReflectiveRemoteServicePayload extends Model {
		private String methodName;

		private Class<? extends ReflectiveRemoteServiceAsync> asyncInterfaceClass;

		private List<?> methodArguments;

		private List<Class> methodArgumentTypes;

		public ReflectiveRemoteServicePayload() {
		}

		public ReflectiveRemoteServicePayload(
				Class<? extends ReflectiveRemoteServiceAsync> asyncInterfaceClass,
				String methodName, Class[] methodArgumentTypes,
				Object[] methodArguments) {
			this.methodName = methodName;
			this.asyncInterfaceClass = asyncInterfaceClass;
			this.methodArgumentTypes = Arrays.stream(methodArgumentTypes)
					.collect(Collectors.toList());
			this.methodArguments = Arrays.stream(methodArguments)
					.collect(Collectors.toList());
		}

		public Class<? extends ReflectiveRemoteServiceAsync>
				getAsyncInterfaceClass() {
			return this.asyncInterfaceClass;
		}

		public List<?> getMethodArguments() {
			return this.methodArguments;
		}

		public List<Class> getMethodArgumentTypes() {
			return this.methodArgumentTypes;
		}

		public String getMethodName() {
			return this.methodName;
		}

		public void setAsyncInterfaceClass(
				Class<? extends ReflectiveRemoteServiceAsync> asyncInterfaceClass) {
			this.asyncInterfaceClass = asyncInterfaceClass;
		}

		public void setMethodArguments(List<?> methodArguments) {
			this.methodArguments = methodArguments;
		}

		public void setMethodArgumentTypes(List<Class> methodArgumentTypes) {
			this.methodArgumentTypes = methodArgumentTypes;
		}

		public void setMethodName(String methodName) {
			this.methodName = methodName;
		}
	}
}
