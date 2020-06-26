package cc.alcina.extras.webdriver.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;

public class TestResult {
	private List<TestResult> results = new ArrayList<TestResult>();

	private String message = "";

	private String name = "";

	private long startTime;

	private long endTime;

	private long startTimeExcludingDependent;

	private boolean rootResult = false;

	private boolean noTimePayload;

	public long getStartTime() {
		return this.startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return this.endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	private TestResultType resultType = TestResultType.OK;

	public String getMessage() {
		StringBuffer sb = new StringBuffer();
		for (TestResult tr : getResults()) {
			sb.append(tr.getMessage());
		}
		sb.append(message);
		return sb.toString();
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public TestResultType getResultType() {
		TestResultType rt = this.resultType;
		for (TestResult tr : getResults()) {
			TestResultType crt = tr.getResultType();
			if (crt.compareTo(rt) > 0) {
				rt = crt;
			}
		}
		return rt;
	}

	public void setResultType(TestResultType resultType) {
		this.resultType = resultType;
	}

	long getAdminTime() {
		long result = noTimePayload ? endTime - startTime : 0;
		for (TestResult tr : getResults()) {
			result += tr.getAdminTime();
		}
		return result;
	}

	@Override
	public String toString() {
		String template = "%s [Result]: %s %s %sms";
		String s = Ax.format(template, getName(), getResultType(), getMessage(),
				testDuration(true));
		if (isRootResult()) {
			s += Ax.format(",  %sms excl. admin, %sms total",
					testDurationExcludingAdmin(), testDuration(false));
			s += "\n - - " + new Date();
		}
		return s + "\n";
	}

	public long testDuration(boolean justThisTest) {
		return endTime
				- (justThisTest ? startTimeExcludingDependent : startTime);
	}

	public long testDurationExcludingAdmin() {
		return endTime - startTime - getAdminTime();
	}

	public void setNoTimePayload(boolean noTimePayload) {
		this.noTimePayload = noTimePayload;
	}

	public boolean isNoTimePayload() {
		return noTimePayload;
	}

	public void setResults(List<TestResult> results) {
		this.results = results;
	}

	public List<TestResult> getResults() {
		return results;
	}

	public void addResult(TestResult result) {
		results.add(result);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void
			setStartTimeExcludingDependent(long startTimeExcludingDependent) {
		this.startTimeExcludingDependent = startTimeExcludingDependent;
	}

	public long getStartTimeExcludingDependent() {
		return startTimeExcludingDependent;
	}

	public void setRootResult(boolean rootResult) {
		this.rootResult = rootResult;
	}

	public boolean isRootResult() {
		return rootResult;
	}
}
