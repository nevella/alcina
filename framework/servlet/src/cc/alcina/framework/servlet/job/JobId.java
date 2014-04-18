package cc.alcina.framework.servlet.job;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import cc.alcina.framework.common.client.util.CommonUtils;

public class JobId implements Serializable {
	static final transient long serialVersionUID = -3L;

	public String id;

	private static transient AtomicInteger counter = new AtomicInteger();

	static transient SimpleDateFormat NUMERICAL_DATE_FORMAT = new SimpleDateFormat(
			"yyyyMMdd_HHmmss_SSS");

	public JobId(Class performerClass, String jobLauncher) {
		this.id = String.format("%s:%s:%s:%s", jobLauncher,
				performerClass.getSimpleName(),
				NUMERICAL_DATE_FORMAT.format(new Date()), counter.addAndGet(1));
	}

	public JobId(JobId parent, String id) {
		this.id = CommonUtils
				.join(Arrays.asList(parent.toString(), id), "::");
	}

	public JobId(String path) {
		this.id = path;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof JobId && toString().equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id;
	}
}
