package cc.alcina.extras.webdriver.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.WebDriver;

import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WDUtils.TimedOutException;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;

@Registration(WebdriverTest.class)
public abstract class WebdriverTest implements Registration.Ensure {
	public static final String CONTEXT_CURRENT_TEST = WebdriverTest.class
			.getName() + ".CONTEXT_CURRENT_TEST";

	protected static Map<Class<? extends WebdriverTest>, WebdriverTest> testTemplates;

	public static <T extends WebdriverTest> T current() {
		return LooseContext.get(CONTEXT_CURRENT_TEST);
	}

	protected int myLevel;

	private TestResult testResult;

	public Class<? extends WebdriverTest>[] childTests() {
		return new Class[0];
	}

	public Enum<?>[] depends() {
		return new Enum[0];
	}

	public void getAndLog(WebDriver driver, String uri) {
		getAndLog(driver, uri, null);
	}

	public List<WebdriverTest> getRequiredDependentTests(WDToken token) {
		List<WebdriverTest> results = new ArrayList<WebdriverTest>();
		Enum<?>[] depends = depends();
		Map<Class<? extends WebdriverTest>, WebdriverTest> templates = getTestTemplates();
		Set<Class<? extends WebdriverTest>> added = new LinkedHashSet<Class<? extends WebdriverTest>>();
		for (Enum<?> e : depends) {
			if (!token.hasUIState(e)) {
				for (Class<? extends WebdriverTest> c : templates.keySet()) {
					WebdriverTest test = templates.get(c);
					List<Enum<?>> providedState = Arrays
							.asList(test.providesUIState());
					if (providedState.contains(e)) {
						try {
							if (added.contains(c)) {
							} else {
								results.add(0, c.getDeclaredConstructor()
										.newInstance());
								added.add(c);
							}
						} catch (Exception e2) {
							throw new WrappedRuntimeException(e2);
						}
					}
				}
			}
		}
		return results;
	}

