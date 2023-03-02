package cc.alcina.extras.webdriver.gallery;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.Document;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.JaxbUtils;

/**
 * <p>
 * The Gallery is a an extensible system for building a series of webapp UI
 * snapshots for development and testing. It currently uploads images and a
 * simple html viewer to GDrive, and stores snapshot metadata in a GSheet
 *
 * <h2>Gallery configuration and snapshotting (code)</h2>
 *
 * <p>
 * See {@link GalleryConfiguration} for configuration schema details <div>
 *
 * <pre>
 * <code>
 *
 * <h3>Devconsole properties (in addition to the persistence configuration xml):</h3>
GalleryPersister.persistToGoogle=true
Gallery.defaultLocalPath=/tmp/barpub/gallery
Gallery.preSnapPause=200
Gallery.snap=true
 *
 *
//set up the gallery
 *
Gallery.begin(
"my-app", "desktop",
//the configuration file is a jaxb/xml serialized instance of GalleryConfiguration
"/tmp/gallery-configuration.xml");
Gallery.putDriver((RemoteWebDriver) token.getWebDriver());

	...

//take snapshots (in this case using a TourWd json tour test series)
 *
protected void onStepRendered(Step step) {
List<? extends PopupInfo> popups = step.providePopups();
if (popups.size() > 0) {
	Gallery.snap(popups.get(0).getCaption());
	try {
		Thread.sleep(200);
	} catch (Exception e) {
		throw new WrappedRuntimeException(e);
	}
}
}

...

//upload etc if so configured

Gallery.end();
 *
 *
 * </code>
 * </pre>
 *
 * </div>
 *
 *
 *
 *
 *
 * @author nick@alcina.cc
 *
 */
public class Gallery {
	public static final String DEVICE = "device";

	public static final String SNAP = "snap";

	private static ThreadLocal<Gallery> gallery = new ThreadLocal<>();

	public static void begin(String appName, String userAgentType,
			URL configurationUrl) {
		userAgentType = userAgentType.toLowerCase();
		gallery.set(new Gallery(appName, userAgentType, configurationUrl));
		instance().initialise();
	}

	public static void end() {
		instance().end0();
		gallery.remove();
	}

	public static boolean isInitialised() {
		return instance() != null;
	}

	public static void putDriver(RemoteWebDriver driver) {
		if (instance() != null) {
			instance().driver = driver;
			instance().setWidth();
		}
	}

	public static void snap(String snapName) {
		if (instance() != null && Configuration.key(SNAP).is()) {
			instance().snap0(snapName);
		}
	}

	static Gallery instance() {
		return gallery.get();
	}

	private String appName;

	private String userAgentType;

	private File base;

	private RemoteWebDriver driver;

	private URL configurationUrl;

	private GalleryConfiguration galleryConfiguration;

	private GalleryConfiguration.Element configuration;

	private Gallery(String appName, String userAgentType,
			URL configurationUrl) {
		this.appName = appName;
		this.userAgentType = userAgentType;
		this.configurationUrl = configurationUrl;
	}

	private void end0() {
		if (galleryConfiguration != null) {
			try {
				new GalleryPersister().persist(base, configuration,
						userAgentType);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private void initialise() {
		base = new File(
				Ax.format("%s/%s/%s", Configuration.get("defaultLocalPath"),
						appName, userAgentType));
		SEUtilities.deleteDirectory(base);
		base.mkdirs();
		if (this.configurationUrl != null) {
			try {
				String configurationXml = ResourceUtilities
						.readStreamToString(this.configurationUrl.openStream());
				this.galleryConfiguration = JaxbUtils.xmlDeserialize(
						GalleryConfiguration.class, configurationXml);
				configuration = this.galleryConfiguration.find(appName);
				if (configuration == null) {
					throw Ax.runtimeException("No configuration with name '%s'",
							appName);
				}
			} catch (Exception e) {
				GalleryConfiguration.dumpSampleConfiguration();
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private void setWidth() {
		if (userAgentType != null) {
			Dimension dim = null;
			Device device = CommonUtils.getEnumValueOrNull(Device.class,
					userAgentType, true, Device.DESKTOP);
			switch (device) {
			case DESKTOP:
				dim = new Dimension(1700, 1200);
				break;
			case TABLET:
				dim = new Dimension(820, 1180);
				break;
			case PHONE:
				dim = new Dimension(375, 687);
				break;
			default:
				// never reached
				throw new UnsupportedOperationException();
			}
			try {
				driver.manage().window().setSize(dim);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void snap0(String snapName) {
		try {
			Thread.sleep(Configuration.getInt(Gallery.class,
					"preSnapPause"));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		File toFileImage = SEUtilities.getChildFile(base, snapName + ".png");
		File toFileHtml = SEUtilities.getChildFile(base, snapName + ".html");
		RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
		try {
			byte[] bytes = remoteDriver.getScreenshotAs(OutputType.BYTES);
			ResourceUtilities.writeBytesToFile(bytes, toFileImage);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		String currentUrl = remoteDriver.getCurrentUrl();
		String base = currentUrl.replaceFirst("(https?://.+?)/.+", "$1");
		String pageSource = (String) remoteDriver
				.executeScript("return document.documentElement.outerHTML;");
		try {
			Document w3cdoc = ResourceUtilities
					.loadHtmlDocumentFromString(pageSource, false);
			DomDocument doc = new DomDocument(w3cdoc);
			doc.xpath("//script ").forEach(DomNode::removeFromParent);
			List<DomNode> stylesheetNodes = doc
					.xpath("//link[@rel='stylesheet'] ").nodes();
			for (DomNode node : stylesheetNodes) {
				String href = node.attr("href");
				if (href.startsWith("/")) {
					String resolved = base + href;
					try {
						String contents = ResourceUtilities
								.readUrlAsString(resolved);
						node.builder().tag("style").text(contents)
								.insertAfterThis();
					} catch (Exception e) {
						Ax.simpleExceptionOut(e);
					}
					node.removeFromParent();
				}
			}
			ResourceUtilities.write(doc.fullToString(), toFileHtml);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public enum Device {
		DESKTOP, TABLET, PHONE
	}
}
