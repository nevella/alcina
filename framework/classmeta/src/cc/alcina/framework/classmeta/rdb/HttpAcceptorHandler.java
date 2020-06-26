package cc.alcina.framework.classmeta.rdb;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.classmeta.rdb.HttpAcceptorTransport.HttpConnectionPair;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.JacksonUtils;

public class HttpAcceptorHandler extends AbstractHandler {
	public static void main(String[] args) {
		String requestJson = ResourceUtilities.readClazzp("tmp.json");
		HttpTransportModel transportRequest = JacksonUtils
				.deserialize(requestJson, HttpTransportModel.class);
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		Ax.out("acceptor handler - thread: " + Thread.currentThread());
		HttpConnectionPair pair = new HttpConnectionPair();
		pair.request = request;
		pair.response = response;
		String requestJson = ResourceUtilities
				.readStreamToString(request.getInputStream());
		HttpTransportModel transportRequest = JacksonUtils
				.deserialize(requestJson, HttpTransportModel.class);
		Endpoint endpoint = RdbProxies.get()
				.endpointByName(transportRequest.endpointName);
		HttpAcceptorTransport transport = (HttpAcceptorTransport) endpoint.transport;
		if (transportRequest.close) {
			Ax.err("***close");
		}
		transport.receiveTransportModel(transportRequest, pair);
		baseRequest.setHandled(true);
	}
}
