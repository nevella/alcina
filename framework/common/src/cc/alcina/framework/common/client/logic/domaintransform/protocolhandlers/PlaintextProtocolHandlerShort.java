package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.SimpleStringParser;

@RegistryLocation(registryPoint = DTRProtocolHandler.class, j2seOnly = false)
@ClientInstantiable
public class PlaintextProtocolHandlerShort implements DTRProtocolHandler {
	private static final String START_OF_STRING_TABLE = "str:\n";

	public static final String VERSION = "1.4 - plain text, short, GWT2.5";

	private static final String DOMAIN_TRANSFORM_EVENT_MARKER = "\ndte:";

	private static String escape(String str) {
		return str == null
		|| (str.indexOf("\n") == -1 && str
				.indexOf("\\") == -1) ? str : str
						.replace("\\", "\\\\").replace("\n", "\\n");
	}

	public static String unescape(String str) {
		if (str == null) {
			return null;
		}
		int idx = 0, x = 0;
		StringBuilder sb = new StringBuilder();
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

	private StringBuilder stringLookupBuilder;

	private LinkedHashMap<String, String> stringLookup0;

	private LinkedHashMap<String, String> reverseStringLookup;

	public void appendTo(DomainTransformEvent domainTransformEvent,
			StringBuffer sb) {
		String newStringValue = domainTransformEvent.getNewStringValue();
		String ns = escape(newStringValue);
		sb.append(getDomainTransformEventMarker());
		sb.append("\n");
		appendString(
				(domainTransformEvent.getObjectClass() == null ? domainTransformEvent.getObjectClassName()
						: domainTransformEvent.getObjectClass().getName()), sb);
		sb.append(",");
		sb.append(SimpleStringParser.toStringNoInfo(domainTransformEvent
				.getObjectId()));
		sb.append(",");
		sb.append(SimpleStringParser.toStringNoInfo(domainTransformEvent
				.getObjectLocalId()));
		sb.append(",");
		sb.append(SimpleStringParser.toStringOrNullNonNegativeInteger(domainTransformEvent
				.getObjectVersionNumber()));
		sb.append("\n");
		appendString(domainTransformEvent.getPropertyName(), sb);
		sb.append(",");
		appendString(domainTransformEvent.getCommitType().toString(), sb);
		sb.append(",");
		appendString(domainTransformEvent.getTransformType().toString(), sb);
		sb.append("\n");
		appendString(ns, sb);
		sb.append("\n");
		appendString(
				domainTransformEvent.getValueClass() == null ? domainTransformEvent.getValueClassName()
						: domainTransformEvent.getValueClass().getName(), sb);
		sb.append(",");
		sb.append(SimpleStringParser.toStringNoInfo(domainTransformEvent
				.getValueId()));
		sb.append(",");
		sb.append(SimpleStringParser.toStringNoInfo(domainTransformEvent
				.getValueLocalId()));
		sb.append(",");
		sb.append(SimpleStringParser.toStringOrNullNonNegativeInteger(domainTransformEvent
				.getValueVersionNumber()));
		sb.append("\n");
	}

	public List<DomainTransformEvent> deserialize(String serializedEvents) {
		List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();
		asyncParser = null;
		deserialize(serializedEvents, events, Integer.MAX_VALUE);
		return events;
	}

	public void maybeDeserializeStringLookup(SimpleStringParser p) {
		String s;
		if (reverseStringLookup == null) {
			s = p.read(START_OF_STRING_TABLE, "\n\n");
			reverseStringLookup = new LinkedHashMap<String, String>();
			int idx = 0;
			reverseStringLookup.put(String.valueOf(idx++), null);
			reverseStringLookup.put(String.valueOf(idx++), "");
			for (String value : s.split("\n")) {
				reverseStringLookup.put(
						String.valueOf(reverseStringLookup.size()), value);
			}
		}
	}

	public String deserialize(String serializedEvents,
			List<DomainTransformEvent> events, int maxCount) {
		if (asyncParser == null) {
			asyncParser = new SimpleStringParser(serializedEvents);
			maybeDeserializeStringLookup(asyncParser);
		}
		int i = 0;
		String s;
		while ((s = asyncParser.read(getDomainTransformEventMarker(),
				getDomainTransformEventMarker(), false, false)) != null) {
			events.add(fromString(s));
			if (i++ == maxCount) {
				break;
			}
		}
		return s;
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
		return finishSerialization(sb2).toString();
	}

	@Override
	public StringBuffer finishSerialization(StringBuffer sb) {
		StringBuffer sb1 = new StringBuffer();// table
		sb1.append(START_OF_STRING_TABLE);
		Iterator<String> itr = ensureStringLookup().keySet().iterator();
		itr.next();
		itr.next();
		while (itr.hasNext()) {
			sb1.append(itr.next());
			sb1.append("\n");
		}
		sb1.append("\n");
		sb1.append(sb);
		return sb1;
	}

	private void appendString(String string, StringBuffer sb) {
		LinkedHashMap<String, String> stringLookup = ensureStringLookup();
		if (!stringLookup.containsKey(string)) {
			stringLookup.put(string, String.valueOf(stringLookup.size()));
		}
		sb.append(stringLookup.get(string));
	}

	private LinkedHashMap<String, String> ensureStringLookup() {
		if (stringLookup0 == null) {
			stringLookupBuilder = new StringBuilder();
			stringLookupBuilder.append(START_OF_STRING_TABLE);
			stringLookup0 = new LinkedHashMap<String, String>();
			stringLookup0.put(null, String.valueOf(0));
			stringLookup0.put("", String.valueOf(1));
		}
		return stringLookup0;
				
	}

	/*
	 * Note - the way client handshake works, if this is called from a
	 * "from-offline upload" (partialdtruploader) we won't have any classrefs -
	 * so the setXXClassRef calls will just set a null. This means upload calls
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
		String i = getString(p.read("\n", ","));
		dte.setObjectClassName(i);// just in case we're in a no-classref
		// environment
		dte.setObjectClassRef(ClassRef.forName(i));
		dte.setObjectId(p.readLongShort("", ","));
		dte.setObjectLocalId(p.readLongShort("", ","));
		dte.setObjectVersionNumber(p.readNonNegativeIntegerOrNull("", "\n"));
		String pName = getString(p.read("", ","));
		dte.setPropertyName(pName);
		String commitTypeStr = getString(p.read("", ","));
		dte.setCommitType(CommitType.valueOf(commitTypeStr));
		dte.setTransformType(TransformType.valueOf(getString(p.read("", "\n"))));
		i = getString(p.read("", "\n"));
		dte.setNewStringValue(unescape(i));
		i = getString(p.read("", ","));
		dte.setValueClassName(i);// just in case we're in a no-classref
		// environment
		dte.setValueClassRef(ClassRef.forName(i));
		dte.setValueId(p.readLongShort("", ","));
		dte.setValueLocalId(p.readLongShort("", ","));
		dte.setValueVersionNumber(p.readNonNegativeIntegerOrNull("", "\n"));
		return dte;
	}

	private String getString(String key) {
		return reverseStringLookup.get(key);
	}
}
