package cc.alcina.extras.webdriver;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import cc.alcina.extras.webdriver.WDUtils.TestCallback;
import cc.alcina.extras.webdriver.WDUtils.TimedOutException;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.Io;

public class WdExec {
	private static final int DEFAULT_TIMEOUT = 5;

	private WebDriver driver;

	private String xpath;

	private String cssSelector;

	private int timeoutSecs = DEFAULT_TIMEOUT;

	private int index;

	private WDToken token;

	private String linkText;

	private String textMatchParent;

	private TestCallback testCallback;

	private WebElement fromElement;

	private boolean useScriptedClick;

	public Actions actions() {
		Actions actions = new Actions(driver);
		java.util.logging.Logger.getLogger(Actions.class.getName())
				.setLevel(java.util.logging.Level.WARNING);
		return actions;
	}

	public void assertContainsText(String text) {
		String elementText = getElement().getText();
		assert (elementText.contains(text));
	}

	public void assertExists() {
		assert (getElement() != null);
	}

	public void assertHasText() {
		assert (getElement().getText().length() > 0);
	}

	public void awaitRemoval(WebElement elt) {
		long timeoutAt = System.currentTimeMillis() + timeoutSecs * 1000;
		while (System.currentTimeMillis() < timeoutAt) {
			try {
				elt.isDisplayed();
				sleep(100);
			} catch (Exception e) {
				// removed
				int debug = 3;
				return;
			}
		}
		throw new TimedOutException();
	}

	public void clear() {
		getElement().clear();
	}

	public void clearAndEnterText(String string) {
		clear();
		click();
		sleep(1000);
		sendKeys(string);
	}

	public void clearAndSetText(String string) {
		clear();
		click();
		sleep(100);
		setTextAndFire(string);
	}

	public void clearBy() {
		linkText = null;
		cssSelector = null;
		xpath = null;
		textMatchParent = null;
	}

	public boolean click() {
		return click(false);
	}

	Consumer<WebElement> theClick = WebElement::click;

	public boolean click(boolean returnIfNotVisible) {
		return performAction(returnIfNotVisible, theClick);
	}

	public void clickLink(String linkText) {
		xpath(Ax.format("//a[.='%s']", linkText)).click();
	}

	public WdExec css(String cssSelector) {
		clearBy();
		this.cssSelector = cssSelector;
		return this;
	}

	public WdExec driver(WebDriver driver) {
		this.driver = driver;
		return this;
	}

	public Object executeScript(String script) {
		if (getBy() == null) {
			return WDUtils.executeScript(driver, null, script);
		} else {
			Ref<Object> scriptResult = new Ref<>();
			performAction(false, e -> scriptResult
					.set(WDUtils.executeScript(driver, e, script)));
			return scriptResult.get();
		}
	}

	/**
	 * Change the search context to be from the specified element (or the
	 * document element if fromElement is null)
	 */
	public WdExec from(WebElement fromElement) {
		this.fromElement = fromElement;
		return this;
	}

	public WebDriver getDriver() {
		return driver;
	}

	public WebElement getElement() {
		By by = getBy();
		if (by == null) {
			return null;
		}
		int oIndex = index;
		index = 0;
		return getElements().get(oIndex);
	}

	public List<WebElement> getElements() {
		By by = getBy();
		if (by == null) {
			return null;
		}
		return WDUtils.query().withBy(by).withCallback(testCallback)
				.withContext(fromElement != null ? fromElement : driver)
				.withRequired(true).withTimeout(timeoutSecs).getElements();
	}

	public String getOuterHtml() {
		WebElement elem = getElement();
		return WDUtils.outerHtml(driver, elem);
	}

	public WdExec id(String id) {
		clearBy();
		this.xpath = Ax.format("//*[@id='%s']", id);
		return this;
	}

