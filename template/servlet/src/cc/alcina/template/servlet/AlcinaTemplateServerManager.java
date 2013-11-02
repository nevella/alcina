package cc.alcina.template.servlet;

import java.io.File;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolderProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.entityaccess.AlcinaTemplateBeanProvider;
@RegistryLocation(registryPoint=AlcinaTemplateServerManager.class)
public class AlcinaTemplateServerManager implements
		DomainModelHolderProvider<AlcinaTemplateObjects> {
	private AlcinaTemplateObjects currentAlcinaTemplateObjects;

	public AlcinaTemplateObjects getDomainModelHolder() {
		if (currentAlcinaTemplateObjects == null) {
			currentAlcinaTemplateObjects = loadInitial(true);
		}
		return currentAlcinaTemplateObjects;
	}

	public AlcinaTemplateObjects loadInitial(boolean internal) {
		try {
			AlcinaTemplateObjects ijo = AlcinaTemplateBeanProvider.get()
					.getAlcinaTemplatePersistenceBean().loadInitial(internal);
			ijo.setHomepageHtml("Hello world!");
			return ijo;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static AlcinaTemplateServerManager get() {
		return Registry.impl(AlcinaTemplateServerManager.class);
	}

	public File getDataFolder() {
		String testStr = "";
		String homeDir = (System.getenv("USERPROFILE") != null) ? System
				.getenv("USERPROFILE") : System.getProperty("user.home");
		File file = new File(homeDir + File.separator + ".alcina" + testStr
				+ File.separator + "alcinaTemplate-server");
		file.mkdirs();
		return file;
	}
}
