package com.meneguello.coi.test;

import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementLocated;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public abstract class FormPage extends DefaultPage {

	public FormPage(WebDriver driver) {
		super(driver);
		waitUntil(textToBePresentInElementLocated(By.xpath("/html/body/form/header"), getTitle()));
	}

	protected abstract String getTitle();
	
}
