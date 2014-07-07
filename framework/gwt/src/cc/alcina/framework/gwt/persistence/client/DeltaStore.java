package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.LoadObjectsResponse;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaLookup;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaMetadata;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaSignature;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaTransport;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.AllStatesConsort;
import cc.alcina.framework.common.client.state.ConsortPlayer.SubconsortSupport;
import cc.alcina.framework.common.client.state.Player;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

@RegistryLocation(registryPoint = DeltaStore.class, implementationType = ImplementationType.SINGLETON)
/*
 * Use one object store so we can have easy transactionality
 */
@ClientInstantiable
public class DeltaStore {
	public static final String CONTENT = "content:";

	public static final String META = "meta:";

	public static DeltaStore get() {
		return Registry.impl(DeltaStore.class);
	}

	public static DomainModelDeltaSignature parseSignature(String key) {
		int offset = key.startsWith(META) ? META.length() : CONTENT.length();
		return DomainModelDeltaSignature.parseSignature(key.substring(offset));
	}

	protected TaggedLogger infoLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(getClass(), TaggedLogger.INFO);

	private DomainModelDeltaLookup cache = null;

	protected PersistenceObjectStore objectStore;

	public void getDelta(DomainModelDeltaSignature sig,
			final AsyncCallback<DomainModelDelta> callback) {
		final String key = getKey(sig, true);
		final AsyncCallback<DomainModelDelta> assignCallback = new AsyncCallbackStd<DomainModelDelta>() {
			@Override
			public void onSuccess(DomainModelDelta result) {
				cache.deltaCache.put(result.getSignature()
						.nonVersionedSignature(), result);
				callback.onSuccess(result);
			}
		};
		AsyncCallback<String> valueCallback = new AsyncCallbackStd<String>() {
			@Override
			public void onSuccess(String result) {
				try {
					if (result == null) {
						throw new RuntimeException("Null content for key "
								+ key);
					}
					Registry.impl(RpcDeserialiser.class).deserialize(
							DomainModelDelta.class, result, assignCallback);
					infoLogger.format("delta store: deserialized %s", key);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		};
		objectStore.get(key, valueCallback);
	}

	public void getDeltaForNonVersionSignature(DomainModelDeltaSignature sig,
			final AsyncCallback<DomainModelDelta> callback) {
		DomainModelDeltaSignature versioned = cache.nonVersionedSignatures
				.get(sig.nonVersionedSignature());
		if (versioned == null) {
			callback.onFailure(new RuntimeException("No cache entry for " + sig));
		}
		getDelta(versioned, callback);
	}

	public DomainModelDeltaMetadata getDomainObjectsMetadata() {
		if (cache == null) {
			return null;
		}
		CollectionFilter<DomainModelDeltaMetadata> hasDomainObjectsFilter = new CollectionFilter<DomainModelDeltaMetadata>() {
			@Override
			public boolean allow(DomainModelDeltaMetadata o) {
				return o.isDomainObjectsFieldSet();
			}
		};
		return CollectionFilters.first(cache.metadataCache.values(),
				hasDomainObjectsFilter);
	}

	public List<String> getExistingDeltaSignatures() {
		return cache == null ? null : cache.versionedSignatures;
	}

	public String getKey(DomainModelDeltaSignature sig, boolean content) {
		return (content ? CONTENT : META) + sig.toString();
	}

	public DomainModelDeltaMetadata getMetadata(DomainModelDeltaSignature sig) {
		return cache.metadataCache.get(sig);
	}

	public String getTableName() {
		return objectStore.getTableName();
	}

	public Long getUserId() {
		if (cache == null) {
			return null;
		}
		DomainModelDeltaSignature sig = CommonUtils.first(cache.metadataCache
				.keySet());
		return sig == null ? null : (Long) sig.getUserId();
	}

	public void mergeResponse(final LoadObjectsResponse response,
			boolean deserializeTranches, boolean removeUnusedTranches,
			final AsyncCallback<Void> callback) {
		MergeResponseConsort mergeResponseConsort = new MergeResponseConsort(
				response, deserializeTranches, removeUnusedTranches, callback);
		new SubconsortSupport().maybeAttach(callback, mergeResponseConsort,
				false);
	}

	public void refreshCache(AsyncCallback callback) {
		EnsureCacheConsort ensureCacheConsort = new EnsureCacheConsort(callback);
		if (callback instanceof Player) {
			Player player = (Player) callback;
			new SubconsortSupport().run(player.getConsort(),
					ensureCacheConsort, player);
		} else {
			ensureCacheConsort.start();
		}
	}

	public void registerDelegate(PersistenceObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	private void persistTranches(
			List<DomainModelDeltaTransport> deltaTransports,
			AsyncCallback callback) {
		StringMap out = new StringMap();
		for (DomainModelDeltaTransport transport : deltaTransports) {
			if (!transport.provideIsCacheReference()) {
				DomainModelDeltaSignature sig = DomainModelDeltaSignature
						.parseSignature(transport.getSignature());
				out.put(getKey(sig, false), transport.getMetadataJson());
				out.put(getKey(sig, true), transport.getSerializedDelta());
				cache.deltaCache.put(sig.nonVersionedSignature(),
						transport.getDelta());
				cache.contentCache.put(sig, transport.getSerializedDelta());
				cache.addSignature(sig.toString());
			}
		}
		objectStore.put(out, callback);
	}

	void removeUnusedTranches(List<String> preserveClientDeltaSignatures,
			final AsyncCallback callback) {
		List<String> toRemove = cache.existingKeys;
		cache = null;
		Set<String> preserveKeys = new LinkedHashSet<String>();
		for (String signature : preserveClientDeltaSignatures) {
			preserveKeys.add(getKey(
					DomainModelDeltaSignature.parseSignature(signature), true));
			preserveKeys
					.add(getKey(
							DomainModelDeltaSignature.parseSignature(signature),
							false));
		}
		toRemove.removeAll(preserveKeys);
		AsyncCallback refreshAfterRemoveCallback = new AsyncCallback() {
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(Object result) {
				refreshCache(callback);
			}
		};
		objectStore.remove(toRemove, refreshAfterRemoveCallback);
	}

	class EnsureCacheConsort extends AllStatesConsort<EnsureCachePhase> {
		DomainModelDeltaLookup newCache = new DomainModelDeltaLookup();

		public EnsureCacheConsort(AsyncCallback callback) {
			super(EnsureCachePhase.class, callback);
		}

		@Override
		public void runPlayer(AllStatesPlayer player, EnsureCachePhase next) {
			if (cache != null) {
				wasPlayed(player, Collections.singleton(EnsureCachePhase.MERGE));
				return;
			}
			switch (next) {
			case LIST_KEYS:
				objectStore.getKeysPrefixedBy("", player);
				break;
			case LIST_METADATA:
				List<String> queryKeys = new ArrayList<String>();
				newCache.existingKeys = (List<String>) lastCallbackResult;
				for (String sigWithMarker : newCache.existingKeys) {
					if (sigWithMarker.startsWith(META)) {
						String signatureString = sigWithMarker.substring(META
								.length());
						newCache.addSignature(signatureString);
						queryKeys.add(sigWithMarker);
					}
				}
				objectStore.get(queryKeys, player);
				break;
			case MERGE:
				StringMap mdKvs = (StringMap) lastCallbackResult;
				try {
					for (Entry<String, String> e : mdKvs.entrySet()) {
						DomainModelDeltaSignature signature = DomainModelDeltaSignature
								.parseSignature(e.getKey().substring(
										META.length()));
						newCache.metadataCache.put(
								signature,
								(DomainModelDeltaMetadata) Registry.impl(
										AlcinaBeanSerializer.class)
										.deserialize(e.getValue()));
					}
					cache = newCache;
					wasPlayed(player);
				} catch (Exception e) {
					Registry.impl(ClientNotifications.class).log(
							"Problem deserialising delta store - "
									+ e.getMessage());
					GWT.log("Problem deserialising delta store - ", e);
					objectStore.clear(player);
					cache = new DomainModelDeltaLookup();
				}
				break;
			}
		}
	}

	enum EnsureCachePhase {
		LIST_KEYS, LIST_METADATA, MERGE
	}

	class MergeResponseConsort extends AllStatesConsort<MergeResponsePhase> {
		private LoadObjectsResponse response;

		// TODO j5 - implement deser phase
		private boolean deserializeTranches;

		private boolean removeUnusedTranches;

		public MergeResponseConsort(LoadObjectsResponse response,
				boolean deserializeTranches, boolean removeUnusedTranches,
				AsyncCallback<Void> callback) {
			super(MergeResponsePhase.class, callback);
			this.response = response;
			this.deserializeTranches = deserializeTranches;
			this.removeUnusedTranches = removeUnusedTranches;
		}

		@Override
		public void runPlayer(AllStatesPlayer player, MergeResponsePhase next) {
			switch (next) {
			case ENSURE_CACHE:
				new SubconsortSupport().maybeAttach(player,
						new EnsureCacheConsort(player), false);
				break;
			case REMOVE_UNUSED:
				if (removeUnusedTranches) {
					removeUnusedTranches(
							response.getPreserveClientDeltaSignatures(), player);
				} else {
					player.onSuccess(null);
				}
				break;
			case PERSIST_TRANCHES:
				persistTranches(response.getDeltaTransports(), player);
				break;
			case DESERIALIZE_TRANCHES:
				if (deserializeTranches) {
					deserializeTranches(player, response.getDeltaTransports());
				} else {
					player.onSuccess(null);
				}
				break;
			case RELOAD_CACHE:
				player.stateConsort = new EnsureCacheConsort(player);
				new SubconsortSupport().run(this, player.stateConsort, player);
				break;
			default:
				break;
			}
		}
	}

	enum MergeResponsePhase {
		ENSURE_CACHE, REMOVE_UNUSED, PERSIST_TRANCHES, DESERIALIZE_TRANCHES,
		RELOAD_CACHE
	}

	public void clear(final AsyncCallback callback) {
		AsyncCallback removeCallback = new AsyncCallback() {
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(Object result) {
				removeUnusedTranches(new ArrayList<String>(), callback);
			}
		};
		refreshCache(removeCallback);
	}

	public void deserializeTranches(final AsyncCallback playerCallback,
			final List<DomainModelDeltaTransport> transports) {
		for (DomainModelDeltaTransport transport : transports) {
			DomainModelDeltaSignature sig = DomainModelDeltaSignature
					.parseSignature(transport.getSignature());
			String nvs = sig.nonVersionedSignature();
			DomainModelDeltaSignature versioned = cache.nonVersionedSignatures
					.get(nvs);
			if (cache.deltaCache.get(nvs) == null
					&& !hasNoSerializedContent(nvs)) {
				AsyncCallback<DomainModelDelta> loopCallback = new AsyncCallback<DomainModelDelta>() {
					@Override
					public void onSuccess(DomainModelDelta result) {
						deserializeTranches(playerCallback, transports);
					}

					@Override
					public void onFailure(Throwable caught) {
						playerCallback.onFailure(caught);
					}
				};
				getDelta(versioned, loopCallback);
				return;
			}
		}
		playerCallback.onSuccess(null);
	}

	private boolean hasNoSerializedContent(String nonVersionedKey) {
		return cache.hasNoSerializedContent(nonVersionedKey);
	}

	public DomainModelDelta getDeltaSync(DomainModelDeltaSignature sig) {
		return cache.deltaCache.get(sig.nonVersionedSignature());
	}

	public DomainModelDeltaSignature getExistingVersionedSignature(
			DomainModelDeltaSignature sig) {
		return cache.nonVersionedSignatures.get(sig.nonVersionedSignature());
	}

	public boolean hasContentFor(DomainModelDeltaSignature sig) {
		return getExistingVersionedSignature(sig) != null
				&& cache.contentCache.get(getExistingVersionedSignature(sig)) != null;
	}
}
