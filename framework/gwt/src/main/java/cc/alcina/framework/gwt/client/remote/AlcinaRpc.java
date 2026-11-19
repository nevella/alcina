package cc.alcina.framework.gwt.client.remote;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializerOptions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;

public class AlcinaRpc {
	@Bean
	public static class Request {
		private String methodName;

		private Class<? extends ReflectiveRemoteServiceAsync> asyncInterfaceClass;

		private List<?> methodArguments;

		private List<Class> methodArgumentTypes;

		public Request() {
		}

		public Request(
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

		public String toDebugString() {
			FormatBuilder format = new FormatBuilder();
			format.line("Method: %s", methodName);
			format.line("User: %s", Permissions.get().getUserString());
			format.line("Types:");
			format.indent(2);
			methodArgumentTypes.forEach(format::line);
			format.indent(0);
			format.newLine();
			format.line("Parameters:");
			format.indent(2);
			methodArguments.forEach(param -> {
				String serializedParam = ReflectiveSerializer.serialize(param,
						new SerializerOptions().withPretty(true)
								.withElideDefaults(true)
								.withDefaultCollectionTypes(true));
				format.line(Ax.trimForLogging(serializedParam, 1000));
			});
			return format.toString();
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class Response {
		public boolean success;

		@ReflectiveSerializer.Checks(ignore = true)
		public Object payload;
	}

	@Bean
	public static class RpcException extends Exception {
		public String message;

		public RpcException() {
		}

		public RpcException(Throwable wrappedException) {
			message = CommonUtils.toSimpleExceptionMessage(wrappedException);
		}

		@Override
		public String getMessage() {
			return message;
		}
	}
}
