package cc.alcina.extras.webdriver.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
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

	private TestResultType resultType = TestResultType.OK;

	private transient Exception exception;

	public void addResult(TestResult result) {
		results.add(result);
	}

	public long getEndTime() {
		return this.endTime;
	}

	@JsonIgnore
	@AlcinaTransient
	public Exception getException() {
		return this.exception;
	}

	public String getMessage() {
		StringBuffer sb = new StringBuffer();
		for (TestResult tr : getResults()) {
			sb.append(tr.getMessage());
		}
		sb.append(message);
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	public List<TestResult> getResults() {
		return results;
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

	public long getStartTime() {
		return this.startTime;
	}

	public long getStartTimeExcludingDependent() {
		return startTimeExcludingDependent;
	}

	public boolean isNoTimePayload() {
		return noTimePayload;
	}

	public boolean isRootResult() {
		return rootResult;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNoTimePayload(boolean noTimePayload) {
		this.noTimePayload = noTimePayload;
	}

	public void setResults(List<TestResult> results) {
		this.results = results;
	}

	public void setResultType(TestResultType resultType) {
		this.resultType = resultType;
	}

	public void setRootResult(boolean rootResult) {
		this.rootResult = rootResult;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void
			setStartTimeExcludingDependent(long startTimeExcludingDependent) {
		this.startTimeExcludingDependent = startTimeExcludingDependent;
	}

	public long testDuration(boolean justThisTest) {
		return endTime
				- (justThisTest ? startTimeExcludingDependent : startTime);
	}

	public long testDurationExcludingAdmin() {
		return endTime - startTime - getAdminTime();
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

	long getAdminTime() {
		long result = noTimePayload ? endTime - startTime : 0;
		for (TestResult tr : getResults()) {
			result += tr.getAdminTime();
		}
		return result;
	}
}
