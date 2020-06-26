package cc.alcina.extras.dev.console.remote.client.module.index;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.gwt.client.lux.LuxModule;

public class IndexModule {
	private static IndexModule indexModule;

	public static void ensure() {
		if (indexModule == null) {
			indexModule = new IndexModule();
		}
	}

	public static void focusNavbarSearch() {
		ensure();
	}

	public IndexResources resources = GWT.create(IndexResources.class);

	private IndexModule() {
		LuxModule.get().interpolateAndInject(resources.indexStyles());
	}
}
