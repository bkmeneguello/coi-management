package com.meneguello.coi.test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class PessoasPage extends GridPage<PessoaPage> {
	
	private static final int ACAO_EDITAR = 1;
	
	private static final int ACAO_EXCLUIR = 2;
	
	@FindBy(id = "coi-action-pessoas-importar")
	private WebElement importar;

	public PessoasPage(WebDriver driver) {
		super(driver);
	}
	
	@Override
	protected String getTitle() {
		return "Pessoas";
	}

	@Override
	protected int getColunaAcao() {
		return 3;
	}

	public PessoaPage clickEditar(int row) {
		clickBotaoAcao(row, ACAO_EDITAR);
		return page(PessoaPage.class);
	}

	public ExcluirPessoaPopup clickExcluir(int row) {
		clickBotaoAcao(row, ACAO_EXCLUIR);
		return page(ExcluirPessoaPopup.class);
	}

	public ImportarPessoasPopup clickImportar() {
		return click(importar, ImportarPessoasPopup.class);
	}

	@Override
	protected Class<PessoaPage> getFormClass() {
		return PessoaPage.class;
	}
	
}
