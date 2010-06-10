package cc.alcina.template.server;

import java.io.File;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolderProvider;

import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.entityaccess.AlcinaTemplateBeanProvider;


public class AlcinaTemplateServerManager implements
		DomainModelHolderProvider<AlcinaTemplateObjects>,
		CommonPersistenceProvider {
	private AlcinaTemplateObjects currentAlcinaTemplateObjects;

	public AlcinaTemplateObjects getDomainModelHolder() {
		if (currentAlcinaTemplateObjects == null) {
			currentAlcinaTemplateObjects = loadInitial(true);
		}
		return currentAlcinaTemplateObjects;
	}

	public CommonPersistenceLocal getCommonPersistence() {
		return AlcinaTemplateBeanProvider.get().getCommonPersistenceBean();
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

	private AlcinaTemplateServerManager() {
		super();
	}

	private static AlcinaTemplateServerManager theInstance;

	public static AlcinaTemplateServerManager get() {
		if (theInstance == null) {
			theInstance = new AlcinaTemplateServerManager();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
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
