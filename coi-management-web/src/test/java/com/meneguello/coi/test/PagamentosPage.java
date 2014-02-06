package com.meneguello.coi.test;

import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class PagamentosPage extends GridPage {

	private static final int COLUNA_VENCIMENTO = 1;
	
	private static final int COLUNA_CATEGORIA = 2;
	
	private static final int COLUNA_DESCRICAO = 3;
	
	private static final int COLUNA_VALOR = 4;

	private static final int ACAO_EDITAR = 1;
	
	@FindBy(className = "coi-action-create")
	private WebElement adicionar;
	
	private WebElement situacao;
	
	@FindBy(id = "data-inicial")
	private WebElement dataInicial;
	
	@FindBy(id = "data-final")
	private WebElement dataFinal;

	public PagamentosPage(WebDriver driver) {
		super(driver);
	}

	@Override
	protected String getTitle() {
		return "Pagamentos";
	}
	
	@Override
	protected int getColunaAcao() {
		return 5;
	}

	public PagamentoPage clickAdicionar() {
		return click(adicionar, PagamentoPage.class);
	}
	
	public String getVencimento(int row) {
		return getValorColuna(row, COLUNA_VENCIMENTO).getText();
	}

	public String getDescricao(int row) {
		return getValorColuna(row, COLUNA_DESCRICAO).getText();
	}

	public String getCategoria(int row) {
		return getValorColuna(row, COLUNA_CATEGORIA).getText();
	}

	public String getValor(int row) {
		return getValorColuna(row, COLUNA_VALOR).getText();
	}

	protected WebElement getValorColuna(int row, int col) {
		return waitUntil(visibilityOfElementLocated(By.xpath("/html/body/form/table/tbody/tr["+ row +"]/td["+ col +"]")));
	}
	
	public void typeDataInicial(CharSequence data) {
		this.dataInicial.sendKeys(data);
	}
	
	public void typeDataFinal(CharSequence data) {
		this.dataFinal.sendKeys(data);
	}

	public PagamentoPage clickEditar(int row) {
		clickBotaoAcao(row, ACAO_EDITAR);
		return page(PagamentoPage.class);
	}

	public void selectSituacao(String situacao) {
		selectValue(this.situacao, situacao);
	}

}
