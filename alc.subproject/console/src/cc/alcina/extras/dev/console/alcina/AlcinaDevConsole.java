package cc.alcina.extras.dev.console.alcina;

import javax.xml.bind.UnmarshalException;

import cc.alcina.extras.dev.console.DevConsole;

public class AlcinaDevConsole extends DevConsole {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.setProperty("jsse.enableSNIExtension", "false");
			new AlcinaDevConsole(args).init();
		} catch (Throwable e) {
			stdSysOut();
			e.printStackTrace();
			if (e instanceof UnmarshalException) {
				((UnmarshalException) e).getLinkedException().printStackTrace();
			}
			System.exit(1);
		}
	}

	public AlcinaDevConsole(String[] args) {
		super(args);
	}

	@Override
	public void ensureDomainStore() throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void createDevHelper() {
		devHelper = new AlcinaDevHelper();
	}
}
