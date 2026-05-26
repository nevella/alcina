package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.MutationRecord.DehydratedValue;
import com.google.gwt.storage.client.Storage;

import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;
import cc.alcina.framework.servlet.component.romcom.protocol.StringProtocol.Cache.Entry;

/**
 * <p>
 * This class is the data carrier and implementation support which reduces
 * client/server bandwidth by caching large string constants
 */
@Feature.Ref(Feature_Romcom_Impl._StringProtocol.class)
public class StringProtocol {
	static final int CACHE_THRESHOLD = 10000;

	public static class Cache {
		public String contextPath;

		public Map<Key, Entry> keyEntry = AlcinaCollections.newLinkedHashMap();

		public State clientState;

		@Bean(PropertySource.FIELDS)
		public static final class Key implements TreeSerializable {
			String contextPath;

			String key;

			Key() {
			}

			public Key(String contextPath, String key) {
				this.contextPath = contextPath;
				this.key = key;
			}

			@Override
			public int hashCode() {
				return Objects.hash(contextPath, key);
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof Key) {
					Key o = (Key) obj;
					return CommonUtils.equals(contextPath, o.contextPath, key,
							o.key);
				} else {
					return super.equals(obj);
				}
			}

			@Override
			public String toString() {
				return Ax.format("%s/%s", namespacedContextPath(contextPath),
						key);
			}
		}

		/*
		 * note that contextPath with always start with a slash and not end with
		 * one
		 */
		static String namespacedContextPath(String contextPath) {
			return Ax.format("/%s%s", StringProtocol.class.getSimpleName(),
					contextPath);
		}

		@Bean(PropertySource.FIELDS)
		public static final class Entry implements TreeSerializable {
			public String value;

			public String valueHash;

			public int length;

			public Key key;

			public Entry() {
			}

			public Entry(Key key, String value, String valueHash) {
				this.key = key;
				this.value = value;
				this.valueHash = valueHash;
				this.length = value.length();
			}

			Entry toMetadata() {
				Entry result = new Entry();
				result.key = key;
				result.valueHash = valueHash;
				result.length = length;
				return result;
			}

			@Property.Not
			boolean isMatchesMetadata(Entry serverEntry) {
				return CommonUtils.equals(key, serverEntry.key, valueHash,
						serverEntry.valueHash);
			}
		}

		public Cache(String contextPath) {
			this.contextPath = contextPath;
		}

		public static Cache fromLocalStorage(String contextPath) {
			Cache result = new Cache(contextPath);
			Storage storage = Storage.getLocalStorageIfSupported();
			String contextPathPrefix = namespacedContextPath(contextPath);
			for (int idx = 0; idx < storage.getLength(); idx++) {
				String key = storage.key(idx);
				Ax.out(key);
				if (key.startsWith(contextPathPrefix)) {
					String item = storage.getItem(key);
					Entry entry = FlatTreeSerializer.deserialize(item);
					result.keyEntry.put(entry.key, entry);
				}
			}
			return result;
		}

		public static Cache fromRegistry(String contextPath,
				Class<? extends CacheableStringProvider> providerRegistration) {
			Cache result = new Cache(contextPath);
			Registry.query(providerRegistration).implementations()
					.forEach(provider -> {
						result.addEntry(provider.getKey(), provider.getValue(),
								provider.getValueHash());
					});
			return result;
		}

		Entry addEntry(String keyPart, String value, String valueHash) {
			Key key = new Key(contextPath, keyPart);
			Entry entry = new Entry(key, value, valueHash);
			keyEntry.put(key, entry);
			return entry;
		}

		void persistEntry(Entry entry) {
			Storage.getLocalStorageIfSupported().setItem(entry.key.toString(),
					FlatTreeSerializer.serialize(entry));
		}

		public State toMetadata() {
			State state = new State();
			state.metadataEntries = keyEntry.values().stream()
					.map(Entry::toMetadata).toList();
			return state;
		}

		public boolean isInvalid(Entry entry) {
			if (!keyEntry.containsKey(entry.key)) {
				return true;
			}
			if (!Objects.equals(keyEntry.get(entry.key).valueHash,
					entry.valueHash)) {
				return true;
			}
			return false;
		}

