package cc.alcina.extras.webdriver.gallery;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.Document;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.servlet.google.SheetAccessor;
import cc.alcina.framework.servlet.google.SheetAccessor.SheetAccess;

@SuppressWarnings("unused")
public class Gallery {
	private static Gallery instance;

	public static void begin(String appName, String userAgentType,
			URL configurationUrl) {
		instance = new Gallery(appName, userAgentType, configurationUrl);
	}

	public static void end() {
		instance.end0();
		instance = null;
	}

	public static void putDriver(RemoteWebDriver driver) {
		if (instance != null) {
			instance.driver = driver;
		}
	}

	public static void snap(String snapName) {
		if (ResourceUtilities.is("snap")) {
			instance.snap0(snapName);
		}
	}

	private String appName;

	private String userAgentType;

	private File base;

	private RemoteWebDriver driver;

	private SheetAccess sheetAccess;

	private URL configurationUrl;

	private GalleryConfiguration galleryConfiguration;

	private GalleryConfiguration.Element configuration;

	private Gallery(String appName, String userAgentType,
			URL configurationUrl) {
		this.appName = appName;
		this.userAgentType = userAgentType;
		this.configurationUrl = configurationUrl;
		Gallery.instance = this;
		base = new File(
				Ax.format("%s/%s/%s", ResourceUtilities.get("defaultLocalPath"),
						appName, userAgentType));
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
				new SheetAccessor()
						.withSheetAccess(configuration.asSheetAccess())
						.ensureSpreadsheet();
			} catch (Exception e) {
				GalleryConfiguration.dumpSampleConfiguration();
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private void end0() {
		if (galleryConfiguration != null) {
			try {
				new SheetPersister().persist(base, configuration,
						userAgentType);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private void snap0(String snapName) {
		try {
			Thread.sleep(ResourceUtilities.getInteger(Gallery.class,
					"preSnapPause"));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		File toFileImage = SEUtilities.getChildFile(base, snapName + ".png");
		File toFileHtml = SEUtilities.getChildFile(base, snapName + ".html");
		RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
		byte[] bytes = remoteDriver.getScreenshotAs(OutputType.BYTES);
		try {
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
					.loadHtmlDocumentFromString(pageSource,false);
			DomDoc doc = new DomDoc(w3cdoc);
			doc.xpath("//script ").forEach(DomNode::removeFromParent);
			List<DomNode> stylesheetNodes = doc.xpath(
					"//link[@rel='stylesheet'] ")
					.nodes();
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
}
