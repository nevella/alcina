package cc.alcina.framework.entity.parser.structured;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.parser.structured.StructuredTokenParserContext.NodeAncestorsContextProvider;

public class XmlTokenContext implements Cloneable {
	public	static final String P_contextResolutionRoot = "P_contextResolutionRoot";
 
	public static XmlTokenContext EMPTY = new XmlTokenContext()
			.empty();

	protected StringMap properties = new StringMap();

	protected StringMap emitAttributes = new StringMap();

	protected Set<String> seenKeys = new LinkedHashSet<>();

	@SuppressWarnings("unused")
	private boolean empty;

	private NodeAncestorsContextProvider contextProvider;

	private String tag;

	@Override
	public XmlTokenContext clone() {
		try {
			XmlTokenContext newInstance = getClass().newInstance();
			copyProperties(newInstance);
			return newInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public XmlTokenContext
			contextProvider(NodeAncestorsContextProvider contextProvider) {
		this.contextProvider = contextProvider;
		return this;
	}

	public XmlTokenContext contextResolutionRoot() {
		putTrue(P_contextResolutionRoot);
		return this;
	}

	public void copyProperties(XmlTokenContext newInstance) {
		newInstance.properties = properties.clone();
		newInstance.emitAttributes = emitAttributes.clone();
		newInstance.seenKeys = new LinkedHashSet<>(seenKeys);
	}

	public XmlTokenContext emit(String key, String value) {
		emitAttributes.put(key, value);
		return this;
	}

	public String get(String key) {
		return properties.get(key);
	}

	public StringMap getEmitAttributes() {
		return this.emitAttributes;
	}

	public StringMap getProperties() {
		return this.properties;
	}

	public Set<String> getSeenKeys() {
		return this.seenKeys;
	}

	public String getTag() {
		return this.tag;
	}
	public boolean hasTag() {
		return this.tag != null;
	}

	public boolean is(String key) {
		return properties.is(key);
	}

	public boolean isContextResolutionRoot() {
		return is(P_contextResolutionRoot);
	}

	public XmlTokenContext outputTag(String tag) {
		this.tag = tag;
		return this;
	}

	public void propertyDelta(String key, boolean add) {
		if (add) {
			properties.put(key, key);
		} else {
			properties.remove(key);
		}
		seenKeys.add(key);
	}
	public XmlTokenContext put(String key, String value) {
		properties.put(key, value);
		return this;
	}

	public XmlTokenContext putTrue(String name) {
		properties.put(name, "true");
		return this;
	}

	public String resolve(String key) {
		Iterator<XmlTokenContext> itr = contextProvider.contexts();
		while (itr.hasNext()) {
			XmlTokenContext context = itr.next();
			if (context.properties.containsKey(key)) {
				return context.properties.get(key);
			}
		}
		return null;
	}

	public boolean resolveTrue(String key) {
		return Boolean.valueOf(resolve(key));
	}

	@Override
	public String toString() {
		return String.format("Ctx:\nproperties: %s\nemit-attr: %s", properties,
				emitAttributes);
	}

	protected XmlTokenContext empty() {
		this.empty = true;
		return this;
	}
}
