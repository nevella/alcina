package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.SimpleStringParser;

@RegistryLocation(registryPoint = DTRProtocolHandler.class, j2seOnly = false)
public class PlaintextProtocolHandler implements DTRProtocolHandler {
	public static final String VERSION = "1.0 - plain text";

	private static final String TGT = "tgt: ";

	private static final String STRING_VALUE = "string value: ";
	private static final String PARAMS = "params: ";
	private static final String SRC = "src: ";

	public static final String DOMAIN_TRANSFORM_EVENT_MARKER = "\nDomainTransformEvent: ";

	private static Class classFromName(String className) {
		if (className == null || className.equals("null")) {
			return null;
		}
		return CommonLocator.get().classLookup().getClassForName(className);
	}

	private static String unescape(String s) {
		int idx = 0, x = 0;
		StringBuffer sb = new StringBuffer();
		while ((idx = s.indexOf("\\", x)) != -1) {
			sb.append(s.substring(x, idx));
			char c = s.charAt(idx + 1);
			switch (c) {
			case '\\':
				sb.append("\\");
				break;
			case 'n':
				sb.append("\n");
				break;
			}
			x = idx + 2;
		}
		sb.append(s.substring(x));
		return s.toString();
	}

	public void appendTo(DomainTransformEvent domainTransformEvent,
			StringBuffer sb) {
		String ns = domainTransformEvent.getNewStringValue() == null
				|| (domainTransformEvent.getNewStringValue().indexOf("\n") == -1 && domainTransformEvent
						.getNewStringValue().indexOf("\\") != -1) ? domainTransformEvent
				.getNewStringValue()
				: domainTransformEvent.getNewStringValue()
						.replace("\\", "\\\\").replace("\n", "\\n");
		sb.append(DOMAIN_TRANSFORM_EVENT_MARKER);
		String newlineTab = "\n\t";
		sb.append(newlineTab);
		sb.append(SRC);
		sb.append(domainTransformEvent.getObjectClass().getName());
		sb.append(",");
		sb.append(SimpleStringParser
				.longToGwtDoublesToString(domainTransformEvent.getObjectId()));
		sb.append(",");
		sb.append(SimpleStringParser
				.longToGwtDoublesToString(domainTransformEvent
						.getObjectLocalId()));
		sb.append(newlineTab);
		sb.append(PARAMS);
		sb.append(domainTransformEvent.getPropertyName());
		sb.append(",");
		sb.append(domainTransformEvent.getCommitType());
		sb.append(",");
		sb.append(domainTransformEvent.getTransformType());
		sb.append(",");
		sb.append(domainTransformEvent.getUtcDate() == null ? System
				.currentTimeMillis() : domainTransformEvent.getUtcDate()
				.getTime());
		sb.append(newlineTab);
		sb.append(STRING_VALUE);
		sb.append(ns);
		sb.append(newlineTab);
		sb.append(TGT);
		sb.append(domainTransformEvent.getValueClass() == null ? null
				: domainTransformEvent.getValueClass().getName());
		sb.append(",");
		sb.append(SimpleStringParser
				.longToGwtDoublesToString(domainTransformEvent.getValueId()));
		sb.append(",");
		sb.append(SimpleStringParser
				.longToGwtDoublesToString(domainTransformEvent
						.getValueLocalId()));
		sb.append("\n");
	}

	public List<DomainTransformEvent> deserialize(String serializedEvents) {
		List<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>();
		SimpleStringParser p = new SimpleStringParser(serializedEvents);
		String s;
		while ((s = p.read(DomainTransformEvent.DATA_TRANSFORM_EVENT_MARKER,
				DomainTransformEvent.DATA_TRANSFORM_EVENT_MARKER, true, false)) != null) {
			items.add(fromString(s));
		}
		return items;
	}

	public String handlesVersion() {
		return VERSION;
	}

	public String serialize(List<DomainTransformEvent> events) {
		StringBuffer sb = new StringBuffer();
		for (DomainTransformEvent domainTransformEvent : events) {
			appendTo(domainTransformEvent, sb);
		}
		return sb.toString();
	}

	private DomainTransformEvent fromString(String s) {
		DomainTransformEvent dte = new DomainTransformEvent();
		SimpleStringParser p = new SimpleStringParser(s);
		String i = p.read(SRC, ",");
		dte.setObjectClass(classFromName(i));
		dte.setObjectId(p.readLong("", ","));
		dte.setObjectLocalId(p.readLong("", "\n"));
		String pName = p.read(PARAMS, ",");
		dte.setPropertyName(pName.equals("null") ? null : pName);
		String commitTypeStr = p.read("", ",");
		commitTypeStr = commitTypeStr.equals("TO_REMOTE_STORAGE")?"TO_STORAGE":commitTypeStr;
		dte.setCommitType(CommitType.valueOf(commitTypeStr));
		// TODO - temporary compat
		if (p.indexOf(",") < p.indexOf("\n")) {
			dte.setTransformType(TransformType.valueOf(commitTypeStr));
			long utcTime = p.readLong("", "\n");
			dte.setUtcDate(new Date(utcTime));
		} else {
			dte.setTransformType(TransformType.valueOf(p.read("", "\n")));
			dte.setUtcDate(CommonLocator.get().currentUtcDateProvider()
					.currentUtcDate());
		}
		i = p.read(STRING_VALUE, "\n");
		dte.setNewStringValue(i.indexOf("\\") == -1 ? i : unescape(i));
		i = p.read(TGT, ",");
		dte.setValueClass(classFromName(i));
		dte.setValueId(p.readLong("", ","));
		dte.setValueLocalId(p.readLong("", "\n"));
		if (dte.getTransformType() != TransformType.CHANGE_PROPERTY_SIMPLE_VALUE
				&& dte.getNewStringValue().equals("null")) {
			dte.setNewStringValue(null);
		}
		return dte;
	}
}
