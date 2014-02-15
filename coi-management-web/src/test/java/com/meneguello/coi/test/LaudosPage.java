package com.meneguello.coi.test;

import org.openqa.selenium.WebDriver;

public class LaudosPage extends GridPage<LaudoPage> {

	public LaudosPage(WebDriver driver) {
		super(driver);
	}
	
	@Override
	protected String getTitle() {
		return "Laudos";
	}

	@Override
	protected int getColunaAcao() {
		return 4;
	}

	@Override
	protected Class<LaudoPage> getFormClass() {
		return LaudoPage.class;
	}

}
