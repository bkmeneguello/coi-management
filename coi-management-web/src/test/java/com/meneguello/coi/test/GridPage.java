package com.meneguello.coi.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public abstract class GridPage extends FormPage {
	
	@FindBy(xpath = "/html/body/form/table/tbody")
	private WebElement gridData;

	public GridPage(WebDriver driver) {
		super(driver);
	}
	
	public boolean isEmpty() {
		return gridData.findElements(By.xpath(".//tr")).isEmpty();
	}

	public int size() {
		return gridData.findElements(By.xpath(".//tr")).size();
	}

	protected void clickBotaoAcao(int row, int acao) {
		WebElement botao = driver.findElement(By.xpath("/html/body/form/table/tbody/tr["+ row +"]/td["+ getColunaAcao() +"]/button["+ acao +"]"));
		waitUntil(ExpectedConditions.elementToBeClickable(botao));
		botao.click();
	}
	
	protected abstract int getColunaAcao();
	
}
