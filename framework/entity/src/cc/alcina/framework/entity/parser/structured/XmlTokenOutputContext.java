package cc.alcina.framework.entity.parser.structured;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.StringMap;

public class XmlTokenOutputContext implements Cloneable {
	public static XmlTokenOutputContext EMPTY = new XmlTokenOutputContext()
			.empty();

	protected StringMap properties = new StringMap();

	protected StringMap emitAttributes = new StringMap();

	protected Set<String> seenKeys = new LinkedHashSet<>();

	@SuppressWarnings("unused")
	private boolean empty;

	private HierarchicalContextProvider contextProvider;

	private String tag;

	@Override
	public XmlTokenOutputContext clone() {
		try {
			XmlTokenOutputContext newInstance = getClass().newInstance();
			copyProperties(newInstance);
			return newInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public XmlTokenOutputContext
			contextProvider(HierarchicalContextProvider contextProvider) {
		this.contextProvider = contextProvider;
		return this;
	}

	public void copyProperties(XmlTokenOutputContext newInstance) {
		newInstance.properties = properties.clone();
		newInstance.emitAttributes = emitAttributes.clone();
		newInstance.seenKeys = new LinkedHashSet<>(seenKeys);
	}

	public XmlTokenOutputContext emit(String key, String value) {
		emitAttributes.put(key, value);
		return this;
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

	public String resolve(String key) {
		Iterator<XmlTokenOutputContext> itr = contextProvider.contexts();
		while (itr.hasNext()) {
			XmlTokenOutputContext context = itr.next();
			if (context.properties.containsKey(key)) {
				return context.properties.get(key);
			}
		}
		return null;
	}

	public boolean resolveTrue(String key) {
		return Boolean.valueOf(resolve(key));
	}

	protected XmlTokenOutputContext empty() {
		this.empty = true;
		return this;
	}

	public interface HierarchicalContextProvider {
		public Iterator<XmlTokenOutputContext> contexts();
	}

	@Override
	public String toString() {
		return String.format("Ctx:\nemit: %s\nattr: %s", properties,
				emitAttributes);
	}
}