	public void goToHash(WebDriver driver, String hash) {
		try {
			String curr = driver.getCurrentUrl();
			curr = String.format("%s#%s", curr.replaceFirst("#.*", ""), hash);
			driver.get(curr);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public boolean noTimePayload() {
		return false;
	}

	public void onTimeoutException(TimedOutException timedOutException) {
		testResult.setException(timedOutException);
		ProcessObservers.publish(WebdriverTest.TestException.class,
				() -> new WebdriverTest.TestException(this));
	}

	public void predelay(WDToken token, int level) {
		try {
			int predelayMs = token.getConfiguration().predelayMs;
			if (predelayMs != 0) {
				token.getWriter().write(
						Ax.format("Predelay: %s ms \n", predelayMs), level);
				Thread.sleep(predelayMs);
			}
		} catch (InterruptedException e) {
		}
	}

	public TestResult process(WDToken token, int level, TestResult parent)
			throws Exception {
		try {
			LooseContext.pushWithKey(CONTEXT_CURRENT_TEST, this);
			return process0(token, level, parent);
		} finally {
			LooseContext.pop();
		}
	}

	/**
	 * advertise
	 *
	 * @return
	 */
	public Enum<?>[] providesUIState() {
		return new Enum[0];
	}

	/**
	 * don't advertise
	 *
	 * @return
	 */
	public Enum<?>[] returnsUIState() {
		return new Enum[0];
	}

	public abstract void run(WDToken token, TestResult result) throws Exception;

	@Override
	public String toString() {
		FormatBuilder builder = new FormatBuilder().separator(" :: ");
		builder.appendIfNotBlank(getClass().getName());
		if (testResult != null && testResult.getException() != null) {
			builder.appendIfNotBlank(
					testResult.getException().getClass().getSimpleName());
			builder.appendIfNotBlank(testResult.getException().getMessage());
		}
		return builder.toString();
	}

	public void uiStateChange(WDToken token, Enum<?> e) {
		token.getUiStates().put(e.getDeclaringClass(), e);
	}

	public void uiStateChange(WDToken token, Enum<?>[] enums) {
		for (Enum<?> e : enums) {
			uiStateChange(token, e);
		}
	}

	private boolean cancelDueToError(WDToken token, int level) {
		if (token.getRootResult().getResultType() == TestResultType.ERROR) {
			token.getWriter().write("cancelled - prior error", level);
			return true;
		} else {
			return false;
		}
	}

	private TestResult process0(WDToken token, int level, TestResult parent)
			throws Exception {
		this.myLevel = level;
		String oldThreadName = Thread.currentThread().getName();
		Thread.currentThread()
				.setName("Test--" + token.getConfiguration().name);
		testResult = new TestResult();
		testResult.setStartTime(System.currentTimeMillis());
		testResult.setNoTimePayload(noTimePayload());
		testResult.setName(getClass().getSimpleName());
		token.getWriter().write(
				Ax.format("Test: %s - \n", getClass().getSimpleName()), level);
		if (parent == null) {
			token.setRootResult(testResult);
			testResult.setRootResult(true);
			predelay(token, level + 1);
			testResult.setStartTime(System.currentTimeMillis());
		} else {
			parent.addResult(testResult);
		}
		if (cancelDueToError(token, level)) {
			return testResult;
		}
		level++;
		List<WebdriverTest> dependentTests = getRequiredDependentTests(token);
		beforeDependentTests(token);
		if (!dependentTests.isEmpty()) {
			level++;
			token.getWriter().write("Processing dependencies - \n", level);
			for (WebdriverTest dtest : dependentTests) {
				dtest.process(token, level, testResult);
			}
			level--;
		}
		if (cancelDueToError(token, level)) {
			return testResult;
		}
		beforeChildTests(token);
		long startTime = System.currentTimeMillis();
		token.getWriter().write(
				Ax.format("Starting test: %s - \n", getClass().getSimpleName()),
				level);
		Class<? extends WebdriverTest>[] childTests = childTests();
		if (childTests.length != 0) {
			level++;
			token.getWriter().write("Processing child tests - \n", level);
			for (Class<? extends WebdriverTest> tc : childTests) {
				WebdriverTest childTest = tc.getDeclaredConstructor()
						.newInstance();
				childTest.process(token, level, testResult);
			}
			level--;
		}
		testResult.setResultType(TestResultType.OK);
		try {
			int maxAttempts = getRetryCount();
			int attempt = 1;
			while (true) {
				try {
					testResult.setStartTimeExcludingDependent(
							System.currentTimeMillis());
					System.err.println(
							"running test - " + getClass().getSimpleName());
					run(token, testResult);
					uiStateChange(token, providesUIState());
					uiStateChange(token, returnsUIState());
					break;
				} catch (Exception e) {
					if (attempt >= maxAttempts) {
						throw e;
					} else {
						attempt++;
						Ax.out("============================================================");
						Ax.out("Unit test execption - retrying [%s/%s]",
								attempt, maxAttempts);
						Ax.out("============================================================");
						e.printStackTrace();
						Ax.out("============================================================\n");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			testResult.setResultType(TestResultType.ERROR);
			testResult.setMessage(e.getMessage());
			testResult.setException(e);
			if (!(e instanceof TimedOutException)) {
				// will have already been published
				ProcessObservers.publish(WebdriverTest.TestException.class,
						() -> new WebdriverTest.TestException(this));
			}
		} finally {
			long endTime = System.currentTimeMillis();
			testResult.setEndTime(endTime);
		}
		token.getWriter().write(testResult.toString(), level);
		Thread.currentThread().setName(oldThreadName);
		return testResult;
	}

	protected void beforeChildTests(WDToken token) {
	}

	protected void beforeDependentTests(WDToken token) {
	}

	protected void getAndLog(WebDriver driver, String uri, WDToken token) {
		String key = "Load: " + uri;
		MetricLogging.get().start(key);
		driver.get(uri);
		if (token != null) {
			token.setLoadedUrl(uri);
		}
		MetricLogging.get().end(key);
	}

	protected int getRetryCount() {
		return 1;
	}

	protected Map<Class<? extends WebdriverTest>, WebdriverTest>
			getTestTemplates() {
		if (testTemplates == null) {
			testTemplates = new HashMap<Class<? extends WebdriverTest>, WebdriverTest>();
			Registry.query(WebdriverTest.class).implementations()
					.forEach(t -> testTemplates.put(t.getClass(), t));
		}
		return testTemplates;
	}

	public static class TestException implements ProcessObservable {
		private WebdriverTest test;

		public TestException(WebdriverTest test) {
			this.test = test;
		}

		public WebdriverTest getTest() {
			return this.test;
		}

		public void setTest(WebdriverTest test) {
			this.test = test;
		}

		@Override
		public String toString() {
			return test.toString();
		}
	}
}
