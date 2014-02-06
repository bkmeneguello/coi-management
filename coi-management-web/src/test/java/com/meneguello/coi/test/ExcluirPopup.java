package com.meneguello.coi.test;

import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementLocated;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public abstract class ExcluirPopup<T extends DefaultPage> extends PopupPage<T> {
	
	@FindBy(xpath = "//*[@class='noty_bar']/div[2]/button[1]")
	private WebElement cancelar;
	
	@FindBy(xpath = "//*[@class='noty_bar']/div[2]/button[2]")
	private WebElement confirmar;

	public ExcluirPopup(WebDriver driver) {
		super(driver);
		waitUntil(textToBePresentInElementLocated(By.className("noty_text"), "Deseja realmente excluir o registro?"));
	}

	public T confirmar() {
		final T page = click(confirmar, getPreviousPage());
		waitUntil(not(textToBePresentInElementLocated(By.className("noty_text"), "Deseja realmente excluir o registro?")));
		return page;
	}

	
}
