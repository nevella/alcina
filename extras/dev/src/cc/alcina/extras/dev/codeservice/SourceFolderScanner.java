package cc.alcina.extras.dev.codeservice;

import java.io.File;

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
		File folder = new File(sourceFolder.sourceFolderPath);
		if (!folder.exists()) {
			logger.warn("No source folder: {}", sourceFolder);
			return;
		}
		SEUtilities.listFilesRecursive(folder.getPath(), null, true).stream()
				.filter(sourceFolder).forEach(file -> event.context
						.submitFileEvent(sourceFolder, file));
	}
}
