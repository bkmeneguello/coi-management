package com.meneguello.coi.test;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementLocated;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class ImportarPessoasPopup extends PopupPage<PessoasPage> {

	public ImportarPessoasPopup(WebDriver driver) {
		super(driver);
		waitUntil(textToBePresentInElementLocated(By.xpath("/html/body/div[2]/div[1]/span"), "Importar Clientes"));
	}
	
	@FindBy(id = "file")
	private WebElement file;
	
	@FindBy(className = "coi-action-cancel")
	private WebElement cancelar;
	
	@FindBy(className = "coi-action-confirm")
	private WebElement confirmar;

	@Override
	protected Class<PessoasPage> getPreviousPage() {
		return PessoasPage.class;
	}

	public void selectUploadFile(String path) {
		waitUntil(elementToBeClickable(file));
		file.sendKeys(path);
	}

	public PessoasPage clickConfirmarSucesso() {
		click(confirmar);
		waitUntil(not(error(presenceOfElementLocated(By.id("coi-pessoas-import")))));
		return page(getPreviousPage());
	}
	
	private ExpectedCondition<?> error(final ExpectedCondition<WebElement> condition) {
		return new ExpectedCondition<WebElement>() {
			@Override
			public WebElement apply(WebDriver input) {
				try {
					return condition.apply(input);
				} catch(NoSuchElementException e) {
					return null;
				}
			}
		};
	}

}
