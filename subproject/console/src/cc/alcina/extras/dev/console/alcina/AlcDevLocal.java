package cc.alcina.extras.dev.console.alcina;

import cc.alcina.extras.dev.console.alcina.AlcinaDevConsoleCommand.CmdTraverseCroissainteria;

public class AlcDevLocal extends AlcinaDevConsoleRunnable {
	@Override
	public boolean requiresDomainStore() {
		return false;
	}

	@Override
	public void run() throws Exception {
		// Ax.out("hello world");
		// new AlcListStatics().sub();
		// new AlcSubStory().sub();
		new CmdTraverseCroissainteria().run(new String[] {});
	}
}