	public boolean immediateIsInteractable() {
		try {
			return immediate(this::click);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean immediate(Runnable runnable) {
		int oTimeoutSecs = timeoutSecs;
		try {
			LooseContext.pushWithTrue(WDUtils.CONTEXT_DONT_LOG_EXCEPTION);
			LooseContext.set(WDUtils.CONTEXT_OVERRIDE_TIMEOUT, 0);
			timeoutSecs = 0;
			runnable.run();
			return true;
		} catch (RuntimeException e) {
			if (e instanceof TimedOutException) {
			} else {
				throw e;
			}
		} finally {
			timeoutSecs = oTimeoutSecs;
			LooseContext.pop();
		}
		return false;
	}

	public boolean immediateTest() {
		return immediate(this::getElement);
	}

	public boolean immediateTest(String... paths) {
		return Arrays.stream(paths).anyMatch(path -> {
			return xpath(path).immediateTest();
		});
	}

	public WdExec index(int index) {
		this.index = index;
		return this;
	}

	public WdExec inputForLabel(String labelText) {
		return xpath("label[.='%s']/preceding-sibling::input", labelText);
	}

	public WdExec linkText(String linkText) {
		clearBy();
		this.linkText = linkText;
		return this;
	}

	public void outer() {
		WebElement elem = getElement();
		System.out.println(WDUtils.outerHtml(driver, elem));
	}

	public boolean performAction(boolean returnIfNotVisible,
			Consumer<WebElement> actor) {
		RuntimeException lastException = null;
		int maxTries = Math.max(1, timeoutSecs * 5);
		boolean obscuredBySelf = false;
		for (int attempt = 0; attempt < maxTries; attempt++) {
			WebElement elem = getElement();
			Actions actions = new Actions(driver);
			actions.moveToElement(elem);
			if (WDUtils.isForceTimeout()) {
				throw new TimedOutException("forced timeout");
			}
			try {
				elem = getElement();
				actor.accept(elem);
				return false;
			} catch (RuntimeException e) {
				lastException = e;
				if (e instanceof ElementNotInteractableException) {
					if (returnIfNotVisible) {
						return true;
					}
					if (!obscuredBySelf && useScriptedClick) {
						String selfReceivedPattern = "element click intercepted: Element (<.+?) is not clickable.+receive the click: (<.+)\n";
						Pattern p = Pattern.compile(selfReceivedPattern);
						Matcher m = p.matcher(e.getMessage());
						if (m.find()) {
							if (Objects.equals(m.group(1), m.group(2))) {
								obscuredBySelf = true;
								actor = elem2 -> WDUtils.executeScript(driver,
										elem2, "arguments[0].click()");
								// Ax.out("Obscured by self :: using scripted
								// click");
								continue;
							}
						}
					}
					WDUtils.scrollToCenterUsingBoundingClientRect(driver, elem);
					sleep(200);
				} else if (e instanceof StaleElementReferenceException) {
					if (returnIfNotVisible) {
						return true;
					}
					// WDUtils.scrollToCenterUsingBoundingClientRect(driver,
					// elem);
					sleep(200);
				} else if (e instanceof ElementNotInteractableException) {
					if (returnIfNotVisible) {
						return true;
					}
					String message = e.getMessage();
					if (message.contains("is not clickable")) {
						if (ignoreSpuriousOtherElementWouldReceiveClickException(
								elem, message)) {
							return false;
						}
						WDUtils.scrollToCenterUsingBoundingClientRect(driver,
								elem);
						sleep(200);
					}
					// WDUtils.scrollToCenterUsingBoundingClientRect(driver,
					// elem);
					sleep(200);
				} else {
					String message = e.getMessage();
					if (message.contains("is not clickable")) {
						if (ignoreSpuriousOtherElementWouldReceiveClickException(
								elem, message)) {
							return false;
						}
						WDUtils.scrollToCenterUsingBoundingClientRect(driver,
								elem);
						sleep(200);
					} else {
						throw e;
					}
				}
			}
		}
		throw lastException;
	}

	public String readRelativeUrl(String relativeUrl) {
		try {
			String url = CommonUtils.combinePaths(token.getConfiguration().uri,
					relativeUrl);
			return Io.read().url(url).asString();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void scrollIntoView() {
		WDUtils.scrollIntoView(driver, getElement());
	}

	public void selectFancySelectItem(String formFieldName,
			String selectItemText) {
		xpath("//div[@container-name='%s']//div[@class='select-item-container']",
				formFieldName).click();
		xpath("//div[@class='dropdown-popup alcina-Selector']//div[@class='select-item-container']//a[.='%s']",
				selectItemText).click();
	}

	public void selectItemByIndex(int idx) {
		WDUtils.setSelectedIndex(token, driver, getElement(), idx);
	}

	public void selectItemByText(String text) {
		WDUtils.setSelectedText(token, driver, getElement(), text);
	}

	public void sendKeys(CharSequence string) {
		performAction(false, e -> e.sendKeys(string));
	}

	public void setFancyInputTextAndFire(String labelText, String inputText) {
		xpath("//label[.='%s']", labelText).click();
		xpath("//label[.='%s']/ancestor::div/input", labelText)
				.setTextAndFire(inputText);
	}

	public void setTextAndFire(String text) {
		String f_text = text.replace("\\", "\\\\");
		performAction(false,
				elem -> WDUtils.setProperty(driver, elem, "value", f_text));
		performAction(false, elem -> WDUtils.sendChanged(token, driver, elem));
	}

	public void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public WdExec testCallback(TestCallback testCallback) {
		this.testCallback = testCallback;
		return this;
	}

	public WdExec textMatchParent(String template, Object... args) {
		clearBy();
		textMatchParent = Ax.format(template, args);
		return this;
	}

	public WdExec timeout(int timeoutSecs) {
		this.timeoutSecs = timeoutSecs;
		return this;
	}

	public void toDefaultTimeout() {
		timeout(DEFAULT_TIMEOUT);
	}

	public WdExec token(WDToken token) {
		this.token = token;
		if (driver == null) {
			driver = token.getWebDriver();
		}
		return this;
	}

	public void waitFor() {
		for (int i = 0; i < timeoutSecs * 10; i++) {
			if (immediateTest()) {
				return;
			} else {
				sleep(100);
			}
		}
		throw new TimedOutException(
				Ax.format("Timed out waiting for %s", getBy()));
	}

	public void waitForOneOf(String... paths) {
		String xpath = Arrays.stream(paths).collect(Collectors.joining(" or "));
		for (int i = 0; i < timeoutSecs * 10; i++) {
			boolean found = (boolean) executeScript(Ax.format(
					"return document.evaluate(\"%s\",document).booleanValue",
					xpath));
			if (found) {
				return;
			} else {
				sleep(100);
			}
		}
		throw new TimedOutException(
				Ax.format("Timed out waiting for %s", Arrays.asList(paths)));
	}

	public WdExec xpath(String xpath) {
		return xpath(xpath, new Object[0]);
	}

	public WdExec xpath(String xpath, Object... args) {
		clearBy();
		this.xpath = Ax.format(xpath, args);
		return this;
	}

	private By getBy() {
		if (cssSelector != null) {
			return By.cssSelector(cssSelector);
		}
		if (xpath != null) {
			return By.xpath(xpath);
		}
		if (linkText != null) {
			return By.linkText(linkText);
		}
		if (textMatchParent != null) {
			return new ByTextMatchParent(textMatchParent);
		}
		return null;
	}

	private boolean ignoreSpuriousOtherElementWouldReceiveClickException(
			WebElement elem, String message) {
		/*
		 * possible chrome driver/version issue
		 */
		Pattern p = Pattern.compile(
				"(?s).+Element (<.+?>).* is not clickable at point.+?(<.+?>).*");
		Matcher m = p.matcher(message);
		if (m.matches() && m.group(1).equals(m.group(2))) {
			WDUtils.executeScript(driver, elem, "arguments[0].click();");
			return true;
		} else {
			return false;
		}
	}

	class ByTextMatchParent extends By {
		private String textMatchParent;

		public ByTextMatchParent(String textMatchParent) {
			this.textMatchParent = textMatchParent;
		}

		@Override
		public List<WebElement> findElements(SearchContext context) {
			String js = Io.read().resource("res/matching-text.js").asString();
			js = js.replace("##regex##", textMatchParent);
			long timeoutAt = System.currentTimeMillis() + timeoutSecs * 1000;
			while (System.currentTimeMillis() < timeoutAt) {
				WebElement result = (WebElement) WDUtils.executeScript(driver,
						null, js);
				if (result != null) {
					return Stream.of(result).collect(Collectors.toList());
				} else {
					sleep(100);
				}
			}
			throw new TimedOutException();
		}
	}

	/**
	 * A workaround for shadow-dom click interceptions (by self)
	 * 
	 * @param useScriptedClick
	 */
	public void useScriptedClick(boolean useScriptedClick) {
		this.useScriptedClick = useScriptedClick;
	}
}
