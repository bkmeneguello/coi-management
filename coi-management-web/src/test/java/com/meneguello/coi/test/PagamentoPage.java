package com.meneguello.coi.test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class PagamentoPage extends FormPage {
	
	@FindBy(name = "categoria")
	private WebElement categoria;
	
	@FindBy(name = "vencimento")
	private WebElement dataVencimento;
	
	@FindBy(name = "descricao")
	private WebElement descricao;
	
	@FindBy(name = "valor")
	private WebElement valor;
	
	private WebElement tipo;
	
	private WebElement situacao;
	
	@FindBy(name = "pagamento")
	private WebElement dataPagamento;
	
	private WebElement formaPagamento;
	
	@FindBy(className = "coi-action-confirm")
	private WebElement confirmar;

	public PagamentoPage(WebDriver driver) {
		super(driver);
	}

	@Override
	protected String getTitle() {
		return "Pagamento";
	}

	public void selectCategoria(String categoria) {
		selectValue(this.categoria, categoria);
	}

	public void typeDataVencimento(String data) {
		this.dataVencimento.sendKeys(data);
	}

	public void typeDescricao(String descricao) {
		this.descricao.sendKeys(descricao);
	}

	public void typeValor(String valor) {
		this.valor.sendKeys(valor);
	}

	public PagamentosPage clickConfirmar() {
		return click(confirmar, PagamentosPage.class);
	}

	public void selectSituacao(String situacao) {
		selectValue(this.situacao, situacao);
	}

	public void typeDataPagamento(String data) {
		this.dataPagamento.sendKeys(data);
	}

	public void selectFormaPagamento(String formaPagamento) {
		selectValue(this.formaPagamento, formaPagamento);
	}

}
