package cc.alcina.extras.webdriver.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.WebDriver;

import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WDUtils;
import cc.alcina.extras.webdriver.WDUtils.TimedOutException;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.MetricLogging;

/**
 * <p>
 * Models a test in a test tree
 *
 * <p>
 * Tests are evaluated as follows: [evaluate tests which fulfil
 * dependencies][evaluate children][evaluate self]
 *
 * 
 *
 */
@Registration(WebdriverTest.class)
public abstract class WebdriverTest implements Registration.Ensure {
	public static final String CONTEXT_CURRENT_TEST = WebdriverTest.class
			.getName() + ".CONTEXT_CURRENT_TEST";

	protected static Map<Class<? extends WebdriverTest>, WebdriverTest> testTemplates;

	private static final String ADD_GWT_CLIENT_SUFFIX = "addGwtClientSuffix";

	protected static String addGwtClientUrl(String url) {
		if (Configuration.key(ADD_GWT_CLIENT_SUFFIX).is()) {
			String suffix = "?gwt.codesvr=127.0.0.1:9997";
			return url.contains(suffix) ? url : url + suffix;
		} else {
			return url;
		}
	}

	public static <T extends WebdriverTest> T current() {
		return LooseContext.get(CONTEXT_CURRENT_TEST);
	}

	protected int myLevel;

	protected TestResult result;

	private StringMap configuration = new StringMap();

	protected transient WDToken token;

	protected transient WdExec exec;

	private boolean cancelDueToError(int level) {
		if (token.getRootResult()
				.computeTreeResultType() == TestResultType.ERROR) {
			token.getWriter().write("cancelled - prior error", level);
			return true;
		} else {
			return false;
		}
	}

	// -->dependent tests
	public Class<? extends WebdriverTest>[] childTests() {
		return new Class[0];
	}

	public Enum<?>[] depends() {
		return new Enum[0];
	}

	public final WebDriver driver() {
		return token.getWebDriver();
	}

