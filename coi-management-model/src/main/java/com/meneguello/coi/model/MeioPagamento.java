package com.meneguello.coi.model;

public enum MeioPagamento {
	
	DINHEIRO("Dinheiro"),
	CARTAO_CREDITO("Cartão de Crédito (venc)"),
	CARTAO_CREDITO_2X("Cartão de Crédito (2x)"),
	CARTAO_CREDITO_3X("Cartão de Crédito (3x)"),
	CARTAO_DEBITO("Cartão de Débito"),
	CHEQUE("Cheque");
	
	private String value;

	private MeioPagamento(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static MeioPagamento fromValue(String value) {
		for(MeioPagamento enumValue : MeioPagamento.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
