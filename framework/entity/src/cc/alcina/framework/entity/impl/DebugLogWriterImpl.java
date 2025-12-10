package cc.alcina.framework.entity.impl;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.DebugLogWriter;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;

@Registration(
	value = DebugLogWriter.class,
	priority = Registration.Priority.PREFERRED_LIBRARY)
public class DebugLogWriterImpl extends Job.DebugLogWriter {
	public void write(Job job) {
		job.domain().ensurePopulated();
		Io.log().toFile(job.getLargeResult().toString());
		Ax.out("Job result files:\n/tmp/log/log.xml\n  /tmp/log/log.html");
	}
}