	public void ensurePath(String path) {
		try {
			String url = pathToUrl(path);
			String curr = driver().getCurrentUrl();
			if (!Objects.equals(curr, url)) {
				goToUri(url);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void getAndLog(String uri) {
		String key = "Load: " + uri;
		MetricLogging.get().start(key);
		goToUri(uri);
		token.setLoadedUrl(uri);
		MetricLogging.get().end(key);
	}

	protected List<WebdriverTest> getChildTests() {
		return Arrays.stream(childTests()).map(t -> {
			try {
				return t.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}).collect(Collectors.toList());
	}

	public StringMap getConfiguration() {
		return this.configuration;
	}

	public WdExec getExec() {
		return this.exec;
	}

	public List<WebdriverTest> getRequiredDependentTests() {
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

	public TestResult getResult() {
		return this.result;
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

	public WDToken getToken() {
		return this.token;
	}

	public void goToHash(String hash) {
		try {
			String curr = driver().getCurrentUrl();
			curr = String.format("%s#%s", curr.replaceFirst("#.*", ""), hash);
			goToUri(curr);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void goToPath(String path) {
		try {
			String url = pathToUrl(path);
			goToUri(url);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void goToUri(String url) {
		Ax.out(" --> " + url);
		driver().get(url);
	}

	protected void initialiseContext() {
		if (configuration != null) {
			LooseContext.getContext().addProperties(configuration, false);
		}
	}

	public boolean noTimePayload() {
		return false;
	}

	protected void onAfterProcess() {
	}

	protected void onBeforeChildTests() {
	}

	protected void onBeforeDependentTests() {
	}

	protected void onBeforeProcess() {
		token.ensureDriver();
		WebDriver driver = token.getWebDriver();
		exec = new WdExec().driver(driver).token(token).timeout(5);
	}

	public void onTimeoutException(TimedOutException timedOutException) {
		result.setException(timedOutException);
		ProcessObservers.publish(WebdriverTest.TestException.class,
				() -> new WebdriverTest.TestException(this));
	}

	public String pathToUrl(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String curr = driver().getCurrentUrl();
		String hostAndProtocol = curr.replaceFirst("(http.?://[^/]+).*", "$1");
		String url = hostAndProtocol + path;
		url = addGwtClientUrl(url);
		return url;
	}

	public void predelay(int level) {
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

	public final TestResult process(WDToken token, int level, TestResult parent)
			throws Exception {
		try {
			LooseContext.pushWithKey(CONTEXT_CURRENT_TEST, this);
			this.token = token;
			initialiseContext();
			onBeforeProcess();
			return process0(level, parent);
		} catch (Throwable e) {
			if (level == 0 && e instanceof CancelParentsException) {
				// result will be in error state due to descendant exception
				return result;
			} else {
				Ax.sysLogHigh("WDT framework - %s",
						CommonUtils.toSimpleExceptionMessage(e));
				throw e;
			}
		} finally {
			if (token.getRootResult() != null) {
				onAfterProcess();
			}
			LooseContext.pop();
		}
	}

	private TestResult process0(int level, TestResult parent) throws Exception {
		this.myLevel = level;
		String oldThreadName = Thread.currentThread().getName();
		Thread.currentThread()
				.setName("Test--" + token.getConfiguration().name);
		result = new TestResult();
		result.setStartTime(System.currentTimeMillis());
		result.setNoTimePayload(noTimePayload());
		result.setName(getClass().getSimpleName());
		result.setParent(parent);
		token.getWriter().write(
				Ax.format("Test: %s - \n", getClass().getSimpleName()), level);
		if (parent == null) {
			if (Configuration.is("activateOsxChromeOnRootTestStart")) {
				WDUtils.activateOsxChrome();
			}
			token.setRootResult(result);
			result.setRootResult(true);
			predelay(level + 1);
			result.setStartTime(System.currentTimeMillis());
		} else {
			parent.addResult(result);
		}
		if (cancelDueToError(level)) {
			return result;
		}
		level++;
		List<WebdriverTest> dependentTests = getRequiredDependentTests();
		onBeforeDependentTests();
		if (!dependentTests.isEmpty()) {
			level++;
			token.getWriter().write("Processing dependencies - \n", level);
			for (WebdriverTest dependent : dependentTests) {
				// confirm still required (may have been processed by a
				// dependency)
				List<WebdriverTest> recheck = getRequiredDependentTests();
				if (recheck.stream()
						.anyMatch(r -> r.getClass() == dependent.getClass())) {
					dependent.process(token, level, result);
				}
			}
			level--;
		}
		if (cancelDueToError(level)) {
			return result;
		}
		onBeforeChildTests();
		long startTime = System.currentTimeMillis();
		token.getWriter().write(
				Ax.format("Starting test: %s - \n", getClass().getSimpleName()),
				level);
		List<WebdriverTest> childTests = getChildTests();
		if (childTests.size() != 0) {
			level++;
			token.getWriter().write("Processing child tests - \n", level);
			for (WebdriverTest childTest : childTests) {
				childTest.process(token, level, result);
			}
			level--;
		}
		result.setResultType(TestResultType.OK);
		try {
			int maxAttempts = getRetryCount();
			int attempt = 1;
			while (true) {
				try {
					result.setStartTimeExcludingDependent(
							System.currentTimeMillis());
					String message = "running test - "
							+ getClass().getSimpleName();
					if (level == 0) {
						System.err.println(message);
					} else {
						System.out.println(
								CommonUtils.padStringLeft("", level, " ")
										+ message);
					}
					run();
					uiStateChange(providesUIState());
					uiStateChange(returnsUIState());
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
			result.setResultType(TestResultType.ERROR);
			result.setMessage(e.getMessage());
			result.setException(e);
			if (!(e instanceof TimedOutException)) {
				// will have already been published
				ProcessObservers.publish(WebdriverTest.TestException.class,
						() -> new WebdriverTest.TestException(this));
			}
		} finally {
			long endTime = System.currentTimeMillis();
			result.setEndTime(endTime);
		}
		token.getWriter().write(result.toString(), level);
		Thread.currentThread().setName(oldThreadName);
		if (!result.providePassed()) {
			throw new CancelParentsException();
		}
		return result;
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

	public abstract void run() throws Exception;

	public void setConfiguration(StringMap configuration) {
		this.configuration = configuration;
	}

	@Override
	public String toString() {
		FormatBuilder builder = new FormatBuilder().separator(" :: ");
		builder.appendIfNotBlank(getClass().getName());
		if (result != null && result.getException() != null) {
			builder.appendIfNotBlank(
					result.getException().getClass().getSimpleName());
			builder.appendIfNotBlank(result.getException().getMessage());
		}
		return builder.toString();
	}

	public void uiStateChange(Enum<?> e) {
		token.getUiStates().put(e.getDeclaringClass(), e);
	}

	public void uiStateChange(Enum<?>[] enums) {
		for (Enum<?> e : enums) {
			uiStateChange(e);
		}
	}

	public static class CancelParentsException extends RuntimeException {
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
