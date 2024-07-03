package cc.alcina.framework.common.client.remote;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.AsyncSerializableTypes;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializationException;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializerOptions;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.Async;

/*
 * @formatter:off
 * GWT serialization integration notes:
 *
 * - serializer should throw on unknown property/class/enum
 * - otherwise should not fail
 * - this basically means "no need for policy" - since there's enough metadata
 *   in the serialization format itself to enable success/fail logic at the serialization
 *   recipient end
 * - still, for versioning, we can tie gwt app hash (xor of modules) > server serialization hash
 * - this will let us see the server serialization diff, which is a superset of the client serialization diff
 *
 * @formatter:on
 */
public class ReflectiveRemoteServiceAsync implements AsyncSerializableTypes {
	/*
	 * To short-circuit normal (asynccallback) handling of these exceptions -
	 * which are probably best handled globally with an app refresh - queue a
	 * [diaplay notification ui + refresh task] async throw an exception (sync)
	 * in the topic handler
	 */
	public static Topic<SerializationException> topicDeserializationException = Topic
			.create();

	protected <T> void call(String methodName, Class[] methodArgumentTypes,
			AsyncCallback callback, Object... methodArguments) {
		try {
			ReflectiveRemoteServicePayload payload = new ReflectiveRemoteServicePayload(
					getClass(), methodName, methodArgumentTypes,
					methodArguments);
			String serializedPayload = null;
			try {
				LooseContext.push();
				AlcinaTransient.Support
						.setTransienceContexts(TransienceContext.RPC);
				SerializerOptions options = new ReflectiveSerializer.SerializerOptions()
						.withElideDefaults(true);
				serializedPayload = ReflectiveSerializer.serialize(payload,
						options);
			} finally {
				LooseContext.pop();
			}
			// FIXME - dirndl 1x1f - shift to a different
			// serializationstream
			// (but using gwt-rpc infrastructure)
			Registry.impl(ReflectiveRpcRemoteServiceAsync.class)
					.callRpc(serializedPayload, new AsyncCallback<String>() {
						@Override
						public void onFailure(Throwable caught) {
							if (caught instanceof SerializationException) {
								topicDeserializationException.publish(
										(SerializationException) caught);
							}
							callback.onFailure(caught);
						}

						@Override
						public void onSuccess(String result) {
							T t = null;
							try {
								try {
									LooseContext.push();
									AlcinaTransient.Support
											.setTransienceContexts(
													TransienceContext.CLIENT);
									t = ReflectiveSerializer
											.deserialize(result);
								} finally {
									LooseContext.pop();
								}
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

	protected AsyncCallback successCallback(Consumer consumer) {
		return Async.callbackBuilder().success(consumer::accept).build();
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
