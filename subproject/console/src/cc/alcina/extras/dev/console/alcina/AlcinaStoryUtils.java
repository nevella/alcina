package cc.alcina.extras.dev.console.alcina;

import java.io.File;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.entity.util.source.SourceFinder;

public class AlcinaStoryUtils {
	static String getAlcinaCommit() {
		File alcinaRoot = getAlcinaRoot();
		return Shell.exec("cd %s && git rev-parse HEAD", alcinaRoot);
	}

	static File getAlcinaRoot() {
		try {
			File path = SourceFinder.findSourceFile(AlcinaStoryUtils.class);
			return new File(
					path.getPath().replaceFirst("(.+?/alcina/).+", "$1"));
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}
