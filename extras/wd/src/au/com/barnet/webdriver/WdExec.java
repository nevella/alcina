package au.com.barnet.webdriver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import au.com.barnet.webdriver.WDUtils.TimedOutException;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;

public class WdExec {
	private WebDriver driver;

	private String xpath;

	private String cssSelector;

	private int timeoutSecs;

	private int index;

	private WDToken token;

	private String linkText;

	private String textMatchParent;

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

	public boolean click() {
		return click(false);
	}

	/**
	 * 
	 * @param returnIfNotVisible
	 * @return true if not visible
	 */
	public boolean click(boolean returnIfNotVisible) {
		RuntimeException lastException = null;
		for (int i = 0; i < timeoutSecs * 5; i++) {
			WebElement elem = getElement();
			Actions actions = new Actions(driver);
			actions.moveToElement(elem);
			if (WDUtils.forceTimeout) {
				throw new TimedOutException("forced timeout");
			}
			try {
				elem.click();
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
				} else {
					if (e.getMessage().contains("is not clickable")) {
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

	public void clickLink(String linkText) {
		xpath(Ax.format("//a[.='%s']", linkText)).click();
	}

	public WdExec css(String cssSelector) {
		clearByParams();
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
		if (oIndex == 0) {
			return WDUtils.waitForElement(driver, by, timeoutSecs);
		} else {
			return WDUtils.waitForElements(driver, by, timeoutSecs, true)
					.get(oIndex);
		}
	}

	public String getOuterHtml() {
		WebElement elem = getElement();
		return WDUtils.outerHtml(driver, elem);
	}

	public WdExec id(String id) {
		clearByParams();
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
		clearByParams();
		this.linkText = linkText;
		return this;
	}

	public void outer() {
		WebElement elem = getElement();
		System.out.println(WDUtils.outerHtml(driver, elem));
	}

	public void scrollIntoView() {
		WDUtils.scrollIntoView(driver, getElement());
	}

	public void selectFancySelectItem(String formFieldName,
			String selectItemText) {
		xpath("//div[@name='%s']//div[@class='select-item-container']",
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
		getElement().sendKeys(string);
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

	public WdExec textMatchParent(String template, Object... args) {
		clearByParams();
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
		for (int i = 0; i < 30; i++) {
			if (immediateTest()) {
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
		clearByParams();
		this.xpath = Ax.format(xpath, args);
		return this;
	}

	private void clearByParams() {
		linkText = null;
		cssSelector = null;
		xpath = null;
		textMatchParent = null;
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
