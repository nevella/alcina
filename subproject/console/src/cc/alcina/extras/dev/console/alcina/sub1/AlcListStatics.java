package cc.alcina.extras.dev.console.alcina.sub1;

import cc.alcina.extras.dev.console.alcina.AlcinaDevConsoleRunnable;
import cc.alcina.extras.dev.console.code.env.TaskAnalyseStatics;
import cc.alcina.extras.dev.console.code.env.TaskAnalyseStatics.PersistentResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;

/*
 * TODO: fix 'todos' in csv - make TaskAnalyseStatics able to merge exiting -
 * singletons?
 */
public class AlcListStatics extends AlcinaDevConsoleRunnable {
	@Override
	public void run() throws Exception {
		String txt = Io.read().resource("ClassTrackingAgent.txt").asString();
		TaskAnalyseStatics task = new TaskAnalyseStatics();
		task.classList = txt;
		task.classNameFilter = "(com.google.gwt|(cc.alcina.framework.(common|gwt|entity))	)\\..+";
		task.run();
		Ax.out(task.result.toUnresolvedString());
		PersistentResult persistentResult = new TaskAnalyseStatics.PersistentResult(
				task.result);
		String csvString = persistentResult.toCsvString();
		Io.write().string(csvString).toPath(
				"/g/alcina/subproject/console/src/cc/alcina/extras/dev/console/alcina/sub1/ClassTrackingAgent.out.csv");
	}
}
