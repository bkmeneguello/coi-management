package com.meneguello.coi.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.openqa.selenium.Keys.CONTROL;
import static org.openqa.selenium.Keys.TAB;
import static org.openqa.selenium.Keys.chord;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;

public class PagamentosTest extends CoiTestCase {
	
	@Override
	protected IDataSet createDataSet() throws DataSetException {
		return new FlatXmlDataSetBuilder().build(stream("/pagamento-1.dataset.xml"));
	}

	@Test
	public void cadastrar() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PagamentosPage pagamentosPage = mainPage.clickPagamentos();
		PagamentoPage pagamentoPage = pagamentosPage.clickAdicionar();
		pagamentoPage.selectCategoria("Aluguel");
		pagamentoPage.typeDataVencimento("010114");
		pagamentoPage.typeDescricao("Aluguel de Janeiro");
		pagamentoPage.typeValor("2000");
		pagamentosPage = pagamentoPage.clickConfirmar();
		
		assertThat(pagamentosPage.getNotification(), equalTo("Registro incluido com sucesso"));
		
		pagamentosPage.typeDataInicial(chord(CONTROL, "a"));
		pagamentosPage.typeDataInicial("010114");
		pagamentosPage.typeDataInicial(TAB);
		
		pagamentosPage.typeDataFinal(chord(CONTROL, "a"));
		pagamentosPage.typeDataFinal("310114");
		pagamentosPage.typeDataFinal(TAB);
		
		assertThat(pagamentosPage.getVencimento(1), equalTo("01/01/2014"));
		assertThat(pagamentosPage.getCategoria(1), equalTo("Aluguel"));
		assertThat(pagamentosPage.getDescricao(1), equalTo("Aluguel de Janeiro"));
		assertThat(pagamentosPage.getValor(1), equalTo("2000,00"));
	}

	@Test
	public void cadastrarApósHorárioDeVerão() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PagamentosPage pagamentosPage = mainPage.clickPagamentos();
		PagamentoPage pagamentoPage = pagamentosPage.clickAdicionar();
		pagamentoPage.selectCategoria("Aluguel");
		pagamentoPage.typeDataVencimento("010314");
		pagamentoPage.typeDescricao("Aluguel de Março");
		pagamentoPage.typeValor("2000");
		pagamentosPage = pagamentoPage.clickConfirmar();
		
		assertThat(pagamentosPage.getNotification(), equalTo("Registro incluido com sucesso"));
		
		pagamentosPage.typeDataInicial(chord(CONTROL, "a"));
		pagamentosPage.typeDataInicial("010314");
		pagamentosPage.typeDataInicial(TAB);
		
		pagamentosPage.typeDataFinal(chord(CONTROL, "a"));
		pagamentosPage.typeDataFinal("310314");
		pagamentosPage.typeDataFinal(TAB);
		
		assertThat(pagamentosPage.getVencimento(1), equalTo("01/03/2014"));
		assertThat(pagamentosPage.getCategoria(1), equalTo("Aluguel"));
		assertThat(pagamentosPage.getDescricao(1), equalTo("Aluguel de Março"));
		assertThat(pagamentosPage.getValor(1), equalTo("2000,00"));
	}

	@Test
	public void baixarPagamento() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PagamentosPage pagamentosPage = mainPage.clickPagamentos();
		
		pagamentosPage.typeDataInicial(chord(CONTROL, "a"));
		pagamentosPage.typeDataInicial("010214");
		pagamentosPage.typeDataInicial(TAB);
		
		pagamentosPage.typeDataFinal(chord(CONTROL, "a"));
		pagamentosPage.typeDataFinal("280214");
		pagamentosPage.typeDataFinal(TAB);
		
		PagamentoPage pagamentoPage = pagamentosPage.clickEditar(1);
		pagamentoPage.selectSituacao("Pago");
		pagamentoPage.typeDataPagamento("020214");
		pagamentoPage.selectFormaPagamento("Dinheiro");
		pagamentosPage = pagamentoPage.clickConfirmar();
		
		assertThat(pagamentosPage.getNotification(), equalTo("Registro incluido com sucesso"));
		
		pagamentosPage.selectSituacao("Pago");
		
		pagamentosPage.typeDataInicial(chord(CONTROL, "a"));
		pagamentosPage.typeDataInicial("010214");
		pagamentosPage.typeDataInicial(TAB);
		
		pagamentosPage.typeDataFinal(chord(CONTROL, "a"));
		pagamentosPage.typeDataFinal("280214");
		pagamentosPage.typeDataFinal(TAB);
		
		assertThat(pagamentosPage.getVencimento(1), equalTo("01/02/2014"));
		assertThat(pagamentosPage.getCategoria(1), equalTo("Aluguel"));
		assertThat(pagamentosPage.getDescricao(1), equalTo("Aluguel de Fevereiro"));
		assertThat(pagamentosPage.getValor(1), equalTo("2000,00"));
	}
	
}
