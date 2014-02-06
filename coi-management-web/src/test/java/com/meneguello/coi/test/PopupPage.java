package com.meneguello.coi.test;

import org.openqa.selenium.WebDriver;

public abstract class PopupPage<T extends DefaultPage> extends DefaultPage {

	public PopupPage(WebDriver driver) {
		super(driver);
	}

	protected abstract Class<T> getPreviousPage();

}
