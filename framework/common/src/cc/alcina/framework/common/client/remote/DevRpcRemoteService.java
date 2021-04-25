package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.RemoteService;

public interface DevRpcRemoteService extends RemoteService {
	public String devRpc(String encodedRpcPayload);
}
