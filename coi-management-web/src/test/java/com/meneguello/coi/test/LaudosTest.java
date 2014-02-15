package com.meneguello.coi.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.openqa.selenium.Keys.CONTROL;
import static org.openqa.selenium.Keys.chord;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;

public class LaudosTest extends CoiTestCase {

	@Override
	protected IDataSet createDataSet() throws Exception {
		return new FlatXmlDataSetBuilder().build(stream("/pessoas-laudo-1.dataset.xml"));
	}
	
	@Test
	public void cadastroPosMenopausal() throws Exception {
		MainPage mainPage = MainPage.page(driver);
		LaudosPage laudosPage = mainPage.clickLaudos();
		LaudoPage laudoPage = laudosPage.clickAdicionar();
		laudoPage.typeData(chord(CONTROL, "a"));
		laudoPage.typeData("010214");
		laudoPage.typeMedico("Dr. Fulano de Tal");
		laudoPage.selectMedico(1);
		laudoPage.typePaciente("Joaquina de Castro");
		laudoPage.selectPaciente(1);
		laudoPage.selectStatusHormonal("Pós-Menopausal");
		laudoPage.selectSexo("Feminino");
		laudoPage.typeDataNascimento("150953");
		laudoPage.clickColunaLombarL1();
		laudoPage.clickColunaLombarL3();
		laudoPage.clickColunaLombarL4();
		laudoPage.typeColunaLombarDensidade(",868");
		laudoPage.typeColunaLombarTScore("-2,5");
		laudoPage.typeColunaLombarZScore("-,5");
		laudoPage.typeColoFemurDensidade(",633");
		laudoPage.typeColoFemurTScore("-3,4");
		laudoPage.typeColoFemurZScore("-1,1");
		laudoPage.typeFemurTotalDensidade(",674");
		laudoPage.typeFemurTotalTScore("-2,7");
		laudoPage.typeFemurTotalZScore("-1,1");
		laudoPage.typeRadioTercoDensidade(",608");
		laudoPage.typeRadioTercoTScore("-1,5");
		laudoPage.typeRadioTercoZScore("-,5");
		laudoPage.selectConclusao(4);
		laudoPage.selectObservacao(1);
		laudoPage.clickAdicionarObservacao();
		laudoPage.selectObservacao(2);
		laudoPage.clickAdicionarObservacao();
		laudoPage.typeCampoObservacao(2, 1, "L2-L3");
		laudoPage.selectComparacao(11);
		laudoPage.clickAdicionarComparacao();
		laudoPage.typeCampoComparacao(1, 1, "L2-L3");
		laudoPage.selectComparacao(1);
		laudoPage.clickAdicionarComparacao();
		laudoPage.selectComparacao(12);
		laudoPage.clickAdicionarComparacao();
		laudoPage.typeCampoComparacao(3, 1, "12");
		laudosPage = laudoPage.clickConfirmarSucesso();
		
		assertThat(laudosPage.getNotification(), equalTo("Registro incluido com sucesso"));
	}
	
}
