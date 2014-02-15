package com.meneguello.coi.test;

import static org.openqa.selenium.Keys.DOWN;
import static org.openqa.selenium.Keys.TAB;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Function;

public abstract class DefaultPage {
	
	protected final WebDriver driver;
	
	@FindBy(className = "noty_text")
	private WebElement notificacao;
	
	@FindBy(className = "coi-form")
	private WebElement form;
	
	public DefaultPage(WebDriver driver) {
		this.driver = driver;
	}
	
	protected <T extends DefaultPage> T page(Class<T> page) {
		return PageFactory.initElements(driver, page);
	}
	
	protected <V> V waitUntil(Function<? super WebDriver, V> isTrue, long timeout) {
		WebDriverWait wait = new WebDriverWait(driver, timeout);
		return wait.until(isTrue);
	}
	
	protected <V> V waitUntil(Function<? super WebDriver, V> isTrue) {
		return waitUntil(isTrue, 5);
	}
	
	protected WebElement waitUntil(ExpectedCondition<WebElement> condition, long timeout) {
		WebDriverWait wait = new WebDriverWait(driver, timeout);
		return wait.ignoring(NoSuchElementException.class).until(condition);
	}
	
	protected WebElement waitUntil(ExpectedCondition<WebElement> condition) {
		return waitUntil(condition, 5);
	}

	protected <T extends DefaultPage> T click(WebElement clickable, Class<T> returnPage) {
		waitUntil(elementToBeClickable(clickable));
		clickable.click();
		return page(returnPage);
	}
	
	protected void click(WebElement clickable) {
		waitUntil(elementToBeClickable(clickable));
		clickable.click();
	}

	public String getNotification() {
		waitUntil(visibilityOf(notificacao));
		return notificacao.getText();
	}

	protected void selectValue(WebElement element, String value) {
		new Select(element).selectByVisibleText(value);
	}
	
	protected void selectIndex(WebElement element, int index) {
		new Select(element).selectByIndex(index - 1);
	}
	
	protected void selectAutocomplete(WebElement autocomplete, int index) {
		waitUntil(visibilityOfAnyElementLocated(By.className("ui-autocomplete")));
		for (int i = 0; i < index; i++) {
			autocomplete.sendKeys(DOWN);
		}
		autocomplete.sendKeys(TAB);
		waitUntil(not(visibilityOfAnyElementLocated(By.className("ui-autocomplete"))));
	}

	private ExpectedCondition<WebElement> visibilityOfAnyElementLocated(final By by) {
		return new ExpectedCondition<WebElement>() {
			@Override
			public WebElement apply(WebDriver input) {
				for (WebElement element : input.findElements(by)) {
					if (element.isDisplayed()) {
						return element;
					}
				}
				return null;
			}
		};
	}

}
