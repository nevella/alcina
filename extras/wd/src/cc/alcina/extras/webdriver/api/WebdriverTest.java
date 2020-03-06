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
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.MetricLogging;

@RegistryLocation(registryPoint = WebdriverTest.class)
public abstract class WebdriverTest {
	protected static Map<Class<? extends WebdriverTest>, WebdriverTest> testTemplates;

	protected int myLevel;

	@SuppressWarnings("unchecked")
	public Class<? extends WebdriverTest>[] childTests() {
		return new Class[0];
	}

	public Enum<?>[] depends() {
		return new Enum[0];
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
								results.add(0, c.newInstance());
								added.add(c);
							}
						} catch (Exception e2) {
							throw new WrappedRuntimeException(e2,
									SuggestedAction.NOTIFY_WARNING);
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
		this.myLevel = level;
		String oldThreadName = Thread.currentThread().getName();
		Thread.currentThread()
				.setName("Test--" + token.getConfiguration().name);
		// dependencies, then child tests, then me
		TestResult testResult = new TestResult();
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
		token.getWriter().write(
				Ax.format("Starting test: %s - \n", getClass().getSimpleName()),
				level);
		Class<? extends WebdriverTest>[] childTests = childTests();
		if (childTests.length != 0) {
			level++;
			token.getWriter().write("Processing child tests - \n", level);
			for (Class<? extends WebdriverTest> tc : childTests) {
				WebdriverTest childTest = tc.newInstance();
				childTest.process(token, level, testResult);
			}
			level--;
		}
		testResult.setResultType(TestResultType.OK);
		try {
			int retryCount = 1;
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
					if (--retryCount == 0) {
						throw e;
					} else {
						System.out.println("Unit test execption - retrying ");
						System.out.println(e);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			testResult.setResultType(TestResultType.ERROR);
			testResult.setMessage(e.getMessage());
		} finally {
			long endTime = System.currentTimeMillis();
			testResult.setEndTime(endTime);
		}
		token.getWriter().write(testResult.toString(), level);
		Thread.currentThread().setName(oldThreadName);
		return testResult;
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

	protected void getAndLog(WebDriver driver, String uri) {
		String key = "Load: " + uri;
		MetricLogging.get().start(key);
		driver.get(uri);
		MetricLogging.get().end(key);
	}

	@SuppressWarnings("unchecked")
	protected Map<Class<? extends WebdriverTest>, WebdriverTest>
			getTestTemplates() {
		if (testTemplates == null) {
			testTemplates = new HashMap<Class<? extends WebdriverTest>, WebdriverTest>();
			@SuppressWarnings("rawtypes")
			List<Class> tests = Registry.get().lookup(WebdriverTest.class);
			try {
				for (Class<? extends WebdriverTest> c : tests) {
					testTemplates.put(c, (WebdriverTest) c.newInstance());
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e,
						SuggestedAction.NOTIFY_WARNING);
			}
		}
		return testTemplates;
	}
}
