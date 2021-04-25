package cc.alcina.framework.common.client.remote;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class DevRemoteServiceAsync {
	protected <T> void call(String methodName, Class[] methodArgumentTypes,
			AsyncCallback callback, Object... methodArguments) {
		DevRemoteServicePayload payload = new DevRemoteServicePayload(
				getClass(), methodName, methodArgumentTypes, methodArguments);
		String serializedPayload = AlcinaBeanSerializer
				.serializeHolder(payload);
		Registry.impl(DevRpcRemoteServiceAsync.class).devRpc(serializedPayload,
				new AsyncCallback<String>() {
					@Override
					public void onFailure(Throwable caught) {
						callback.onFailure(caught);
					}

					@Override
					public void onSuccess(String result) {
						T t = AlcinaBeanSerializer.deserializeHolder(result);
						callback.onSuccess(t);
					}
				});
	}

	public static class DevRemoteServicePayload extends Model {
		private String methodName;

		private Class<? extends DevRemoteServiceAsync> asyncInterfaceClass;

		private List<?> methodArguments;

		private List<Class> methodArgumentTypes;

		public DevRemoteServicePayload() {
		}

		public DevRemoteServicePayload(
				Class<? extends DevRemoteServiceAsync> asyncInterfaceClass,
				String methodName, Class[] methodArgumentTypes,
				Object[] methodArguments) {
			this.methodName = methodName;
			this.asyncInterfaceClass = asyncInterfaceClass;
			this.methodArgumentTypes = Arrays.stream(methodArgumentTypes)
					.collect(Collectors.toList());
			this.methodArguments = Arrays.stream(methodArguments)
					.collect(Collectors.toList());
		}

		public Class<? extends DevRemoteServiceAsync> getAsyncInterfaceClass() {
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
				Class<? extends DevRemoteServiceAsync> asyncInterfaceClass) {
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
