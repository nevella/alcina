package cc.alcina.appcreator;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

public class CheckConfig extends Task {
	@Override
	public void execute() throws BuildException {
		setProperty(Constants.APP_NAME_LC, getProperty(Constants.APP_NAME)
				.toLowerCase());
		setProperty(Constants.APP_NAME_CC, PackageUtils
				.camelCase(getProperty(Constants.APP_NAME)));
		setProperty(Constants.APP_ROOT_PACKAGE_DIR, getProperty(
				Constants.APP_PACKAGE).replace('.', '/'));
		try {
			setProperty(Constants.ALCINA_HOME_ABS, new File(PackageUtils.translatePath(getProperty(Constants.ALCINA_HOME)))
					.getCanonicalPath());
			setProperty(Constants.DEPLOY_JBOSS_DIR_ABS, new File(PackageUtils.translatePath(getProperty(Constants.DEPLOY_JBOSS_DIR)))
					.getCanonicalPath());
			setProperty(Constants.GWT_SDK_DIR_ABS, new File(PackageUtils.translatePath(getProperty(Constants.GWT_SDK_DIR)))
					.getCanonicalPath());
			setProperty(Constants.ALCINA_HOME_ABS_FSLASH, new File(PackageUtils.translatePath(getProperty(Constants.ALCINA_HOME)))
			.getCanonicalPath().replace('\\', '/'));
	setProperty(Constants.DEPLOY_JBOSS_DIR_ABS_FSLASH, new File(PackageUtils.translatePath(getProperty(Constants.DEPLOY_JBOSS_DIR)))
			.getCanonicalPath().replace('\\', '/'));
	setProperty(Constants.GWT_SDK_DIR_ABS_FSLASH, new File(PackageUtils.translatePath(getProperty(Constants.GWT_SDK_DIR)))
			.getCanonicalPath().replace('\\', '/'));
		} catch (IOException e) {
			throw new BuildException(e);
		}
		boolean jbossExists = new File(PackageUtils.translatePath(getProperty(Constants.DEPLOY_JBOSS_DIR)))
				.exists();
		if (jbossExists) {
			setProperty(Constants.DEPLOY_JBOSS_DIR_EXISTS, Boolean
					.toString(true));
		}
		boolean jbossRootAppExists = new File(PackageUtils.translatePath(getProperty(Constants.DEPLOY_JBOSS_DIR)
						+ "/server/all/deploy/ROOT.war")).exists();
		if (jbossExists&&!jbossRootAppExists) {
			setProperty(Constants.DEPLOY_JBOSS_CONFIGURED, Boolean
					.toString(true));
		}
	}

	private void setProperty(String name, String value) {
		getProject().setProperty(name, value);
	}

	private String getProperty(String name) {
		return getProject().getProperty(name);
	}
}
