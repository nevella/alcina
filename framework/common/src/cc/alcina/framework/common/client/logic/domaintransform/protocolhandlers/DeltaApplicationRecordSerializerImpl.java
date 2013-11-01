package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.SimpleStringParser;

public class DeltaApplicationRecordSerializerImpl implements
		DeltaApplicationSerializer {
	private static final String TEXT = "text:\n";

	private static final String TAG2 = "tag:\n";

	private static final String TYPE = "type:";

	private static final String USER_ID = "userId:";

	private static final String TIMESTAMP = "timestamp:";

	private static final String REQUEST_ID = "requestId:";

	private static final String CLIENT_INSTANCE_ID = "clientInstanceId:";

	private static final String CLIENT_INSTANCE_AUTH = "clientInstanceAuth:";

	private static final String VERSION = "DeltaApplicationRecordSerializer/01";

	private static final String TRANSFORM_PROTOCOL_VERSION = "Transform-protocol-version:";

	public List<DeltaApplicationRecord> readMultiple(String data) {
		DeltaApplicationSerializer otherSerializer = otherSerializer(data);
		if (otherSerializer != null) {
			return otherSerializer.readMultiple(data);
		}
		List<DeltaApplicationRecord> wrappers = new ArrayList<DeltaApplicationRecord>();
		String sep = VERSION + "\n";
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

	private DeltaApplicationSerializer otherSerializer(String data) {
		int idx = data.indexOf(VERSION);
		int idxDtr = data
				.indexOf(DTRSimpleSerialSerializerOld.DTR_SIMPLE_SERIAL_SERIALIZER_VERSION_1_0);
		if (idxDtr != -1 && (idx == -1 || idxDtr < idx)) {
			return new DTRSimpleSerialSerializerOld();
		}
		return null;
	}

	public DeltaApplicationRecord read(String data) {
		DeltaApplicationSerializer otherSerializer = otherSerializer(data);
		if (otherSerializer != null) {
			return otherSerializer.read(data);
		}
		SimpleStringParser parser = new SimpleStringParser(data);
		String nl = "\n";
		parser.read(VERSION, nl);
		int clientInstanceAuth = (int) parser.readLongString(
				CLIENT_INSTANCE_AUTH, nl);
		long clientInstanceId = parser.readLongString(CLIENT_INSTANCE_ID, nl);
		int requestId = (int) parser.readLongString(REQUEST_ID, nl);
		long timestamp = parser.readLongString(TIMESTAMP, nl);
		long userId = parser.readLongString(USER_ID, nl);
		DeltaApplicationRecordType type = DeltaApplicationRecordType
				.valueOf(parser.read(TYPE, nl));
		String tag = parser.read(TAG2, nl);
		String protocolVersion = parser.read(TRANSFORM_PROTOCOL_VERSION, nl);
		String transformText = parser.read(TEXT, "");
		int id = 0;
		return new DeltaApplicationRecord(id, transformText, timestamp, userId,
				clientInstanceId, requestId, clientInstanceAuth, type,
				protocolVersion, tag);
	}

	public String write(DeltaApplicationRecord wrapper) {
		return CommonUtils.formatJ(VERSION + "\n" + CLIENT_INSTANCE_AUTH
				+ "%s\n" + CLIENT_INSTANCE_ID + "%s\n" + REQUEST_ID + "%s\n"
				+ TIMESTAMP + "%s\n" + USER_ID + "%s\n" + TYPE + "%s\n" + TAG2
				+ "%s\n" + TRANSFORM_PROTOCOL_VERSION + "%s\n" + TEXT + "%s",
				wrapper.getClientInstanceAuth(), wrapper.getClientInstanceId(),
				wrapper.getRequestId(), wrapper.getTimestamp(),
				wrapper.getUserId(), wrapper.getType(), wrapper.getTag(),
				wrapper.getProtocolVersion(), wrapper.getText());
	}
}