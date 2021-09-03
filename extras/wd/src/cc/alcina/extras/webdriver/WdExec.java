package cc.alcina.extras.webdriver;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import cc.alcina.extras.webdriver.WDUtils.TestCallback;
import cc.alcina.extras.webdriver.WDUtils.TimedOutException;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;

public class WdExec {
	private WebDriver driver;

	private String xpath;

	private String cssSelector;

	private int timeoutSecs;

	private int index;

	private WDToken token;

	private String linkText;

	private String textMatchParent;

	private TestCallback testCallback;

	private WebElement externalElement;

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

	public boolean click(boolean returnIfNotVisible) {
		return performAction(returnIfNotVisible, WebElement::click);
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
		return WDUtils.executeScript(driver, getElement(), script);
	}

	public void externalElement(WebElement element) {
		externalElement = element;
	}

	public WebDriver getDriver() {
		return driver;
	}

	public WebElement getElement() {
		if (externalElement != null) {
			return externalElement;
		}
		By by = getBy();
		if (by == null) {
			return null;
		}
		int oIndex = index;
		index = 0;
		if (oIndex == 0) {
			return WDUtils.waitForElement(driver, by, timeoutSecs,
					testCallback);
		} else {
			return WDUtils.waitForElements(driver, by, timeoutSecs, true)
					.get(oIndex);
		}
	}

	public List<WebElement> getElements() {
		By by = getBy();
		if (by == null) {
			return null;
		}
		return WDUtils.waitForElements(driver, by, timeoutSecs, true);
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

	public boolean immediateTest() {
		int oTimeoutSecs = timeoutSecs;
		try {
			LooseContext.pushWithTrue(WDUtils.CONTEXT_DONT_LOG_EXCEPTION);
			LooseContext.set(WDUtils.CONTEXT_OVERRIDE_TIMEOUT, 0);
			timeoutSecs = 0;
			getElement();
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
		for (int i = 0; i < Math.max(1, timeoutSecs * 5); i++) {
			WebElement elem = getElement();
			Actions actions = new Actions(driver);
			actions.moveToElement(elem);
			if (WDUtils.forceTimeout) {
				throw new TimedOutException("forced timeout");
			}
			try {
				actor.accept(elem);
				return false;
			} catch (RuntimeException e) {
				lastException = e;
				if (e instanceof ElementNotVisibleException) {
					if (returnIfNotVisible) {
						return true;
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
			String url = SEUtilities.combinePaths(token.getConfiguration().uri,
					relativeUrl);
			return ResourceUtilities.readUrlAsString(url);
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
		WebElement elem = getElement();
		text = text.replace("\\", "\\\\");
		WDUtils.setProperty(driver, elem, "value", text);
		WDUtils.sendChanged(token, driver, elem);
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

	public WdExec token(WDToken token) {
		this.token = token;
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
		throw new TimedOutException();
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
		throw new TimedOutException();
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
			String js = ResourceUtilities
					.readClassPathResourceAsStringPreferFile(WdExec.class,
							"res/matching-text.js",
							"/private/var/local/git/webdriver/src/au/com/barnet/webdriver/res/matching-text.js");
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
}
