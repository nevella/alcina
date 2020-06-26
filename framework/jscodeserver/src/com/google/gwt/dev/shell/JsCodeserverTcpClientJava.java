/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.shell;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;

/**
 * Implementation of the BrowserChannel for the client side.
 */
public class JsCodeserverTcpClientJava extends BrowserChannel {
	private DataOutputStream out;

	private Message lastMessage;

	public JsCodeserverTcpClientJava(Socket socket) throws IOException {
		super(socket, new ClientObjectRefFactory());
	}

	public String getLastMessageDetails() {
		if (this.lastMessage instanceof InvokeOnClientMessage) {
			return ((InvokeOnClientMessage) this.lastMessage).getMethodName();
		} else {
			return "-";
		}
	}

	public String getLastMessageName() {
		return this.lastMessage.getClass().getSimpleName();
	}

	public Message receiveMessage() throws Exception {
		MessageType type = Message.readMessageType(getStreamFromOtherSide());
		Message message = null;
		switch (type) {
		case INVOKE:
			message = InvokeOnClientMessage.receive(this);
			break;
		case FREE_VALUE:
			message = FreeMessage.receive(this);
			break;
		case LOAD_JSNI:
			message = LoadJsniMessage.receive(this);
			break;
		case REQUEST_ICON:
			message = RequestIconMessage.receive(this);
			break;
		case RETURN:
			message = ReturnMessage.receive(this);
			break;
		case QUIT:
			message = QuitMessage.receive(this);
			break;
		case PROTOCOL_VERSION:
			message = ProtocolVersionMessage.receive(this);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		this.lastMessage = message;
		return message;
	}

	public byte[] receiveMessageBytes() {
		try {
			Message message = receiveMessage();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			out = new DataOutputStream(baos);
			message.send();
			out.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	protected DataOutputStream getStreamToOtherSide() {
		return out;
	}

	private static class ClientObjectRefFactory implements ObjectRefFactory {
		private final RemoteObjectTable<JavaObjectRef> remoteObjectTable;

		public ClientObjectRefFactory() {
			remoteObjectTable = new RemoteObjectTable<JavaObjectRef>();
		}

		@Override
		public JavaObjectRef getJavaObjectRef(int refId) {
			JavaObjectRef objectRef = remoteObjectTable
					.getRemoteObjectRef(refId);
			if (objectRef == null) {
				objectRef = new JavaObjectRef(refId);
				remoteObjectTable.putRemoteObjectRef(refId, objectRef);
			}
			return objectRef;
		}

		@Override
		public JsObjectRef getJsObjectRef(int refId) {
			return new JsObjectRef(refId);
		}

		@Override
		public Set<Integer> getRefIdsForCleanup() {
			return remoteObjectTable.getRefIdsForCleanup();
		}
	}
}
