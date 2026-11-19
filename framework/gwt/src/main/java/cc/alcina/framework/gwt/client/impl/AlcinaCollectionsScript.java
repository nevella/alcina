package cc.alcina.framework.gwt.client.impl;

import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.IntLookup;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.gwtpeer.client.logic.domaintransform.lookup.JavascriptIntLookup;
import cc.alcina.framework.gwtpeer.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.gwtpeer.client.logic.domaintransform.lookup.JsUniqueSet;

public class AlcinaCollectionsScript {
	public static void register() {
		AlcinaCollections.extension = new ExtensionsImpl();
	}

	static class ExtensionsImpl implements AlcinaCollections.Extension {
		@Override
		public <T> Set<T> newUniqueSet() {
			return JsUniqueSet.create();
		}

		public <K, V> Map<K, V> newUniqueMap() {
			return JsUniqueMap.create();
		}

		@Override
		public <V> Map<Integer, V> newCoarseIntHashMap() {
			return newUniqueMap();
		}

		@Override
		public <V> IntLookup<V> newIntLookup() {
			return JavascriptIntLookup.create();
		}
	}
}
