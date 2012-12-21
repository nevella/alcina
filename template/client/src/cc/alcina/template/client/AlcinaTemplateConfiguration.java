package cc.alcina.template.client;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.RegistryPermissionsExtension;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientConfiguration;
import cc.alcina.framework.gwt.client.ClientConfigurationMachine;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.StandardActionLogProvider;
import cc.alcina.framework.gwt.client.ide.WorkspaceDeletionChecker;
import cc.alcina.framework.gwt.client.ide.node.CollectionRenderingSupport;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider.ContentProviderSource;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;
import cc.alcina.framework.gwt.persistence.client.PersistenceStateHandlers.LocalPersistenceInitHandler;
import cc.alcina.template.client.logic.AlcinaTemplateContentProvider;
import cc.alcina.template.cs.AlcinaTemplateHistory;

import com.google.gwt.user.client.History;

public class AlcinaTemplateConfiguration extends ClientConfiguration {
	@Override
	protected void initExceptionHandling() {
		ClientLayerLocator.get().registerExceptionHandler(
				new AlcinaTemplateExceptionHandler());
	}

	@Override
	protected void initContentProvider() {
		ContentProvider.registerProvider(new AlcinaTemplateContentProvider());
	}

	@Override
	protected void createMachine() {
		super.createMachine();
		machine.registerTransitionHandler(ClientConfigurationMachine.localPersistenceInit, null,
				new LocalPersistenceInitHandler(ClientConfigurationMachine.localPersistenceInitialised));
	}

	

	@Override
	protected void extraConfiguration() {
		ClientLayerLocator.get().registerActionLogProvider(
				new StandardActionLogProvider());
		AlcinaTemplateHistory history = AlcinaTemplateHistory.get();
		History.addValueChangeHandler(history);
		AlcinaHistory.register(history);
		CollectionRenderingSupport.REDRAW_CHILDREN_ON_ORDER_CHANGE = true;
		PermissionsManager
				.setPermissionsExtension(new RegistryPermissionsExtension(
						Registry.get()));
		BreadcrumbBar.asHTML = true;
		WorkspaceDeletionChecker.enabled = true;
		ContentProvider.registerProvider(new ContentProviderSource() {
			public void refresh() {
			}

			public String getContent(String key) {
				if ("blurb".equals(key)) {
					return "<div class='home-text'>"
							+ "Getting started: <ul>"
							+ "<li>Login as admin/admin</li>"
							+ "<li>create a few bookmarks</li>"
							+ "<li>and play with offline (if your browser supports html5 appcache and webdb)"
							+ "<br> - by shutting down the app server, reloading the page "
							+ " <br> - everything should work except setting user passwords "
							+ " - and searching the domain transform log</li>"
							+ "</ul></div>";
				}
				return null;
			}
		});
	}

	@Override
	protected void initHandshakeHelper() {
		ClientLayerLocator.get().setClientHandshakeHelper(
				new AlcinaTemplateHandshakeHelper());
	}
}
