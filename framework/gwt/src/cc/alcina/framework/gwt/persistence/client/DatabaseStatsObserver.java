package cc.alcina.framework.gwt.persistence.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializerC;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.OnetimeWrappingAsyncCallback;
import cc.alcina.framework.gwt.client.util.WrappingAsyncCallback;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence.TypeSizeTuple;

@RegistryLocation(registryPoint = DatabaseStatsObserver.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class DatabaseStatsObserver {
	public static final transient String SERIALIZED_MAX_KEY = CommonUtils
			.simpleClassName(DatabaseStatsObserver.class)
			+ ".SERIALIZED_MAX_KEY";

	private static final int PERSISTENCE_VERSION = 2;

	DatabaseStatsInfo max;

	DatabaseStatsInfo current = new DatabaseStatsInfo();

	protected TopicListener<TypeSizeTuple> transformDeltaListener = new TopicListener<LocalTransformPersistence.TypeSizeTuple>() {
		@Override
		public void topicPublished(String key, TypeSizeTuple message) {
			if (current != null) {
				current.getTransformCounts().add(message.type);
				current.getTransformTexts().add(message.type, message.size);
				checkMax();
			}
		}
	};

	protected TopicListener<IntPair> logStorePersistedListener = new TopicListener<IntPair>() {
		@Override
		public void topicPublished(String key, IntPair idSize) {
			if (current != null) {
				current.getLogSizes().add(idSize.i1, idSize.i2);
				checkMax();
			}
		}
	};

	protected TopicListener<Void> logStoreDeletedListener = new TopicListener<Void>() {
		@Override
		public void topicPublished(String key, Void message) {
			refreshCurrent();
		}
	};

	WrappingAsyncCallback<String> initCallback = new OnetimeWrappingAsyncCallback<String>() {
		@Override
		protected void onSuccess0(String result) {
			try {
				if (result != null) {
					max = Registry.impl(AlcinaBeanSerializer.class)
							.deserialize(result);
				}
			} catch (Exception e) {
				GWT.log("Problem deserialising " + result, e);
			}
		}
	};

	private WrappingAsyncCallback<DatabaseStatsInfo> currentCallback = new OnetimeWrappingAsyncCallback<DatabaseStatsInfo>() {
		public void onFailure(Throwable caught) {
			refreshing = false;
			super.onFailure(caught);
		}

		@Override
		protected void onSuccess0(DatabaseStatsInfo result) {
			refreshing = false;
			current = result;
			checkMax();
		};
	};

	private AsyncCallback<Integer> persistedCallback = new AsyncCallbackStd<Integer>() {
		@Override
		public void onSuccess(Integer result) {
			// nada
		}
	};

	private boolean refreshing;

	public DatabaseStatsObserver() {
	}

	public String getReport() {
		if (max == null) {
			max = current;
		}
		return CommonUtils.formatJ("Database usage report:\nCurrent:\n"
				+ "********\n%s\n\nMax:\n*****\n%s\n", current, max);
	}

	public void init(AsyncCallback<String> notifyOnInitCallback) {
		initCallback.wrapped = notifyOnInitCallback;
		KeyValueStore.get().get(SERIALIZED_MAX_KEY, initCallback);
	}

	public void installPersistenceListeners() {
		LocalTransformPersistence
				.notifyPersistingListenerDelta(transformDeltaListener, true);
		LogStore.notifyPersistedListenerDelta(logStorePersistedListener, true);
		LogStore.notifyDeletedListenerDelta(logStoreDeletedListener, true);
	}

	public void recalcWithListener(AsyncCallback postRecalcCallback) {
		currentCallback.wrapped = postRecalcCallback;
		refreshCurrent();
	}

	public void refreshCurrent() {
		if (!refreshing) {
			current = null;
			refreshing = true;
			new DatabaseStatsCollector().run(currentCallback);
		}
	}

	protected void checkMax() {
		if (current.greaterSizeThan(max)
				|| max.getVersion() < PERSISTENCE_VERSION) {
			max = current;
			max.setVersion(PERSISTENCE_VERSION);
			persistMax();
		}
	}

	protected void persistMax() {
		String ser = Registry.impl(AlcinaBeanSerializer.class).serialize(max);
		KeyValueStore.get().put(SERIALIZED_MAX_KEY, ser, persistedCallback);
	}
}
