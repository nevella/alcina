package cc.alcina.extras.dev.console.alcina;

import java.util.List;

import cc.alcina.extras.dev.codeservice.CodeService;
import cc.alcina.extras.dev.codeservice.PackagePropertiesGenerator;
import cc.alcina.extras.dev.console.DevConsole;
import cc.alcina.framework.entity.Io;

public class CmdLaunchCodeService extends AlcinaDevConsoleCommand {
	@Override
	public String[] getCommandIds() {
		return new String[] { "codeservice" };
	}

	@Override
	public String getDescription() {
		return "Launch the alcina codeservice";
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public boolean rerunIfMostRecentOnRestart() {
		return true;
	}

	@Override
	public String run(String[] argv) throws Exception {
		CodeService codeService = new CodeService();
		// the listpath file might contain - say -
		// /g/alcina/framework/servlet\n/g/alcina/bin
		codeService.sourceAndClassPaths = Io.read()
				.resource("codeserver-paths.local.txt").asList();
		codeService.handlerTypes = List.of(PackagePropertiesGenerator.class);
		codeService.blockStartThread = DevConsole.get().isExitAfterCommand();
		codeService.start();
		return "started";
	}
}