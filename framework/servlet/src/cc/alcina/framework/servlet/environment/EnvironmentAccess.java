package cc.alcina.framework.servlet.environment;

import java.util.List;

import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Session;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.InvokeResponse;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.Startup;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken;
