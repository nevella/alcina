package cc.alcina.framework.entity.parser.structured;

import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.StringMap;

public class XmlTokenOutputContext implements Cloneable {
	public static XmlTokenOutputContext EMPTY = new XmlTokenOutputContext();

	protected StringMap properties = new StringMap();

	protected Set<String> seenKeys = new LinkedHashSet<>();

	public Set<String> getSeenKeys() {
		return this.seenKeys;
	}

	private String tag;

	@Override
	public XmlTokenOutputContext clone() {
		try {
			XmlTokenOutputContext attrs = getClass().newInstance();
			attrs.properties = properties.clone();
			attrs.seenKeys = new LinkedHashSet<>(seenKeys);
			return attrs;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public StringMap getProperties() {
		return this.properties;
	}

	public String getTag() {
		return this.tag;
	}

	public boolean hasTag() {
		return this.tag != null;
	}

	public XmlTokenOutputContext outputTag(String tag) {
		this.tag = tag;
		return this;
	}

	public void propertyDelta(String key, boolean add) {
		properties.setBooleanOrRemove(key, add);
		seenKeys.add(key);
	}

	public XmlTokenOutputContext put(String key, String value) {
		properties.put(key, value);
		return this;
	}

	public XmlTokenOutputContext putTrue(String name) {
		properties.put(name, "true");
		return this;
	}
}
