package com.meneguello.coi.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LaudoPage extends FormPage {
	
	@FindBy(name = "data")
	private WebElement data;
	
	@FindBy(css = "#medico [name=nome]")
	private WebElement medico;
	
	@FindBy(css = "#paciente [name=nome]")
	private WebElement paciente;
	
	@FindBy(name = "status")
	private WebElement statusHormonal;
	
	private WebElement exame;
	
	@FindBy(name = "sexo")
	private WebElement sexo;
	
	@FindBy(name = "dataNascimento")
	private WebElement dataNascimento;
	
	@FindBy(name = "colunaLombarL1")
	private WebElement colunaLombarL1;
	
	@FindBy(name = "colunaLombarL2")
	private WebElement colunaLombarL2;
	
	@FindBy(name = "colunaLombarL3")
	private WebElement colunaLombarL3;
	
	@FindBy(name = "colunaLombarL4")
	private WebElement colunaLombarL4;
	
	@FindBy(name = "colunaLombarDensidade")
	private WebElement colunaLombarDensidade;
	
	@FindBy(name = "colunaLombarTScore")
	private WebElement colunaLombarTScore;
	
	@FindBy(name = "colunaLombarZScore")
	private WebElement colunaLombarZScore;
	
	@FindBy(name = "coloFemurDensidade")
	private WebElement coloFemurDensidade;
	
	@FindBy(name = "coloFemurTScore")
	private WebElement coloFemurTScore;
	
	@FindBy(name = "coloFemurZScore")
	private WebElement coloFemurZScore;
	
	@FindBy(name = "femurTotalDensidade")
	private WebElement femurTotalDensidade;
	
	@FindBy(name = "femurTotalTScore")
	private WebElement femurTotalTScore;
	
	@FindBy(name = "femurTotalZScore")
	private WebElement femurTotalZScore;
	
	@FindBy(name = "radioTercoDensidade")
	private WebElement radioTercoDensidade;
	
	@FindBy(name = "radioTercoTScore")
	private WebElement radioTercoTScore;
	
	@FindBy(name = "radioTercoZScore")
	private WebElement radioTercoZScore;
	
	@FindBy(name = "conclusao")
	private WebElement conclusao;
	
	private WebElement observacoes;
	
	@FindBy(id = "adicionar-observacao")
	private WebElement adicionarObservacao;
	
	private WebElement comparacoes;
	
	@FindBy(id = "adicionar-comparacao")
	private WebElement adicionarComparacao;
	
	@FindBy(className = "coi-action-confirm")
	private WebElement confirmar;

	public LaudoPage(WebDriver driver) {
		super(driver);
	}

	@Override
	protected String getTitle() {
		return "Laudo";
	}

	public void typeData(String data) {
		this.data.sendKeys(data);
	}

	public void typeMedico(String valor) {
		medico.sendKeys(valor);
	}

	public void selectMedico(int index) {
		selectAutocomplete(medico, index);
	}

	public void typePaciente(String valor) {
		paciente.sendKeys(valor);
	}
	
	public void selectPaciente(int index) {
		selectAutocomplete(paciente, index);
	}

	public void selectStatusHormonal(String value) {
		selectValue(statusHormonal, value);
		waitUntil(ExpectedConditions.textToBePresentInElement(exame, "Exame"));
	}

	public void selectSexo(String value) {
		selectValue(sexo, value);
	}

	public void typeDataNascimento(String data) {
		dataNascimento.sendKeys(data);
	}

	public void clickColunaLombarL1() {
		colunaLombarL1.click();
	}
	
	public void clickColunaLombarL2() {
		colunaLombarL2.click();
	}
	
	public void clickColunaLombarL3() {
		colunaLombarL3.click();
	}
	
	public void clickColunaLombarL4() {
		colunaLombarL4.click();
	}

	public void typeColunaLombarDensidade(String valor) {
		colunaLombarDensidade.sendKeys(valor);
	}
	
	public void typeColunaLombarTScore(String valor) {
		colunaLombarTScore.sendKeys(valor);
	}

	public void typeColunaLombarZScore(String valor) {
		colunaLombarZScore.sendKeys(valor);
	}

	public void typeColoFemurDensidade(String string) {
		coloFemurDensidade.sendKeys(string);
	}

	public void typeColoFemurTScore(String string) {
		coloFemurTScore.sendKeys(string);
	}

	public void typeColoFemurZScore(String string) {
		coloFemurZScore.sendKeys(string);
	}

	public void typeFemurTotalDensidade(String string) {
		femurTotalDensidade.sendKeys(string);
	}

	public void typeFemurTotalTScore(String string) {
		femurTotalTScore.sendKeys(string);
	}

	public void typeFemurTotalZScore(String string) {
		femurTotalZScore.sendKeys(string);
	}

	public void typeRadioTercoDensidade(String string) {
		radioTercoDensidade.sendKeys(string);
	}

	public void typeRadioTercoTScore(String string) {
		radioTercoTScore.sendKeys(string);
	}

	public void typeRadioTercoZScore(String string) {
		radioTercoZScore.sendKeys(string);
	}

	public void selectConclusao(int index) {
		selectIndex(conclusao, index);
	}

	public void selectObservacao(int index) {
		selectIndex(observacoes, index);
	}
	
	public void clickAdicionarObservacao() {
		adicionarObservacao.click();
	}

	public void typeCampoObservacao(int row, int campo, String string) {
		WebElement element = driver.findElement(By.xpath("//*[@id=\"observacoes-list\"]/div/tr["+ row +"]/td[2]/input["+ campo +"]"));
		element.sendKeys(string);
	}

	public void selectComparacao(int index) {
		selectIndex(comparacoes, index);
	}

	public void clickAdicionarComparacao() {
		adicionarComparacao.click();
	}
	
	public void typeCampoComparacao(int row, int campo, String string) {
		WebElement element = driver.findElement(By.xpath("//*[@id=\"comparacoes-list\"]/div/tr["+ row +"]/td[2]/input["+ campo +"]"));
		element.sendKeys(string);
	}
	
	public LaudosPage clickConfirmarSucesso() {
		return click(confirmar, LaudosPage.class);
	}
	
}
