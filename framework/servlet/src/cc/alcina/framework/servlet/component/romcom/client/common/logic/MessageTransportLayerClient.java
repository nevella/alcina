package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;

public class MessageTransportLayerClient extends MessageTransportLayer {
	@Override
	public void sendMessage(Message message) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'sendMessage'");
	}

	/*
	 * An layer above the RPC call to either 'send message x' or 'await receipt
	 * of message'
	 */
	class RpcChannel {
	}

	RpcChannel rpcSendChannel;

	RpcChannel rpcAwaitChannel;

	@Override
	protected SendChannel sendChannel() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'sendChannel'");
	}
}
