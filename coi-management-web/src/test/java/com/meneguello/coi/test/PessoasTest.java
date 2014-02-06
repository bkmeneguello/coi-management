package com.meneguello.coi.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;

public class PessoasTest extends CoiTestCase {
	
	@Override
	protected IDataSet createDataSet() throws DataSetException {
		return new FlatXmlDataSetBuilder().build(stream("/pessoa-1.dataset.xml"));
	}
	
	@Test
	public void cadastrar() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PessoasPage pessoasPage = mainPage.clickPessoas();
		PessoaPage pessoaPage = pessoasPage.clickAdicionar();
		pessoaPage.typeCodigo('P', 2);
		pessoaPage.typeNome("Fulano de Tal");
		pessoasPage = pessoaPage.clickConfirmarSucesso();
		
		assertThat(pessoasPage.getNotification(), equalTo("Registro incluido com sucesso"));
	}
	
	@Test
	public void cadastrarDuplicado() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PessoasPage pessoasPage = mainPage.clickPessoas();
		PessoaPage pessoaPage = pessoasPage.clickAdicionar();	
		pessoaPage.typeCodigo('P', 1);
		pessoaPage.typeNome("Beltrano da Silva");
		pessoaPage = pessoaPage.clickConfirmarErro();
		
		assertThat(pessoaPage.getNotification(), equalTo("Falha no cadastro do registro"));
	}

	@Test
	public void excluir() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PessoasPage pessoasPage = mainPage.clickPessoas();		
		ExcluirPessoaPopup excluirPopup = pessoasPage.clickExcluir(1);
		pessoasPage = excluirPopup.confirmar();
		assertThat(pessoasPage.getNotification(), equalTo("Registro excluido com sucesso"));
		
		assertTrue(pessoasPage.isEmpty());
	}

	@Test
	public void editar() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PessoasPage pessoasPage = mainPage.clickPessoas();		
		PessoaPage pessoaPage = pessoasPage.clickEditar(1);
		pessoaPage.clearNome();
		pessoaPage.typeNome("Beltrano da Silva");
		pessoasPage = pessoaPage.clickConfirmarSucesso();
		
		assertThat(pessoasPage.getNotification(), equalTo("Registro incluido com sucesso")); //FIXME
	}

	@Test
	public void upload() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		PessoasPage pessoasPage = mainPage.clickPessoas();		
		ImportarPessoasPopup importarPopup = pessoasPage.clickImportar();
		importarPopup.selectUploadFile(path("/pessoas.csv"));
		pessoasPage = importarPopup.clickConfirmarSucesso();
		
		assertEquals(3, pessoasPage.size());
	}
	
}
