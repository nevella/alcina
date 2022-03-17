package cc.alcina.framework.common.client.remote;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.LooseContext;

/*
 * Marker for dev remoteservice call handlers
 */
public interface ReflectiveRemoteServiceHandler {
	public static String serializeForClient(Object payload){
		try {
			LooseContext.push();
			AlcinaTransient.Support.setTransienceContexts(
					TransienceContext.CLIENT, TransienceContext.RPC);
			return ReflectiveSerializer.serialize(payload);
		} finally {
			LooseContext.pop();
		}	
	}
}
