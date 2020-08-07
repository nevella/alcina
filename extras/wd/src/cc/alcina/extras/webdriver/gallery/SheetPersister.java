package cc.alcina.extras.webdriver.gallery;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RowData;

import cc.alcina.extras.webdriver.gallery.GalleryConfiguration.Element;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.ShellWrapper;

public class SheetPersister {
	private File base;

	List<GalleryFile> files;

	private Element configuration;

	private com.google.api.services.drive.model.File folder;

	private String userAgentType;

	private GoogleDriveAccessor driveAccessor;

	private String dateStamp;

	public void persist(File base, Element configuration, String userAgentType)
			throws Exception {
		this.configuration = configuration;
		this.userAgentType = userAgentType;
		files = Arrays.stream(base.listFiles())
				.sorted(Comparator.comparing(File::lastModified))
				.map(GalleryFile::new).collect(Collectors.toList());
		ZonedDateTime utcZoned = ZonedDateTime.now(ZoneId.of("UTC"));
		dateStamp = utcZoned.format(DateTimeFormatter
				.ofPattern("yyyyMMdd.HHmmss").withZone(ZoneId.of("UTC")));
		uploadImages();
		updateSheet();
	}

	private void updateSheet() throws Exception {
		GoogleSheetAccessor sheetAccessor = new GoogleSheetAccessor()
				.withSheetAccess(configuration.asSheetAccess());
		String hashes = new ShellWrapper()
				.runBashScript(configuration.repoHashesCommand).output;
		{
			List<List<Object>> values = Arrays.asList(
					Arrays.asList("App", configuration.name),
					Arrays.asList("Hashes", hashes),
					Arrays.asList("User agent", userAgentType),
					Arrays.asList("Timestamp (UTC)", dateStamp));
			sheetAccessor.update("A1", values);
		}
		GridRange gridRange = new GridRange();
		gridRange.setStartRowIndex(0);
		gridRange.setEndRowIndex(4);
		gridRange.setStartColumnIndex(0);
		gridRange.setEndColumnIndex(1);
		sheetAccessor.bold(gridRange);
		{
			List<List<Object>> values = Arrays.asList(Arrays
					.asList("UI location", "Image url", "Html url", "Status"));
			sheetAccessor.update("A6", values);
		}
		gridRange = new GridRange();
		gridRange.setStartRowIndex(5);
		gridRange.setEndRowIndex(6);
		gridRange.setStartColumnIndex(0);
		gridRange.setEndColumnIndex(4);
		sheetAccessor.bold(gridRange);
		List<RowData> rowData = sheetAccessor.getRowData();
		Map<String, Integer> keyRow = new LinkedHashMap<>();
		int idx = 6;
		for (; idx < rowData.size(); idx++) {
			keyRow.put(rowData.get(idx).getValues().get(0).getFormattedValue(),
					idx + 1);
		}
		Map<String, GalleryTuple> nameTuples = new LinkedHashMap<>();
		files.forEach(file -> nameTuples
				.computeIfAbsent(file.getName(), key -> new GalleryTuple())
				.put(file));
		// to use in lambda
		AtomicInteger rowCounter = new AtomicInteger(idx + 1);
		nameTuples.values().forEach(tuple -> {
			if (!keyRow.containsKey(tuple.name())) {
				keyRow.put(tuple.name(), rowCounter.getAndIncrement());
			}
			Integer row = keyRow.get(tuple.name());
			List<List<Object>> values = Arrays.asList(Arrays.asList(
					tuple.name(), tuple.image.toDownloadUrl(),
					tuple.html == null ? null : tuple.html.toDownloadUrl()));
			try {
				sheetAccessor.update("A" + row, values);
			} catch (IOException e) {
				throw new WrappedRuntimeException(e);
			}
		});
	}

	void uploadImages() throws IOException {
		driveAccessor = new GoogleDriveAccessor()
				.withDriveAccess(configuration.asDriveAccess());
		String folderPath = Ax.format("%s/%s/%s", configuration.name,
				userAgentType, dateStamp);
		this.folder = driveAccessor.ensureFolder(folderPath);
		files.forEach(GalleryFile::upload);
	}

	class GalleryFile {
		private File file;

		private String id = "none";

		public GalleryFile(File file) {
			this.file = file;
		}

		String getName() {
			return file.getName().replaceFirst("(.+)\\..+", "$1");
		}

		boolean isImage() {
			return file.getName().endsWith(".png");
		}

		String toDownloadUrl() {
			return Ax.format("https://drive.google.com/file/d/%s/view", id);
		}

		void upload() {
			try {
				id = driveAccessor.upload(folder,
						ResourceUtilities.readFileToByteArray(file),
						file.getName()).getId();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	class GalleryTuple {
		GalleryFile image;

		GalleryFile html;

		public void put(GalleryFile file) {
			if (file.isImage()) {
				image = file;
			} else {
				html = file;
			}
		}

		String name() {
			return image.getName();
		}
	}
}
