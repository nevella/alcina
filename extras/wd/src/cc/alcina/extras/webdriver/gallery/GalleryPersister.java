package cc.alcina.extras.webdriver.gallery;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RowData;

import cc.alcina.extras.webdriver.gallery.GalleryConfiguration.Element;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasEquivalenceString;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.servlet.google.DriveAccessor;
import cc.alcina.framework.servlet.google.SheetAccessor;

public class GalleryPersister {
	static boolean isPersistToGoogle() {
		return Configuration.is("persistToGoogle");
	}

	File base;

	List<GalleryFile> files;

	private Element configuration;

	private com.google.api.services.drive.model.File folder;

	private String userAgentType;

	private DriveAccessor driveAccessor;

	private String dateStamp;

	private String hashes;

	private String build;

	private Map<String, GalleryTuple> nameTuples() {
		Map<String, GalleryTuple> nameTuples = new LinkedHashMap<>();
		files.forEach(file -> nameTuples
				.computeIfAbsent(file.getName(), GalleryTuple::new).put(file));
		nameTuples.entrySet().removeIf(e -> e.getValue().isInvalid());
		return nameTuples;
	}

	public void persist(File base, Element configuration, String userAgentType)
			throws Exception {
		this.base = base;
		this.configuration = configuration;
		this.userAgentType = userAgentType;
		files = Arrays.stream(base.listFiles())
				.filter(f -> !f.getName().startsWith("."))
				.sorted(Comparator.comparing(File::lastModified))
				.map(GalleryFile::new).collect(Collectors.toList());
		ZonedDateTime utcZoned = ZonedDateTime.now(ZoneId.of("UTC"));
		dateStamp = utcZoned.format(DateTimeFormatter
				.ofPattern("yyyyMMdd.HHmmss").withZone(ZoneId.of("UTC")));
		driveAccessor = new DriveAccessor()
				.withDriveAccess(configuration.asDriveAccess());
		hashes = new Shell()
				.runBashScript(configuration.repoHashesCommand).output;
		build = BuildNumberProvider.get().getBuildNumber(configuration.name);
		if (isPersistToGoogle()) {
			new SheetAccessor().withSheetAccess(configuration.asSheetAccess())
					.ensureSpreadsheet();
			uploadImages();
			AlcinaChildRunnable.launchWithCurrentThreadContext("update-sheet",
					() -> updateSheet());
		}
		updateViewer();
	}

	private void updateSheet() throws Exception {
		SheetAccessor sheetAccessor = new SheetAccessor()
				.withSheetAccess(configuration.asSheetAccess());
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
		List<RowData> rowData = sheetAccessor.getRowData(0);
		Map<String, Integer> keyRow = new LinkedHashMap<>();
		int idx = 6;
		for (; idx < rowData.size(); idx++) {
			String formattedValue = rowData.get(idx).getValues().get(0)
					.getFormattedValue();
			if (Ax.isBlank(formattedValue)) {
				break;
			}
			keyRow.put(formattedValue, idx + 1);
		}
		Map<String, GalleryTuple> nameTuples = nameTuples();
		// to use in lambda
		AtomicInteger rowCounter = new AtomicInteger(idx + 1);
		nameTuples.values().forEach(tuple -> {
			if (!keyRow.containsKey(tuple.name())) {
				keyRow.put(tuple.name(), rowCounter.getAndIncrement());
			}
			Integer row = keyRow.get(tuple.name());
			List<List<Object>> values = Arrays.asList(Arrays.asList(
					tuple.name(), tuple.image.toPreviewUrl(),
					tuple.html == null ? null : tuple.html.toPreviewUrl()));
			try {
				sheetAccessor.update("A" + row, values);
			} catch (IOException e) {
				throw new WrappedRuntimeException(e);
			}
		});
	}

