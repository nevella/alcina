package cc.alcina.extras.webdriver;

import java.awt.HeadlessException;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;

import cc.alcina.extras.webdriver.WDConfigurationItem.WebDriverType;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.ShellWrapper;

public class WDUtils {
	public static final Locale EN_AU = new Locale("en", "AU", "");

	private static final TimeZone SYDNEY_TZ = TimeZone
			.getTimeZone("Australia/Sydney");

	public static final DateFormat DATE_FORMAT_T = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss", EN_AU);

	public static final String CONTEXT_DONT_LOG_EXCEPTION = WDUtils.class
			.getName() + ".CONTEXT_DONT_LOG_EXCEPTION";

	public static final String CONTEXT_OVERRIDE_TIMEOUT = WDUtils.class
			.getName() + ".CONTEXT_OVERRIDE_TIMEOUT";

	public static final String CONTEXT_IGNORE_OVERRIDE_TIMEOUT = WDUtils.class
			.getName() + ".CONTEXT_IGNORE_OVERRIDE_TIMEOUT";

	public static final String CONTEXT_FAST_ENTER_TEXT = WDUtils.class.getName()
			+ ".CONTEXT_FAST_ENTER_TEXT";
	static {
		DATE_FORMAT_T.setTimeZone(SYDNEY_TZ);
	}

	public static Callback exceptionCallback;

	public static boolean forceTimeout;