		public Set<Entry> dehydrateMutations(Mutations mutations) {
			Set<Entry> entriesEmitted = AlcinaCollections.newHashSet();
			mutations.domMutations.stream()
					.filter(m -> m.type == MutationRecord.Type.innerMarkup
							&& m.newValue.length() > CACHE_THRESHOLD)
					.forEach(markupMutation -> {
						String newValue = markupMutation.newValue;
						DehydratedValue dehydratedValue = new MutationRecord.DehydratedValue();
						int idx = 0;
						while (idx < newValue.length()) {
							int firstMatchIdx = Integer.MAX_VALUE;
							Entry firstMatchEntry = null;
							for (Entry entry : keyEntry.values()) {
								int matchIdx = newValue.indexOf(entry.value,
										idx);
								if (matchIdx != -1
										&& matchIdx < firstMatchIdx) {
									firstMatchIdx = matchIdx;
									firstMatchEntry = entry;
								}
							}
							if (firstMatchEntry != null) {
								String stringPart = newValue.substring(idx,
										firstMatchIdx);
								dehydratedValue.entries
										.add(DehydratedValue.Entry
												.ofValue(stringPart));
								dehydratedValue.entries
										.add(DehydratedValue.Entry
												.ofCacheKey(firstMatchEntry.key
														.toString()));
								if (clientState
										.containsAndIsValid(firstMatchEntry)) {
									// NOOP - the whole point of this exercise
								} else {
									entriesEmitted.add(firstMatchEntry);
								}
								idx = idx + firstMatchEntry.value.length();
							} else {
								String stringPart = newValue.substring(idx);
								dehydratedValue.entries
										.add(DehydratedValue.Entry
												.ofValue(stringPart));
								break;
							}
						}
						if (dehydratedValue.entries.size() > 1) {
							markupMutation.newValue = null;
							dehydratedValue.entries
									.removeIf(DehydratedValue.Entry::isEmpty);
							markupMutation.newValueDehydrated = dehydratedValue;
						}
					});
			return entriesEmitted;
		}

		public void hydrateMutations(Mutations mutations) {
			if (mutations.stringProtocolDelta != null) {
				Storage localStorage = Storage.getLocalStorageIfSupported();
				mutations.stringProtocolDelta.remove.forEach(key -> {
					keyEntry.remove(key);
					localStorage.removeItem(key.toString());
				});
				mutations.stringProtocolDelta.add.forEach(entry -> {
					keyEntry.put(entry.key, entry);
					persistEntry(entry);
				});
			}
			mutations.domMutations.stream()
					.filter(m -> m.newValueDehydrated != null).forEach(
							markupMutation -> markupMutation.newValue = hydrateValue(
									markupMutation.newValueDehydrated));
		}

		String hydrateValue(DehydratedValue dehydratedValue) {
			StringBuilder builder = new StringBuilder();
			dehydratedValue.entries.forEach(e -> {
				if (e.value != null) {
					builder.append(e.value);
				} else {
					String cachedValue = keyEntry.values().stream()
							.filter(ke -> ke.key.toString().equals(e.cacheKey))
							.findFirst().get().value;
					builder.append(cachedValue);
				}
			});
			return builder.toString();
		}
	}

	@Bean(PropertySource.FIELDS)
	public static final class Delta {
		public List<Cache.Key> remove = new ArrayList<>();

		public List<Cache.Entry> add = new ArrayList<>();
	}

	@Bean(PropertySource.FIELDS)
	public static final class State {
		public List<Cache.Entry> metadataEntries = new ArrayList<>();

		public boolean containsAndIsValid(Entry serverEntry) {
			return metadataEntries.stream()
					.anyMatch(e -> e.isMatchesMetadata(serverEntry));
		}

		public String toMetadataString() {
			return metadataEntries.stream()
					.map(FlatTreeSerializer::serializeSingleLine)
					.collect(Collectors.joining("\n"));
		}
	}

	@NonClientRegistryPointType
	public interface CacheableStringProvider {
		String getKey();

		String getValueHash();

		String getValue();

		/*
		 * the default, no cacheable strings
		 */
		public interface None extends CacheableStringProvider {
		}
	}
}
