package cc.alcina.extras.webdriver.query;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ref;

public class ElementQuery {
	private static ThreadLocal<WebDriver> drivers = new ThreadLocal<>();

	public static boolean forceTimeout = false;

	public static void awaitRemoval(WebElement element) {
		if (element == null) {
			return;
		}
		from(element).awaitRemoval(2000.0);
	}

	public static ElementQuery from(SearchContext context) {
		return new ElementQuery(context);
	}

	public static ElementQuery xpath(String template) {
		return xpath(template, new Object[0]);
	}

	public static ElementQuery xpath(String template, Object... args) {
		String xpath = String.format(template, args);
		return new ElementQuery(DriverProvider.get().getDriver())
				.withXpath(xpath);
	}

	public interface DriverProvider {
		public static DriverProvider get() {
			return Registry.impl(DriverProvider.class);
		}

		WebDriver getDriver();
	}

	final SearchContext context;

	String xpath;

	String id;

	Predicate<WebElement> predicate;

	boolean required;

	double timeout = 5.0;

	private String css;

	public ElementQuery(SearchContext context) {
		this.context = context;
		required = true;
		if (context instanceof WebDriver) {
			WebDriver driver = (WebDriver) context;
			WebDriver threadDriver = drivers.get();
			if (threadDriver == null) {
				drivers.set(driver);
			}
		}
	}

	public void await() {
		getElement();
	}

	public void await(Runnable runnable) {
		WebElement element = getElement();
		runnable.run();
		awaitRemoval(element);
	}

	public void awaitRemoval(double timeout) {
		withXpath("//*");
		WebElement element = (WebElement) context;
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start <= timeout) {
			try {
				element.isDisplayed();
				Thread.sleep(10);
			} catch (StaleElementReferenceException e) {
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		throw new IllegalStateException(
				String.format("Element not removed :: %s", this));
	}

	public void click() {
		withElement(WebElement::click);
	}

	public ElementQuery descendant(String descendantXpath) {
		if (descendantXpath.startsWith("/")) {
			descendantXpath = "." + descendantXpath;
		}
		WebElement elementContext = getElement();
		return new ElementQuery(elementContext).withXpath(descendantXpath);
	}

	public void deselectOption(String optionText) {
		setSelected(optionText, false);
	}

	public WebElement getElement() {
		List<WebElement> elements = getElements();
		return elements.isEmpty() ? null : elements.get(0);
	}

	public List<WebElement> getElements() {
		Ref<List<WebElement>> receiver = new Ref<>();
		withElements(receiver::set);
		return receiver.get();
	}

	void withElement(Consumer<WebElement> consumer) {
		Consumer<List<WebElement>> intermediate = list -> consumer
				.accept(list.get(0));
		withElements(intermediate);
	}

	void withElements(Consumer<List<WebElement>> consumer) {
		long start = System.currentTimeMillis();
		drivers.get().manage().timeouts().implicitlyWait(Duration.ZERO);
		Exception lastException = null;
		while (System.currentTimeMillis() - start < timeout * 1000
				|| timeout == 0.0) {
			if (forceTimeout) {
				throw new RuntimeException("forced timeout");
			}
			try {
				List<WebElement> elements = context.findElements(locator());
				if (elements.size() > 0) {
					if (predicate == null || predicate.test(elements.get(0))) {
						consumer.accept(elements);
						return;
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
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
		if (required) {
			throw new RequiredElementNotFoundException();
		} else {
			consumer.accept(new ArrayList<>());
			return;
		}
	}

	By locator() {
		if (xpath != null) {
			return By.xpath(xpath);
		} else if (id != null) {
			return By.id(id);
		} else if (css != null) {
			return By.cssSelector(css);
		} else {
			throw new IllegalStateException("No locator parameter set");
		}
	}

	class RequiredElementNotFoundException extends RuntimeException {
		RequiredElementNotFoundException() {
			super(String.format("Timed out - %s", ElementQuery.this));
		}
	}

	public boolean isPresent() {
		return withTimeout(0).withRequired(false).getElement() != null;
	}

	public ElementQuery clone() {
		ElementQuery copy = from(context);
		copy.predicate = predicate;
		copy.required = required;
		copy.timeout = timeout;
		copy.xpath = xpath;
		copy.id = id;
		return copy;
	}

	public String outerHtml() {
		return (String) ((org.openqa.selenium.JavascriptExecutor) drivers.get())
				.executeScript("return arguments[0].outerHTML;", getElement());
	}

	public void selectOption(String optionText) {
		setSelected(optionText, true);
	}

	public void sendKeys(String text) {
		getElement().sendKeys(text);
	}

	public void setSelected(String optionText, boolean selected) {
		WebElement webElement = getSelectOption(optionText);
		if (webElement.isSelected() ^ selected) {
			webElement.click();
		}
	}

	public WebElement getSelectOption(String optionText) {
		WebElement webElement = new Select(getElement()).getOptions().stream()
				.filter(o -> o.getText().equals(optionText)).findFirst()
				.orElse(null);
		return webElement;
	}

	@Override
	public String toString() {
		return String.format("Query %s : %s", context, locator());
	}

	public ElementQuery withPredicate(Predicate<WebElement> predicate) {
		ElementQuery clone = clone();
		clone.predicate = predicate;
		return clone;
	}

	public ElementQuery withRequired(boolean required) {
		ElementQuery clone = clone();
		clone.required = required;
		return clone;
	}

	public ElementQuery withTimeout(double timeout) {
		ElementQuery clone = clone();
		clone.timeout = timeout;
		return clone;
	}

	public ElementQuery withXpath(String xpath) {
		ElementQuery clone = clone();
		clone.clearLocatorFields();
		clone.xpath = xpath;
		return clone;
	}

	public boolean isSelected(String optionText) {
		return getSelectOption(optionText).isSelected();
	}

	public void clear() {
		getElement().clear();
	}

	/*
	 * returns a new selector with the same context as this query
	 */
	public ElementQuery appendXpath(String append) {
		return new ElementQuery(context).withXpath(xpath + append);
	}

	public WebElement waitlessGetElement() {
		return isPresent() ? getElement() : null;
	}

	public ElementQuery withId(String id) {
		ElementQuery clone = clone();
		clone.clearLocatorFields();
		clone.id = id;
		return clone;
	}

	public void clearAndEnterText(String searchString) {
		clear();
		click();
		sendKeys(searchString);
	}

	public ElementQuery withCss(String css) {
		ElementQuery clone = clone();
		clone.clearLocatorFields();
		clone.css = css;
		return clone;
	}

	void clearLocatorFields() {
		this.css = null;
		this.id = null;
		this.xpath = null;
	}
}
