package cc.alcina.framework.servlet.component.console.rcs;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.servlet.environment.EnvironmentManager;
import cc.alcina.framework.servlet.logging.FlightEventRecorder;

@Registration.Singleton(RomcomSessionProvider.class)
public class RomcomSessionProviderFs implements RomcomSessionProvider {
	class EventFolder {
		File folder;

		long lastModificationDate;

		RomcomSessionEntry entry;

		List<FlightEvent> events;

		File entryFile;

		EventFolder(File folder) {
			this.folder = folder;
		}

		RomcomSessionEntry asEntry() {
			refresh();
			return entry;
		}

		void refresh() {
			if (lastModificationDate == folder.lastModified()) {
				return;
			}
			entryFile = FileUtils.child(metadataFolder,
					Ax.format(".RomcomSessionEntry-%s.json", folder.getName()));
			if (entryFile.exists()) {
				try {
					this.entry = Io.read().file(entryFile)
							.asReflectiveSerializedObject();
					this.lastModificationDate = entry.folderLastModificationDate;
					if (lastModificationDate == folder.lastModified()) {
						return;
					}
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
			lastModificationDate = folder.lastModified();
			/*
			 * swap, don't update in place
			 */
			events = (List) SEUtilities
					.listFilesRecursive(folder.getPath(), null).stream()
					.filter(f -> f.isFile() && !f.isHidden())
					.map(f -> Io.read().file(f).asString())
					.<FlightEvent> map(ReflectiveSerializer::deserialize)
					.sorted().collect(Collectors.toList());
			String sessionId = folder.getName().replaceFirst("(.+?)\\.(.+)",
					"$2");
			boolean active = EnvironmentManager.get().hasEnvironment(sessionId);
			RomcomSessionEntry entry = new RomcomSessionEntry(events, sessionId,
					active, folder.getPath(), lastModificationDate);
			this.entry = entry;
			persistEntry();
		}

		void persistEntry() {
			Io.write().asReflectiveSerialized(true).object(entry)
					.toFile(entryFile);
		}
	}

	String eventRootPath;

	Map<File, EventFolder> fileEventFolder = new LinkedHashMap<>();

	File metadataFolder;

	File eventsFolder;

	RomcomSessionProviderFs() {
		eventRootPath = Configuration.get(FlightEventRecorder.class, "path");
		eventsFolder = new File(eventRootPath);
		eventsFolder.mkdirs();
	}

	@Override
	public synchronized Stream<RomcomSessionEntry> getSessions() {
		metadataFolder = FileUtils.child(eventsFolder, "session-metadata");
		metadataFolder.mkdirs();
		Stream<File> folders = Stream.of(eventsFolder.listFiles())
				.filter(f -> !Objects.equals(f, metadataFolder))
				.sorted(Comparator.comparing(File::lastModified));
		return folders.filter(File::isDirectory)
				.map(f -> fileEventFolder.computeIfAbsent(f, EventFolder::new))
				.map(EventFolder::asEntry).filter(Objects::nonNull);
	}

	@Override
	public synchronized void clear() {
		fileEventFolder.clear();
	}

	@Override
	public void persist(RomcomSessionEntry entry) {
		EventFolder eventFolder = fileEventFolder.get(new File(entry.path));
		eventFolder.persistEntry();
	}

	@Override
	public String ensureSession(String path) {
		File from = new File(path);
		File entryFolder = FileUtils.child(eventsFolder, from.getName());
		if (entryFolder.exists()) {
			return null;
		} else {
			try {
				SEUtilities.copyFile(from, entryFolder);
				return entryFolder.getPath();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}
}
