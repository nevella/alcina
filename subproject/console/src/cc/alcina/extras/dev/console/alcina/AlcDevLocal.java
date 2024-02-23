package cc.alcina.extras.dev.console.alcina;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.entity.gwt.reflection.jdk.JdkReflectionGenerator;

public class AlcDevLocal extends AlcinaDevConsoleRunnable {
	@Override
	public boolean requiresDomainStore() {
		return false;
	}

	@Override
	public void run() throws Exception {
		JdkReflectionGenerator.Attributes attributes = JdkReflectionGenerator
				.attributes();
		// attributes.clean = true;
		attributes.outputRoot = "/tmp/alc/reflection";
		attributes.loadClassDirectoryPaths(Entity.class, AlcDevLocal.class);
		attributes.build().generate();
	}
}
