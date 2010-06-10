package cc.alcina.template.servlet;

import java.io.File;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.RegistryPermissionsExtension;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.impl.jboss.JBossSupport;
import cc.alcina.framework.entity.impl.jboss.JPAHibernateImpl;
import cc.alcina.framework.entity.impl.jboss.JbossLogMuter;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;
import cc.alcina.framework.servlet.ServletLayerLocator;
import cc.alcina.framework.servlet.ServletLayerRegistry;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;
import cc.alcina.template.cs.constants.AlcinaTemplateImplLookup;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.entityaccess.AlcinaTemplateBeanProvider;
import cc.alcina.template.entityaccess.AlcinaTemplatePersistenceLocal;

public class AlcinaTemplateAppLifecycleServlet extends AppLifecycleServletBase {
	public AlcinaTemplateAppLifecycleServlet() {
	}

	/*
	 * @see javax.servlet.Servlet#destroy()
	 */
	public void destroy() {
		try {
			ResourceUtilities.singleton().appShutdown();
			AlcinaTemplatePersistenceLocal jpb = AlcinaTemplateBeanProvider
					.get().getAlcinaTemplatePersistenceBean();
			jpb.destroy();
			ServletLayerRegistry.get().appShutdown();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@Override
	protected void initNames() {
		AlcinaServerConfig.get().setApplicationName("AlcinaTemplate");
		AlcinaServerConfig.get().setMainLoggerName("cc.alcina.template");
		AlcinaServerConfig.get().setCustomPropertiesFilePath(
				new JPAHibernateImpl().getConfigDirectory().getPath()
						+ File.separator + "AlcinaTemplate-server.properties");
		AlcinaServerConfig.get().setMetricLoggerName(
				"cc.alcina.template.metric.server");
		AlcinaServerConfig.get().setDatabaseEventLoggerName(
				"cc.alcina.template.server.persistentlog");
	}

	@Override
	protected void initDataFolder() {
		ServletLayerLocator.get().setDataFolder(
				AlcinaTemplateServerManager.get().getDataFolder());
		EntityLayerLocator.get().setDataFolder(
				AlcinaTemplateServerManager.get().getDataFolder());
	}

	@Override
	protected void initCommonImplServices() {
		CommonLocator.get().registerImplementationLookup(
				new AlcinaTemplateImplLookup());
		ServletLayerLocator.get().registerRemoteActionLoggerProvider(
				new RemoteActionLoggerProvider());
		ServletLayerLocator.get().registerCommonPersistenceProvider(
				AlcinaTemplateServerManager.get());
		AlcinaTemplateObjects.registerProvider(AlcinaTemplateServerManager
				.get());
		PermissionsManager
				.setPermissionsExtension(new RegistryPermissionsExtension(
						ServletLayerRegistry.get()));
	}

	@Override
	protected void initCustom() {
		new JbossLogMuter().run();
	}

	@Override
	protected void initEntityLayer() throws Exception {
		AlcinaTemplatePersistenceLocal appPersistence = AlcinaTemplateBeanProvider
				.get().getAlcinaTemplatePersistenceBean();
		appPersistence.init();
	}

	@Override
	protected void initJPA() {
		JBossSupport.install();
	}

	@Override
	protected void initCustomServices() {
	}
}
