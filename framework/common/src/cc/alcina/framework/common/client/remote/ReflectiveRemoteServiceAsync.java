package cc.alcina.framework.common.client.remote;

import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.AsyncSerializableTypes;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializationException;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializerOptions;
import cc.alcina.framework.common.client.util.Topic;
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
 * - security is handled by requiring a WebMethod annotation on the receiver method
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
			AlcinaRpc.Request payload = new AlcinaRpc.Request(getClass(),
					methodName, methodArgumentTypes, methodArguments);
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
							boolean throwWrapped = false;
							try {
								try {
									LooseContext.push();
									AlcinaTransient.Support
											.setTransienceContexts(
													TransienceContext.CLIENT);
									AlcinaRpc.Response response = ReflectiveSerializer
											.deserialize(result);
									if (response.success) {
										throwWrapped = true;
										callback.onSuccess(response.payload);
									} else {
										onFailure((Throwable) response.payload);
									}
								} finally {
									LooseContext.pop();
								}
							} catch (Exception e) {
								if (throwWrapped) {
									WrappedRuntimeException.throwWrapped(e);
								} else {
									onFailure(e);
									return;
								}
							}
						}
					});
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	protected AsyncCallback successCallback(Consumer consumer) {
		return Async.callbackBuilder().success(consumer::accept).build();
	}
}