	private void updateViewer() throws IOException {
		DomDocument doc = DomDocument.basicHtmlDoc();
		List<Image> images = nameTuples().values().stream().map(Image::new)
				.collect(Collectors.toList());
		String json = JacksonUtils.serializeNoTypes(images);
		String js = Ax.format("let __viewer_data=JSON.parse(\"%s\");",
				StringEscapeUtils.escapeJavaScript(json));
		doc.html().head().builder().tag("script").append();
		doc.html().head().builder().tag("script").append();
		doc.html().head().builder().tag("style").append();
		doc.html().body().addClassName(userAgentType);
		DomNode main = doc.html().body().builder().tag("div").className("main")
				.append();
		DomNode left = main.builder().tag("div").className("left").append();
		List<List<Object>> metadataValues = Arrays.asList(
				Arrays.asList("App", configuration.name),
				Arrays.asList("Hashes", hashes), Arrays.asList("Build", build),
				Arrays.asList("User agent", userAgentType),
				Arrays.asList("Timestamp (UTC)", dateStamp));
		{
			DomNode header = left.builder().tag("div").className("header")
					.append();
			header.builder().tag("h2").text("Gallery").append();
			DomNodeHtmlTableBuilder builder = header.html().tableBuilder();
			for (List<Object> list : metadataValues) {
				builder.row().cell(list.get(0).toString() + ":")
						.cell(list.get(1).toString());
			}
		}
		{
			DomNode buttons = left.builder().tag("div").className("buttons")
					.append();
			DomNode links = left.builder().tag("div").className("links")
					.append();
			buttons.builder().tag("button").text("<").title("Shortcut: <-")
					.attr("onclick", "previous()").append();
			buttons.builder().tag("button").text(">").title("Shortcut: ->")
					.attr("onclick", "next()").append();
			// buttons.builder().tag("div").className("hint")
			// .text("Shortcuts : , and .").append();
			AtomicInteger counter = new AtomicInteger();
			images.forEach(image -> {
				DomNode link = links.html().addLink(image.name, image.url,
						"_blank");
				link.setAttr("id", "link_" + counter.getAndIncrement());
				link.setAttr("onclick", "return view(this.id);");
			});
		}
		{
			DomNode viewer = main.builder().tag("div").className("viewer")
					.append();
			viewer.builder().tag("div").attr("class", "img-wrapper").append()
					.builder().tag("img").attr("id", "img__")
					.attr("onclick", "toggleFull()")
					.attr("onload", "this.className='loaded';").append();
		}
		String prettyToString = doc.toPrettyMarkup();
		prettyToString = prettyToString.replaceFirst("(<script/>)",
				Matcher.quoteReplacement(Ax.format("<script>%s</script>", js)));
		prettyToString = prettyToString.replaceFirst("(<script/>)",
				Ax.format("<script>%s</script>", Matcher.quoteReplacement(
						Io.read().resource("res/viewer.js").asString())));
		prettyToString = prettyToString.replaceFirst("(<style/>)",
				Ax.format("<style>%s</style>", Matcher.quoteReplacement(
						Io.read().resource("res/viewer.css").asString())));
		File indexHtml = new File(base, "index.html");
		File indexJson = new File(base, "index.json");
		Io.write().string(prettyToString).toFile(indexHtml);
		StringMap metadata = new StringMap();
		metadataValues.stream().forEach(list -> metadata
				.put(list.get(0).toString(), list.get(1).toString()));
		GallerySnapshot snapshot = new GallerySnapshot(images, metadata);
		JacksonUtils.serializeToFile(snapshot, indexJson);
		Ax.out("Wrote gallery index to: %s\n", indexHtml.getPath());
		if (Configuration.is("persistToGoogle")) {
			byte[] bytes = prettyToString.getBytes(StandardCharsets.UTF_8);
			{
				String name = Ax.format("%s.%s.%s.html", configuration.name,
						userAgentType, dateStamp);
				AlcinaChildRunnable.launchWithCurrentThreadContext(
						"upload-new-version", () -> {
							com.google.api.services.drive.model.File upload = driveAccessor
									.upload(driveAccessor.rootFolder(), bytes,
											name, false);
							String href = Ax.format("%s/drive?id=%s",
									configuration.base, upload.getId());
							Ax.out("Index %s url: <a href='%s' target='_blank'>%s</a>",
									name, href, href);
						});
			}
			{
				String name = Ax.format("%s.%s.html", configuration.name,
						userAgentType);
				AlcinaChildRunnable.launchWithCurrentThreadContext(
						"update-current-version", () -> {
							com.google.api.services.drive.model.File upload = driveAccessor
									.upload(driveAccessor.rootFolder(), bytes,
											name, true);
							String href = Ax.format("%s/drive?id=%s",
									configuration.base, upload.getId());
							Ax.out("Index %s (current) url: <a href='%s' target='_blank'>%s</a>",
									name, href, href);
						});
			}
		}
	}

	void uploadImages() throws IOException {
		String folderPath = Ax.format("%s/%s/%s", configuration.name,
				userAgentType, dateStamp);
		this.folder = driveAccessor.ensureFolder(folderPath);
		files.parallelStream().forEach(GalleryFile::upload);
	}

	@Registration(BuildNumberProvider.class)
	public static class BuildNumberProvider {
		public static GalleryPersister.BuildNumberProvider get() {
			return Registry.impl(GalleryPersister.BuildNumberProvider.class);
		}

		public String getBuildNumber(String name) {
			return "Unknown";
		}
	}

	class GalleryFile {
		private File file;

		private String id = "none";

		public GalleryFile(File file) {
			this.file = file;
		}

		String getFileName() {
			return file.getName();
		}

		String getName() {
			return file.getName().replaceFirst("(.+)\\..+", "$1");
		}

		boolean isImage() {
			return file.getName().endsWith(".png");
		}

		String toDownloadUrl() {
			return Ax.format(
					"https://drive.google.com/uc?export=download&id=%s", id);
		}

		String toPreviewUrl() {
			return Ax.format("https://drive.google.com/file/d/%s/view", id);
		}

		public String toViewUrl() {
			return id.equals("none") ? file.getName()
					: Ax.format("/drive?id=%s", id);
		}

		void upload() {
			try {
				id = driveAccessor.upload(folder,
						Io.read().file(file).asBytes(), file.getName(), false)
						.getId();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	class GalleryTuple {
		GalleryFile image;

		GalleryFile html;

		String name;

		GalleryTuple(String name) {
			this.name = name;
		}

		public boolean isInvalid() {
			if (image == null || html == null) {
				return true;
			} else {
				return false;
			}
		}

		String name() {
			return name;
		}

		public void put(GalleryFile file) {
			if (file.isImage()) {
				image = file;
			} else {
				html = file;
			}
		}

		@Override
		public String toString() {
			return name;
		}
	}

	static class Image implements HasEquivalenceString<Image> {
		String name;

		String url;

		String fileName;

		String sha1Hash;

		Image() {
		}

		Image(GalleryTuple tuple) {
			name = tuple.name();
			url = tuple.image.toViewUrl();
			fileName = tuple.image.getFileName();
			try {
				sha1Hash = EncryptionUtils.get()
						.SHA1(Io.read().file(tuple.image.file).asBytes());
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		@Override
		public String equivalenceString() {
			return fileName;
		}

		@Override
		public String toString() {
			return fileName;
		}
	}
}
