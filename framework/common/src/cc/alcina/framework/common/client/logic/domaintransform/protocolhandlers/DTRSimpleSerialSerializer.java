package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.SimpleStringParser;

public class DTRSimpleSerialSerializer {
	private static final String TEXT = "text:\n";

	private static final String TAG2 = "tag:\n";

	private static final String DOMAIN_TRANSFORM_REQUEST_TYPE = "DomainTransformRequestType:";

	private static final String USER_ID = "userId:";

	private static final String TIMESTAMP = "timestamp:";

	private static final String REQUEST_ID = "requestId:";


	private static final String CLIENT_INSTANCE_ID = "clientInstanceId:";

	private static final String CLIENT_INSTANCE_AUTH = "clientInstanceAuth:";

	private static final String DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0 = "DTRSimpleSerialSerializer-version:1.0";

	private static final String TRANSFORM_PROTOCOL_VERSION = "Transform-protocol-version:";

	public DTRSimpleSerialWrapper read(String text) {
		SimpleStringParser parser = new SimpleStringParser(text);
		String nl = "\n";
		parser.read(DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0, nl);
		int clientInstanceAuth = (int) parser.readLongString(
				CLIENT_INSTANCE_AUTH, nl);
		long clientInstanceId = parser.readLongString(CLIENT_INSTANCE_ID, nl);
		int requestId = (int) parser.readLongString(REQUEST_ID, nl);
		long timestamp = parser.readLongString(TIMESTAMP, nl);
		long userId = parser.readLongString(USER_ID, nl);
		DomainTransformRequestType domainTransformRequestType = DomainTransformRequestType
				.valueOf(parser.read(DOMAIN_TRANSFORM_REQUEST_TYPE, nl));
		String tag = parser.read(TAG2, nl);
		String protocolVersion = parser.read(TRANSFORM_PROTOCOL_VERSION, nl);
		String transformText = parser.read(TEXT, "");
		int id = 0;
		return new DTRSimpleSerialWrapper(id, transformText, timestamp, userId,
				clientInstanceId, requestId, clientInstanceAuth,
				domainTransformRequestType, protocolVersion, tag);
	}

	public String write(DTRSimpleSerialWrapper wrapper) {
		return CommonUtils.formatJ(DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0
				+ "\n" + CLIENT_INSTANCE_AUTH + "%s\n" + CLIENT_INSTANCE_ID
				+ "%s\n" + REQUEST_ID + "%s\n" + TIMESTAMP + "%s\n" + USER_ID
				+ "%s\n" + DOMAIN_TRANSFORM_REQUEST_TYPE + "%s\n" + TAG2
				+ "%s\n" + TRANSFORM_PROTOCOL_VERSION + "%s\n" + TEXT + "%s",
				wrapper.getClientInstanceAuth(), wrapper.getClientInstanceId(),
				 wrapper.getRequestId(),
				wrapper.getTimestamp(), wrapper.getUserId(),
				wrapper.getDomainTransformRequestType(), wrapper.getTag(),wrapper.getProtocolVersion(),
				wrapper.getText());
	}
}