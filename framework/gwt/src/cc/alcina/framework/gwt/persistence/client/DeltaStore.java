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

	private DomainModelDeltaLookup cache = null;

	protected PersistenceObjectStore objectStore;

	public void getDelta(DomainModelDeltaSignature sig,
			final AsyncCallback<DomainModelDelta> callback) {
		final String key = getKey(sig, true);
		AsyncCallback<String> valueCallback = new AsyncCallbackStd<String>() {
			@Override
			public void onSuccess(String result) {
				try {
					if (result == null) {
						throw new RuntimeException("Null content for key "
								+ key);
					}
					Registry.impl(RpcDeserialiser.class).deserialize(
							DomainModelDelta.class, result, callback);
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
			final AsyncCallback<Void> callback) {
		MergeResponseConsort mergeResponseConsort = new MergeResponseConsort(
				response, callback);
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
				out.put(getKey(DomainModelDeltaSignature
						.parseSignature(transport.getSignature()), false),
						transport.getMetadataJson());
				out.put(getKey(DomainModelDeltaSignature
						.parseSignature(transport.getSignature()), true),
						transport.getSerializedDelta());
			}
		}
		objectStore.put(out, callback);
	}

	void removeUnusedTranches(List<String> preserveClientDeltaSignatures,
			AsyncCallback callback) {
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
		objectStore.remove(toRemove, callback);
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
						newCache.metadataCache
								.put(signature,
										(DomainModelDeltaMetadata) new AlcinaBeanSerializer()
												.deserialize(e.getValue()));
					}
					cache = newCache;
					wasPlayed(player);
				} catch (Exception e) {
					Registry.impl(ClientNotifications.class).log(
							"Problem deserialising delta store - "
									+ e.getMessage());
					e.printStackTrace();
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

		public MergeResponseConsort(LoadObjectsResponse response,
				AsyncCallback<Void> callback) {
			super(MergeResponsePhase.class, callback);
			this.response = response;
		}

		@Override
		public void runPlayer(AllStatesPlayer player, MergeResponsePhase next) {
			switch (next) {
			case ENSURE_CACHE:
				new SubconsortSupport().maybeAttach(player,
						new EnsureCacheConsort(player), false);
				break;
			case REMOVE_UNUSED:
				removeUnusedTranches(
						response.getPreserveClientDeltaSignatures(), player);
				break;
			case PERSIST_TRANCHES:
				persistTranches(response.getDeltaTransports(), player);
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
		ENSURE_CACHE, REMOVE_UNUSED, PERSIST_TRANCHES, RELOAD_CACHE
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
}
