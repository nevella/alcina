package cc.alcina.extras.dev.console.alcina;

import cc.alcina.extras.dev.console.alcina.sub1.AlcSub1TestReflectionGeneration;

public class AlcDevLocal extends AlcinaDevConsoleRunnable {
	@Override
	public boolean requiresDomainStore() {
		return false;
	}

	@Override
	public void run() throws Exception {
		new AlcSub1TestReflectionGeneration().sub(this);
	}
}