	public static void activateOsxChrome() {
		try {
			new ShellWrapper().noLogging().runBashScript(
					"osascript -e 'activate application \"Google Chrome\"'");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activateOsxChromium() {
		try {
			new ShellWrapper().noLogging().runBashScript(
					"osascript -e 'activate application \"Chromium\"'");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activateOsxFirefox() {
		try {
			new ShellWrapper().noLogging().runBashScript(
					"osascript -e 'activate application \"Firefox\"'");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void click(WebElement clickMe) {
		int maxRetries = 100;
		while (true) {
			try {
				clickMe.click();
				break;
			} catch (RuntimeException e) {
				if (exceptionCallback != null) {
					try {
						exceptionCallback.apply(null);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					throw e;
				} else {
					if (maxRetries-- > 0) {
						sleep(100);
					} else {
						e.printStackTrace();
						throwAfterTimeout(e);
					}
				}
			}
		}
	}

	/**
	 * Click on an element matching the given xpath with a wait for it to become
	 * visible.
	 *
	 * @param driver
	 * @param xpath
	 */
	public static void clickWithWaitForVisible(WebDriver driver, String xpath) {
		WebElement elt = waitForElement(driver, xpath);
		clickWithWaitForVisible(elt);
	}

	public static void clickWithWaitForVisible(WebElement elt) {
		boolean ok = false;
		int maxRetries = 100;
		while (!ok) {
			if (forceTimeout) {
				throw new TimedOutException("forced timeout");
			}
			ok = true;
			try {
				elt.click();
			} catch (ElementNotVisibleException e) {
				ok = false;
				try {
					Thread.sleep(100);
					maxRetries--;
					if (maxRetries == 0) {
						throw e;
					}
				} catch (InterruptedException e1) {
				}
			}
		}
	}

	/**
	 * Do a Control+click on a given element
	 *
	 * @param driver
	 * @param xpath
	 *            The xpath to the source element
	 */
	public static void controlClick(WebDriver driver, String xpath) {
		WebElement elt = waitForElement(driver, xpath);
		Actions actions = new Actions(driver);
		actions.keyDown(Keys.LEFT_CONTROL).click(elt).keyUp(Keys.LEFT_CONTROL)
				.build().perform();
	}

	/**
	 * Drag and drop given element to a specified target.
	 *
	 * @param driver
	 * @param source
	 *            The xpath to the source element.
	 * @param target
	 *            The xpath to the target elements.
	 * @throws InterruptedException
	 */
	public static void dragAndDrop(WebDriver driver, String source,
			String target) throws InterruptedException {
		WebElement from = WDUtils.waitForElement(driver, source);
		WebElement to = WDUtils.waitForElement(driver, target);
		((JavascriptExecutor) driver).executeScript(
				"window.simulateDragDrop($(arguments[0]), $(arguments[1]) );",
				from, to);
		Thread.sleep(100);
	}

	/**
	 * Determine if an element exists.
	 *
	 * @param driver
	 * @param by
	 * @return
	 */
	public static boolean elementExists(WebDriver driver, By by) {
		try {
			driver.findElement(by);
		} catch (NoSuchElementException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Determine if an element exists by xpath.
	 *
	 * @param driver
	 * @param xpath
	 * @return
	 */
	public static boolean elementExists(WebDriver driver, String xpath) {
		By by = By.xpath(xpath);
		return elementExists(driver, by);
	}

	/**
	 * Enter the given text in the element given by the xpath expression.
	 *
	 * @param driver
	 * @param xpath
	 * @param textToEnter
	 * @throws InterruptedException
	 */
	public static void enterText(WebDriver driver, String xpath,
			String textToEnter) throws InterruptedException {
		if (LooseContext.is(CONTEXT_FAST_ENTER_TEXT)) {
			// faux
			WDToken token = new WDToken();
			WDConfigurationItem configuration = new WDConfigurationItem();
			configuration.driverType = WebDriverType.CHROME_LOCAL;
			token.setConfiguration(configuration);
			new WdExec().driver(driver).token(token).timeout(1000).xpath(xpath)
					.setTextAndFire(textToEnter);
		} else {
			WebElement elt = WDUtils.waitForElement(driver, xpath);
			clickWithWaitForVisible(elt);
			elt.clear();
			elt.sendKeys(textToEnter);
			Thread.sleep(500);
		}
	}

	public static Object executeScript(WebDriver driver, WebElement elt,
			String script) {
		return ((org.openqa.selenium.JavascriptExecutor) driver)
				.executeScript(script, elt);
	}

	public static void focus(WebDriver driver, WebElement elt, boolean focus) {
		executeScript(driver, elt,
				Ax.format("arguments[0].%s()", focus ? "focus" : "blur"));
	}

	public static void focusWindow(RemoteWebDriver driver) {
		String script = "window.focus()";
		Object result = executeScript(driver, null, script);
	}

	/**
	 * Get the text from an element specified by the xpath.
	 *
	 * @param driver
	 * @param xpath
	 * @return
	 */
	public static String getTextFromElement(WebDriver driver, String xpath) {
		WebElement elt = waitForElement(driver, xpath);
		return elt.getText();
	}

	/**
	 * Get the text inside an input element.
	 *
	 * @param driver
	 * @param xpath
	 * @return
	 */
	public static String getTextFromInputElement(WebDriver driver,
			String xpath) {
		WebElement elt = waitForElement(driver, xpath);
		return elt.getAttribute("value");
	}

	public static String innerHtml(WebDriver driver, WebElement element) {
		return (String) ((org.openqa.selenium.JavascriptExecutor) driver)
				.executeScript("return arguments[0].innerHTML;", element);
	}

	public static boolean isDisplayedAndNotNull(WebElement elt) {
		return elt != null && elt.getSize().height > 0 && elt.isDisplayed();
	}

	public static void maximize(WebDriver driver) {
	}

	public static void maximize(WebDriver driver, WebDriverType type) {
		switch (type) {
		case CHROME:
		case CHROME_NO_PROFILE:
			// case CHROME_LOCAL:
			return;
		}
		try {
			driver.manage().window().setPosition(new Point(0, 0));
			java.awt.Dimension screenSize = new java.awt.Dimension(
					ResourceUtilities.getInteger(WDUtils.class, "width"),
					ResourceUtilities.getInteger(WDUtils.class, "height"));
			try {
				screenSize = java.awt.Toolkit.getDefaultToolkit()
						.getScreenSize();
			} catch (Exception e) {
				if (e instanceof HeadlessException) {
				} else {
					throw e;
				}
			}
			Dimension dim = new Dimension((int) screenSize.getWidth(),
					(int) screenSize.getHeight());
			driver.manage().window().setSize(dim);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String outerHtml(WebDriver driver, WebElement element) {
		return (String) ((org.openqa.selenium.JavascriptExecutor) driver)
				.executeScript("return arguments[0].outerHTML;", element);
	}

	public static String pathBeforeQueryString(String uri, String replace) {
		String regex = "^(http://[^/]+)(/.*?)(\\?.+)?\\z";
		return uri.replaceFirst(regex, "$1" + replace + "$3");
	}

	public static void scrollIntoView(WebDriver driver, WebElement elt) {
		executeScript(driver, elt, "arguments[0].scrollIntoView()");
	}

	public static void scrollToCenterUsingBoundingClientRect(WebDriver driver,
			WebElement elem) {
		String script = "var elem = arguments[0];var rect = elem.getBoundingClientRect();var originalScrollTop = document.documentElement.scrollTop;var delta = rect.top - (document.documentElement.clientHeight / 2);window.scrollTo(0, originalScrollTop + delta);return {    scrollTop : document.documentElement.scrollTop,    rectTop : rect.top,    originalScrollTop : originalScrollTop,    delta : delta};";
		script = script.replace(";", ";\n");
		try {
			Object result = executeScript(driver, elem, script);
		} catch (Exception e) {
			Ax.err(CommonUtils.toSimpleExceptionMessage(e));
		}
	}

	public static void sendChanged(WDToken token, WebDriver driver,
			WebElement elt) {
		try {
			switch (token.getConfiguration().driverType) {
			case IE11:
			case IE9:
				executeScript(driver, elt,
						" var event = document.createEvent('Event');"
								+ "event.initEvent('change', true, true);"
								+ "arguments[0].dispatchEvent(event);");
				break;
			default:
				executeScript(driver, elt,
						"arguments[0].dispatchEvent(new Event('change', { 'bubbles': true }))");
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setProperty(WebDriver driver, WebElement elt, String key,
			String value) {
		executeScript(driver, elt,
				Ax.format("arguments[0].%s='%s'", key, value));
	}

	public static void setSelectedIndex(WDToken token, WebDriver driver,
			WebElement elt, int idx) {
		try {
			switch (token.getConfiguration().driverType) {
			case IE11:
			case IE9:
				executeScript(driver, elt, Ax.format(
						"arguments[0].selectedIndex=%s; var event = document.createEvent('Event');"
								+ "event.initEvent('change', true, true);"
								+ "arguments[0].dispatchEvent(event);",
						idx));
				break;
			default:
				executeScript(driver, elt, Ax.format(
						"arguments[0].selectedIndex=%s;arguments[0].dispatchEvent(new Event('change', { 'bubbles': true }))",
						idx));
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setSelectedText(WDToken token, WebDriver driver,
			WebElement elt, String text) {
		try {
			switch (token.getConfiguration().driverType) {
			case IE11:
			case IE9:
				executeScript(driver, elt,
						Ax.format("var options = arguments[0].options;"
								+ "var text = '%s';"
								+ "for (var idx=0;idx<options.length;idx++){"
								+ "if(options[idx].text==text){"
								+ "arguments[0].selectedIndex=idx;"
								+ "var event = document.createEvent('Event');"
								+ "event.initEvent('change', true, true);"
								+ "arguments[0].dispatchEvent(event);" + "}"
								+ "}", text));
				break;
			default:
				executeScript(driver, elt,
						Ax.format("var options = arguments[0].options;"
								+ "var text = '%s';"
								+ "for (var idx=0;idx<options.length;idx++){"
								+ "if(options[idx].text==text){"
								+ "arguments[0].selectedIndex=idx;"
								+ "arguments[0].dispatchEvent(new Event('change', { 'bubbles': true }));"
								+ "}" + "}", text));
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e,
					SuggestedAction.NOTIFY_WARNING);
		}
	}

	public static void waitForAttribute(WebElement elt, String attr,
			String value, int timeout) {
		int j = 0;
		while (timeout > 0) {
			String v = elt.getAttribute(attr);
			if (v != null && v.equals(value)) {
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if (j++ % 10 == 0) {
				timeout--;
			}
		}
		throw new TimedOutException(Ax
				.format("Wait for attr/value [%s,%s] timed out", attr, value));
	}

	public static WebElement waitForElement(SearchContext context, By by,
			double timeout, TestCallback cb, boolean required) {
		timeout = maybeOverrideTimeout(timeout);
		int j = 0;
		long start = System.currentTimeMillis();
		Exception lastException = null;
		while (System.currentTimeMillis() - start < timeout * 1000
				|| timeout == 0.0) {
			if (forceTimeout) {
				throw new TimedOutException("forced timeout");
			}
			try {
				WebElement element = context.findElement(by);
				if (element != null) {
					if (cb == null || cb.ok(element)) {
						return element;
					}
				}
			} catch (Exception e) {
				lastException = e;
				if (e instanceof InvalidSelectorException) {
					throw e;
				}
			}
			if (timeout == 0.0) {
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		if (required) {
			if (!LooseContext.is(CONTEXT_DONT_LOG_EXCEPTION)) {
				throwAfterTimeout(logException(context, by));
				return null;
			} else {
				throw logException(context, by);
			}
		} else {
			return null;
		}
	}

	public static WebElement waitForElement(SearchContext context, By by,
			int timeout) {
		return waitForElement(context, by, timeout, null);
	}

	public static WebElement waitForElement(SearchContext context, By by,
			int timeout, TestCallback cb) {
		return waitForElement(context, by, timeout, cb, true);
	}

	public static WebElement waitForElement(WebDriver driver, By by) {
		return waitForElement(driver, by, 10);
	}

	/**
	 * Return a web element by xpath after waiting for it.
	 *
	 * @param driver
	 * @param xpath
	 * @return The WebElement, after waiting for it.
	 */
	public static WebElement waitForElement(WebDriver driver, String xpath) {
		By by = By.xpath(xpath);
		return waitForElement(driver, by);
	}

	public static List<WebElement> waitForElements(SearchContext context, By by,
			int timeout) {
		return waitForElements(context, by, timeout, true);
	}

	public static List<WebElement> waitForElements(SearchContext context, By by,
			int timeout, boolean required) {
		int j = 0;
		timeout = maybeOverrideTimeout(timeout);
		long start = System.currentTimeMillis();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
		;// always ...just in case there are stale elements arround
		while (System.currentTimeMillis() - start < timeout * 1000) {
			if (forceTimeout) {
				throw new TimedOutException("forced timeout");
			}
			try {
				List<WebElement> elements = context.findElements(by);
				if (!CommonUtils.isNullOrEmpty(elements)) {
					return elements;
				}
			} catch (Exception e) {
				int debug = 3;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		if (required) {
			throw logException(context, by);
		} else {
			return new ArrayList<WebElement>();
		}
	}

	public static List<WebElement> waitForElements(WebDriver driver, By by) {
		return waitForElements(driver, by, 10);
	}

	/**
	 * Return list of web elements matching the given xpath selector.
	 *
	 * @param driver
	 * @param xpath
	 *            The xpath expression to use for matching.
	 * @return
	 */
	public static List<WebElement> waitForElements(WebDriver driver,
			String xpath) {
		By by = By.xpath(xpath);
		return waitForElements(driver, by, 10);
	}

	public static void waitForTextLength(WebElement elt, int minLength,
			int timeout) {
		int j = 0;
		timeout = maybeOverrideTimeout(timeout);
		while (timeout > 0) {
			if (forceTimeout) {
				throw new TimedOutException("forced timeout");
			}
			String v = elt.getText();
			if (v != null && v.length() > minLength) {
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if (j++ % 10 == 0) {
				timeout--;
			}
		}
		throw new TimedOutException(
				Ax.format("Wait for textlength [%s] timed out", minLength));
	}

	private static TimedOutException logException(SearchContext context,
			By by) {
		if (LooseContext.is(CONTEXT_DONT_LOG_EXCEPTION)) {
			return new TimedOutException(
					Ax.format("Wait for element [%s] timed out", by));
		}
		byte[] bytes = ((RemoteWebDriver) context)
				.getScreenshotAs(OutputType.BYTES);
		try {
			File dataFile = File.createTempFile("webdriver-err-", ".png");
			ResourceUtilities.writeBytesToFile(bytes, dataFile);
			WDToken token = LooseContext.get(WDManager.CONTEXT_TOKEN);
			if (token != null) {
				token.getWriter().write(
						String.format("Screenshot:\n%s\n", dataFile.getPath()),
						0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new TimedOutException(
				Ax.format("Wait for element [%s] timed out", by));
	}

	private static int maybeOverrideTimeout(double timeout) {
		if (LooseContext.has(CONTEXT_OVERRIDE_TIMEOUT)
				&& !LooseContext.has(CONTEXT_IGNORE_OVERRIDE_TIMEOUT)) {
			Integer override = LooseContext
					.getInteger(CONTEXT_OVERRIDE_TIMEOUT);
			return (int) (override > timeout ? override : timeout);
		} else {
			return (int) timeout;
		}
	}

	private static void throwAfterTimeout(RuntimeException e) {
		boolean tryAgain = false;
		int debug = 3;
		if (!tryAgain) {
			throw e;
		}
	}

	public interface TestCallback {
		public boolean ok(WebElement elt);
	}

	public static class TimedOutException extends RuntimeException {
		public TimedOutException() {
			super();
			forceTimeout = false;
		}

		public TimedOutException(String message) {
			super(message);
			forceTimeout = false;
		}
	}
}
