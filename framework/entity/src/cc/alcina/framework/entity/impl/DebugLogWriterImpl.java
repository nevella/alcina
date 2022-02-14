package cc.alcina.framework.entity.impl;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Job.DebugLogWriter;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;

@Registration(value = DebugLogWriter.class, priority = Registration.Priority.PREFERRED_LIBRARY)
public class DebugLogWriterImpl extends Job.DebugLogWriter {
	public void write(Job job) {
		ResourceUtilities.logToFile(
				job.domain().ensurePopulated().getLargeResult().toString());
		Ax.out("Job result files:\n/tmp/log/log.xml\n  /tmp/log/log.html");
	}
}
