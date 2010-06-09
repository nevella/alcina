package cc.alcina.appcreator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class CheckConfig extends Task {
	@Override
	public void execute() throws BuildException {
		getProject().setProperty(Constants.APP_NAME_LC,
				getProject().getProperty(Constants.APP_NAME).toLowerCase());
		getProject().setProperty(Constants.APP_NAME_CC,PackageUtils.camelCase(
				getProject().getProperty(Constants.APP_NAME)));
		getProject().setProperty(
				Constants.APP_ROOT_PACKAGE_DIR,
				getProject().getProperty(Constants.APP_PACKAGE).replace('.',
						'/'));
	}
}
