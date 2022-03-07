package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.SimpleStringParser;

@Reflected
@Registration(DTRProtocolHandler.class)
public class PlaintextProtocolHandler1pt1 implements DTRProtocolHandler {
	public static final String VERSION = "1.1 - plain text, GWT2.1";

	private static final String TGT = "tgt: ";

	private static final String STRING_VALUE = "string value: ";

	private static final String PARAMS = "params: ";

	private static final String SRC = "src: ";

	private static final String DOMAIN_TRANSFORM_EVENT_MARKER = "\nDomainTransformEvent:";

	public static String escape(String str) {
		return str == null
				|| (str.indexOf("\n") == -1 && str.indexOf("\\") == -1) ? str
						: str.replace("\\", "\\\\").replace("\n", "\\n");
	}

	public static String unescape(String str) {
		if (str == null) {
			return null;
		}
		int idx = 0, x = 0;
		StringBuilder sb = new StringBuilder(str.length());
		while ((idx = str.indexOf("\\", x)) != -1) {
			sb.append(str.substring(x, idx));
			char c = str.charAt(idx + 1);
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
		sb.append(str.substring(x));
		return sb.toString();
	}

	private SimpleStringParser asyncParser = null;

	public void appendTo(DomainTransformEvent domainTransformEvent,
			StringBuffer sb) {
		String newStringValue = domainTransformEvent.getNewStringValue();
		String ns = escape(newStringValue);
		sb.append(getDomainTransformEventMarker());
		String newlineTab = "\n\t";
		sb.append(newlineTab);
		sb.append(SRC);
		sb.append(domainTransformEvent.getObjectClass() == null
				? domainTransformEvent.getObjectClassName()
				: domainTransformEvent.getObjectClass().getName());
		sb.append(",");
		sb.append(SimpleStringParser
				.toString(domainTransformEvent.getObjectId()));
		sb.append(",");
		sb.append(SimpleStringParser
				.toString(domainTransformEvent.getObjectLocalId()));
		sb.append(newlineTab);
		sb.append(PARAMS);
		sb.append(domainTransformEvent.getPropertyName());
		sb.append(",");
		sb.append(domainTransformEvent.getCommitType());
		sb.append(",");
		sb.append(domainTransformEvent.getTransformType());
		sb.append(",");
		sb.append(SimpleStringParser
				.toString(domainTransformEvent.getUtcDate() == null
						? System.currentTimeMillis()
						: domainTransformEvent.getUtcDate().getTime()));
		sb.append(newlineTab);
		sb.append(STRING_VALUE);
		sb.append(ns);
		sb.append(newlineTab);
		sb.append(TGT);
		sb.append(domainTransformEvent.getValueClass() == null
				? domainTransformEvent.getValueClassName()
				: domainTransformEvent.getValueClass().getName());
		sb.append(",");
		sb.append(
				SimpleStringParser.toString(domainTransformEvent.getValueId()));
		sb.append(",");
		sb.append(SimpleStringParser
				.toString(domainTransformEvent.getValueLocalId()));
		sb.append("\n");
	}

	public List<DomainTransformEvent> deserialize(String serializedEvents) {
		List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();
		asyncParser = null;
		deserialize(serializedEvents, events, Integer.MAX_VALUE);
		return events;
	}

	public String deserialize(String serializedEvents,
			List<DomainTransformEvent> events, int maxCount) {
		if (asyncParser == null) {
			asyncParser = new SimpleStringParser(serializedEvents);
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

	public String getDomainTransformEventMarker() {
		return DOMAIN_TRANSFORM_EVENT_MARKER;
	}

	public int getOffset() {
		return asyncParser == null ? 0 : asyncParser.getOffset();
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

	/*
	 * Note - the way client handshake works, if this is called from a
	 * "from-offline upload" (partialdtruploader)we won't have any classrefs -
	 * so the setXXClassRef calls will just set a nullThis means upload calls
	 * are not tied to any (app) class structure, so will at least make it and
	 * be stored on the server.
	 * 
	 * Of course, if a class has changed/been deleted on the server, you'll need
	 * to deal with that on the server :- at least you'll have data somewhere
	 * reachable, not stuck on a bunch of iPads in some bit-deprived continent
	 */
	private DomainTransformEvent fromString(String s) {
		DomainTransformEvent dte = new DomainTransformEvent();
		SimpleStringParser p = new SimpleStringParser(s);
		String i = p.read(SRC, ",");
		// just in case we're in a no-classref
		dte.setObjectClassName(i);
		// environment
		dte.setObjectClassRef(ClassRef.forName(i));
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
		}
		i = p.read(STRING_VALUE, "\n");
		dte.setNewStringValue(unescape(i));
		i = p.read(TGT, ",");
		// just in case we're in a no-classref
		dte.setValueClassName(i);
		// environment
		dte.setValueClassRef(ClassRef.forName(i));
		dte.setValueId(p.readLong("", ","));
		dte.setValueLocalId(p.readLong("", "\n"));
		if (dte.getTransformType() != TransformType.CHANGE_PROPERTY_SIMPLE_VALUE
				&& dte.getNewStringValue().equals("null")) {
			dte.setNewStringValue(null);
		}
		return dte;
	}
}
