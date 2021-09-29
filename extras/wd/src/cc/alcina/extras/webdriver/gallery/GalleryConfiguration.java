package cc.alcina.extras.webdriver.gallery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.SheetsScopes;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.servlet.google.DriveAccessor.DriveAccess;
import cc.alcina.framework.servlet.google.SheetAccessor.SheetAccess;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GalleryConfiguration {
	public static void dumpSampleConfiguration() {
		GalleryConfiguration config = new GalleryConfiguration();
		Element element = new Element();
		config.elements.add(element);
		element.name = "---";
		element.spreadSheetId = "---";
		element.credentialsPath = "---";
		element.credentialsStorageLocalPath = "---";
		Ax.out(JaxbUtils.xmlSerialize(config));
	}

	List<Element> elements = new ArrayList<>();

	public Element find(String appName) {
		return elements.stream().filter(e -> e.name.equals(appName)).findFirst()
				.orElse(null);
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	static class Element {
		String name;

		String spreadSheetId;

		String folderId;

		String credentialsPath;

		String credentialsStorageLocalPath;

		String repoHashesCommand;

		public DriveAccess asDriveAccess() {
			return new DriveAccess().withApplicationName(name)
					.withCredentialsPath(credentialsPath)
					.withCredentialsStorageLocalPath(
							credentialsStorageLocalPath)
					.withFolderId(folderId).withScopes(Arrays.asList(
							SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE));
		}

		public SheetAccess asSheetAccess() {
			return new SheetAccess().withApplicationName(name)
					.withCredentialsPath(credentialsPath)
					.withCredentialsStorageLocalPath(
							credentialsStorageLocalPath)
					.withSpreadSheetId(spreadSheetId).withScopes(Arrays.asList(
							SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE));
		}
	}
}
