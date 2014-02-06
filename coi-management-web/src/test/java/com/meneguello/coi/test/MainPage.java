package com.meneguello.coi.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class MainPage extends DefaultPage {

	public MainPage(WebDriver driver) {
		super(driver);
		waitUntil(ExpectedConditions.presenceOfElementLocated(By.id("main")));
	}

	private WebElement pessoas;
	
	private WebElement pagamentos;
	
	public static MainPage page(WebDriver driver) {
		return PageFactory.initElements(driver, MainPage.class);
	}
	
	public PessoasPage clickPessoas() {
		return click(pessoas, PessoasPage.class);
	}

	public PagamentosPage clickPagamentos() {
		return click(pagamentos, PagamentosPage.class);
	}
	
}
