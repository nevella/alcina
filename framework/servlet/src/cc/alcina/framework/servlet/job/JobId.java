package cc.alcina.framework.servlet.job;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.util.SynchronizedDateFormat;

public class JobId implements Serializable, Comparable<JobId> {
	static final transient long serialVersionUID = -3L;

	private static transient AtomicInteger counter = new AtomicInteger();

	static transient SimpleDateFormat NUMERICAL_DATE_FORMAT = new SynchronizedDateFormat(
			"yyyyMMdd_HHmmss_SSS");

	static transient Pattern parentPattern = Pattern.compile("(.+)::(.+)");

	static transient Pattern subJobPattern = Pattern.compile("(.+)::(\\d+)");

	public String id;

	public JobId(Class performerClass, String jobLauncher) {
		this.id = String.format("%s:%s:%s:%s", jobLauncher,
				performerClass.getSimpleName(),
				NUMERICAL_DATE_FORMAT.format(new Date()), counter.addAndGet(1));
	}

	public JobId(JobId parent, String id) {
		this.id = CommonUtils.join(Arrays.asList(parent.toString(), id), "::");
	}

	public JobId(String path) {
		this.id = path;
	}

	@Override
	public int compareTo(JobId o) {
		Matcher m1 = subJobPattern.matcher(id);
		Matcher m2 = subJobPattern.matcher(o.id);
		if (m1.matches() && m2.matches()) {
			int i = m1.group(1).compareTo(m2.group(1));
			if (i != 0) {
				return i;
			}
			return CommonUtils.compareInts(Integer.parseInt(m1.group(2)),
					Integer.parseInt(m2.group(2)));
		}
		return id.compareTo(o.id);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof JobId && toString().equals(obj.toString());
	}

	public int getLastSegmentAsInt() {
		Matcher m = subJobPattern.matcher(id);
		if (m.matches()) {
			return Integer.parseInt(m.group(2));
		}
		return -1;
	}

	public JobId getParent() {
		Matcher m = parentPattern.matcher(id);
		if (m.matches()) {
			return new JobId(m.group(1));
		}
		return null;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public boolean isChildOf(JobId other) {
		return Objects.equals(getParent(), other);
	}

	@Override
	public String toString() {
		return id;
	}
}
