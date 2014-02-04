package cc.alcina.template.servlet;

import java.io.File;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.RegistryPermissionsExtension;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.impl.jboss.JBoss7Support;
import cc.alcina.framework.entity.impl.jboss.JPAHibernateImpl;
import cc.alcina.framework.entity.impl.jboss.JbossLogMuter;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.entityaccess.AlcinaTemplateBeanProvider;
import cc.alcina.template.entityaccess.AlcinaTemplatePersistenceLocal;

public class AlcinaTemplateAppLifecycleServlet extends AppLifecycleServletBase {
	public AlcinaTemplateAppLifecycleServlet() {
	}

	

	@Override
	protected void initBootstrapRegistry() {
		super.initBootstrapRegistry();
		Registry.registerSingleton(JPAImplementation.class, new JPAHibernateImpl());
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
		ServletLayerObjects.get().setDataFolder(
				AlcinaTemplateServerManager.get().getDataFolder());
		EntityLayerObjects.get().setDataFolder(
				AlcinaTemplateServerManager.get().getDataFolder());
	}

	@Override
	protected void initCommonImplServices() {
		AlcinaTemplateObjects.registerProvider(AlcinaTemplateServerManager
				.get());
		PermissionsManager
				.setPermissionsExtension(new RegistryPermissionsExtension(
						Registry.get()));
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
		JBoss7Support.install();
	}

	@Override
	protected void initCustomServices() {
	}
}
