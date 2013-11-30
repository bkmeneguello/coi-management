package com.meneguello.coi;


public enum FormaPagamento {
	
	DINHEIRO("Dinheiro"),
	PARCELADO("Parcelado"),
	CHEQUE("Cheque"),
	CREDITO("Crédito"),
	DEBITO("Débito"),
	DEBITO_AUTOMATICO("Débito Automático");
	
	private String value;
	
	private FormaPagamento(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static FormaPagamento fromValue(String value) {
		for(FormaPagamento enumValue : FormaPagamento.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
