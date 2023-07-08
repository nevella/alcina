package cc.alcina.extras.dev.console.alcina;

public class AlcDevLocal extends AlcinaDevConsoleRunnable {
	@Override
	public boolean requiresDomainStore() {
		return false;
	}

	@Override
	public void run() throws Exception {
	}
}
