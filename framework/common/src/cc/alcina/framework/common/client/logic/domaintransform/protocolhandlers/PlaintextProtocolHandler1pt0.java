package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.SimpleStringParser20;

@RegistryLocation(registryPoint = DTRProtocolHandler.class, j2seOnly = false)
@ClientInstantiable
public class PlaintextProtocolHandler1pt0 implements DTRProtocolHandler {
	public static final String VERSION = "1.0 - plain text";

	private static final String TGT = "tgt: ";

	private static final String STRING_VALUE = "string value: ";

	private static final String PARAMS = "params: ";

	private static final String SRC = "src: ";

	private static final String DOMAIN_TRANSFORM_EVENT_MARKER = "\nDomainTransformEvent:";

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
	private SimpleStringParser20 asyncParser = null;

	public void appendTo(DomainTransformEvent domainTransformEvent,
			StringBuffer sb) {
		String ns = domainTransformEvent.getNewStringValue() == null
				|| (domainTransformEvent.getNewStringValue().indexOf("\n") == -1 && domainTransformEvent
						.getNewStringValue().indexOf("\\") != -1) ? domainTransformEvent
				.getNewStringValue()
				: domainTransformEvent.getNewStringValue()
						.replace("\\", "\\\\").replace("\n", "\\n");
		sb.append(getDomainTransformEventMarker());
		String newlineTab = "\n\t";
		sb.append(newlineTab);
		sb.append(SRC);
		sb.append(domainTransformEvent.getObjectClass().getName());
		sb.append(",");
		sb.append(SimpleStringParser20
				.longToGwtDoublesToString(domainTransformEvent.getObjectId()));
		sb.append(",");
		sb.append(SimpleStringParser20
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
		sb.append(SimpleStringParser20
				.longToGwtDoublesToString(domainTransformEvent.getValueId()));
		sb.append(",");
		sb.append(SimpleStringParser20
				.longToGwtDoublesToString(domainTransformEvent
						.getValueLocalId()));
		sb.append("\n");
	}

	public List<DomainTransformEvent> deserialize(String serializedEvents) {
		List<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>();
		SimpleStringParser20 p = new SimpleStringParser20(serializedEvents);
		String s;
		while ((s = p.read(getDomainTransformEventMarker(),
				getDomainTransformEventMarker(), true, false)) != null) {
			items.add(fromString(s));
		}
		return items;
	}

	public String deserialize(String serializedEvents,
			List<DomainTransformEvent> events, int maxCount) {
		if (asyncParser==null){
			asyncParser=new SimpleStringParser20(serializedEvents);
		}
		int i = 0;
		String s;
		while ((s = asyncParser.read(getDomainTransformEventMarker(),
				getDomainTransformEventMarker(), true, false)) != null) {
			events.add(fromString(s));
			if (i++ == maxCount) {
				break;
			}
		}
		return s;
	}
	@Override
	public StringBuffer finishSerialization(StringBuffer sb) {
		return sb;
	}

	public  String getDomainTransformEventMarker() {
		return DOMAIN_TRANSFORM_EVENT_MARKER;
	}

	public int getOffset() {
		return asyncParser==null?0:asyncParser.getOffset();
	}

	public String handlesVersion() {
		return VERSION;
	}

	public String serialize(List<DomainTransformEvent> events) {
		StringBuffer sb2 = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();
		int i = 0;
		for (DomainTransformEvent dte : events) {
			if (++i % 200 == 0) {
				sb2.append(sb1.toString());
				sb1 = new StringBuffer();
			}
			appendTo(dte, sb1);
		}
		sb2.append(sb1.toString());
		return sb2.toString();
	}

	private DomainTransformEvent fromString(String s) {
		DomainTransformEvent dte = new DomainTransformEvent();
		SimpleStringParser20 p = new SimpleStringParser20(s);
		String i = p.read(SRC, ",");
		dte.setObjectClass(classFromName(i));
		dte.setObjectId(p.readLong("", ","));
		dte.setObjectLocalId(p.readLong("", "\n"));
		String pName = p.read(PARAMS, ",");
		dte.setPropertyName(pName.equals("null") ? null : pName);
		String commitTypeStr = p.read("", ",");
		commitTypeStr = commitTypeStr.equals("TO_REMOTE_STORAGE") ? "TO_STORAGE"
				: commitTypeStr;
		dte.setCommitType(CommitType.valueOf(commitTypeStr));
		// TODO - temporary compat
		if (p.indexOf(",") < p.indexOf("\n")) {
			dte.setTransformType(TransformType.valueOf(p.read("", ",")));
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
