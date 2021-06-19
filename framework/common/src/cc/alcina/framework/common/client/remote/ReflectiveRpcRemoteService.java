package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.RemoteService;

public interface ReflectiveRpcRemoteService extends RemoteService {
	public String callRpc(String encodedRpcPayload);
}
