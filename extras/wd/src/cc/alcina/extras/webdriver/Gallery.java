package cc.alcina.extras.webdriver;

import java.io.File;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.RemoteWebDriver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;

@SuppressWarnings("unused")
public class Gallery {
	private static Gallery instance;

	private String appName;

	private String userAgentType;

	private File base;

	private RemoteWebDriver driver;

	public static void end() {
		instance.end0();
		instance = null;
	}

	private void end0() {
		// send off to googhaus
	}

	public static void begin(String appName, String userAgentType) {
		instance = new Gallery(appName, userAgentType);
	}

	private Gallery(String appName, String userAgentType) {
		this.appName = appName;
		this.userAgentType = userAgentType;
		Gallery.instance = this;
		base = new File(
				Ax.format("%s/%s/%s", ResourceUtilities.get("defaultLocalPath"),
						appName, userAgentType));
		base.mkdirs();
	}

	public static void snap(String snapName) {
		instance.snap0(snapName);
	}

	private void snap0(String snapName) {
		File toFile = SEUtilities.getChildFile(base, snapName + ".png");
		byte[] bytes = ((RemoteWebDriver) driver)
				.getScreenshotAs(OutputType.BYTES);
		try {
			ResourceUtilities.writeBytesToFile(bytes, toFile);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void putDriver(RemoteWebDriver driver) {
		if (instance != null) {
			instance.driver = driver;
		}
	}
}
