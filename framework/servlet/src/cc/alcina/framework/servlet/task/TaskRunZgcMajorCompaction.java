package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskRunZgcMajorCompaction extends PerformerTask.Remote {
	@Override
	public void run() throws Exception {
		logger.info("zgc-event :: Launching ZGC major compaction");
		new Shell().runBashScript("jcmd `pgrep java` GC.run");
		new Shell().runBashScript(
				"tail -n 10000 /opt/jboss/gc.log | grep Major | tail -n 1")
				.logOutput();
	}
}
