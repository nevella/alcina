package cc.alcina.extras.dev.console.task;

import java.io.File;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.entity.util.ZipUtil;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionProvider;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskPopulateRomcomSessionReplay extends PerformerTask
		implements Supplier<String> {
	@Override
	public void run() throws Exception {
		// unzip events
		File outputFolder = File.createTempFile("unzip", "zip");
		SEUtilities.deleteDirectory(outputFolder, false);
		outputFolder.mkdirs();
		File eventFolder = FileUtils.child(outputFolder,
				"leela.local-0-pxdj-rxdzzmr");
		eventFolder.mkdirs();
		ZipUtil.unzip(eventFolder,
				Io.read().resource("flight-events-replay.zip").asInputStream());
		String toPath = RomcomSessionProvider.get()
				.ensureSession(eventFolder.getPath());
		SEUtilities.deleteDirectory(outputFolder, false);
		if (toPath != null) {
			resultMessage = Ax.format("Populated session at %s", toPath);
		}
	}

	transient String resultMessage = "";

	@Override
	public String get() {
		try {
			run();
			return resultMessage;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}
