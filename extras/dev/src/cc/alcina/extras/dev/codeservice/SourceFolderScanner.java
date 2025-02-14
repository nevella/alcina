package cc.alcina.extras.dev.codeservice;

import java.io.File;

import cc.alcina.extras.dev.codeservice.CodeService.Context;
import cc.alcina.extras.dev.codeservice.CodeService.Event;
import cc.alcina.extras.dev.codeservice.CodeService.FileEvent;
import cc.alcina.extras.dev.codeservice.CodeService.SourceFolderEvent;
import cc.alcina.framework.entity.SEUtilities;

/**
 * Scan a source folder and emit a sequence of {@link FileEvent} events
 */
public class SourceFolderScanner extends CodeService.Handler.Abstract {
	@Override
	public void handle(Event event) {
		if (event instanceof SourceFolderEvent) {
			handleSourceFolderEvent((SourceFolderEvent) event);
		}
	}

	void handleSourceFolderEvent(SourceFolderEvent event) {
		SourceFolder sourceFolder = event.sourceFolder;
		File sourceFolderIoFolder = new File(sourceFolder.sourceFolderPath);
		if (!sourceFolderIoFolder.exists()) {
			logger.warn("No source folder: {}", sourceFolder.sourceFolderPath);
			return;
		}
		File classPathFolderIoFolder = new File(
				sourceFolder.classPathFolderPath);
		if (!classPathFolderIoFolder.exists()) {
			logger.warn("No classPath folder: {}",
					sourceFolder.classPathFolderPath);
			return;
		}
		// what are the source packages?
		register(event.context, sourceFolderIoFolder);
		// what are the compiled classes?
		// use classpath (ide-compilation) files and file events to trigger
		// code
		// computation, since ops *currently* use compiled jvm classes
		register(event.context, classPathFolderIoFolder);
	}

	void register(Context context, File folder) {
		if (context.registerPath(folder)) {
			SEUtilities.listFilesRecursive(folder.getPath(), null, true)
					.stream().forEach(file -> context.submitFileEvent(file));
			context.watchFolder(folder);
		}
	}
}
