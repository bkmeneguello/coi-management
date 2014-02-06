package com.meneguello.coi.test;

import org.openqa.selenium.WebDriver;

public class ExcluirPessoaPopup extends ExcluirPopup<PessoasPage> {

	public ExcluirPessoaPopup(WebDriver driver) {
		super(driver);
	}
	
	@Override
	protected Class<PessoasPage> getPreviousPage() {
		return PessoasPage.class;
	}

}
