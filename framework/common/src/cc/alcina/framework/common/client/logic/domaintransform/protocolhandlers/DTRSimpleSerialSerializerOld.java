package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.EnumSerializer;
import cc.alcina.framework.common.client.util.SimpleStringParser;

public class DTRSimpleSerialSerializerOld implements DeltaApplicationSerializer{
	private static final String TEXT = "text:\n";

	private static final String TAG2 = "tag:\n";

	private static final String DOMAIN_TRANSFORM_REQUEST_TYPE = "DeltaApplicationRecordType:";

	private static final String USER_ID = "userId:";

	private static final String TIMESTAMP = "timestamp:";

	private static final String REQUEST_ID = "requestId:";

	private static final String CLIENT_INSTANCE_ID = "clientInstanceId:";

	private static final String CLIENT_INSTANCE_AUTH = "clientInstanceAuth:";

	 static final String DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0 = "DTRSimpleSerialSerializer-version:1.0";

	private static final String TRANSFORM_PROTOCOL_VERSION = "Transform-protocol-version:";

	public List<DeltaApplicationRecord> readMultiple(String data) {
		List<DeltaApplicationRecord> wrappers = new ArrayList<DeltaApplicationRecord>();
		String sep = DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0 + "\n";
		int idx = 0, idy = 0;
		while (true) {
			idx = data.indexOf(sep, idy);
			if (idx == -1) {
				break;
			}
			idy = data.indexOf(sep, idx + sep.length());
			idy = idy == -1 ? data.length() : idy;
			wrappers.add(read(data.substring(idx, idy)));
		}
		return wrappers;
	}

	public DeltaApplicationRecord read(String data) {
		SimpleStringParser parser = new SimpleStringParser(data);
		String nl = "\n";
		parser.read(DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0, nl);
		int clientInstanceAuth = (int) parser.readLongString(
				CLIENT_INSTANCE_AUTH, nl);
		long clientInstanceId = parser.readLongString(CLIENT_INSTANCE_ID, nl);
		int requestId = (int) parser.readLongString(REQUEST_ID, nl);
		long timestamp = parser.readLongString(TIMESTAMP, nl);
		long userId = parser.readLongString(USER_ID, nl);
		DeltaApplicationRecordType type = Registry.impl(EnumSerializer.class)
				.deserialize(DeltaApplicationRecordType.class,
						parser.read(DOMAIN_TRANSFORM_REQUEST_TYPE, nl));
		String tag = parser.read(TAG2, nl);
		String protocolVersion = parser.read(TRANSFORM_PROTOCOL_VERSION, nl);
		String transformText = parser.read(TEXT, "");
		int id = 0;
		return new DeltaApplicationRecord(id, transformText, timestamp, userId,
				clientInstanceId, requestId, clientInstanceAuth, type,
				protocolVersion, tag);
	}

	public String write(DeltaApplicationRecord wrapper) {
		return CommonUtils.formatJ(DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0
				+ "\n" + CLIENT_INSTANCE_AUTH + "%s\n" + CLIENT_INSTANCE_ID
				+ "%s\n" + REQUEST_ID + "%s\n" + TIMESTAMP + "%s\n" + USER_ID
				+ "%s\n" + DOMAIN_TRANSFORM_REQUEST_TYPE + "%s\n" + TAG2
				+ "%s\n" + TRANSFORM_PROTOCOL_VERSION + "%s\n" + TEXT + "%s",
				wrapper.getClientInstanceAuth(), wrapper.getClientInstanceId(),
				wrapper.getRequestId(), wrapper.getTimestamp(),
				wrapper.getUserId(), wrapper.getType(), wrapper.getTag(),
				wrapper.getProtocolVersion(), wrapper.getText());
	}
}