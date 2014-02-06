package com.meneguello.coi.test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class PessoaPage extends FormPage {

	public PessoaPage(WebDriver driver) {
		super(driver);
	}
	
	@Override
	protected String getTitle() {
		return "Pessoa";
	}

	@FindBy(name = "codigo")
	private WebElement codigo;
	
	@FindBy(name = "nome")
	private WebElement nome;
	
	@FindBy(className = "coi-action-confirm")
	private WebElement confirmar;

	public void typeCodigo(Character prefixo, Integer codigo) {
		this.codigo.sendKeys(prefixo.toString());
		waitUntil(ExpectedConditions.textToBePresentInElementValue(this.codigo, prefixo + "-"));
		this.codigo.sendKeys(codigo.toString());
	}

	public void clearNome() {
		this.nome.clear();
	}

	public void typeNome(String nome) {
		this.nome.sendKeys(nome);
	}
	
	public PessoasPage clickConfirmarSucesso() {
		return click(confirmar, PessoasPage.class);
	}

	public PessoaPage clickConfirmarErro() {
		return click(confirmar, PessoaPage.class);
	}

}
