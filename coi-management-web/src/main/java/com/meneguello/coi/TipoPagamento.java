package com.meneguello.coi;

public enum TipoPagamento {
	
	ENTRADA("Entrada"),
	SAIDA("Saída");

	private String value;

	private TipoPagamento(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static TipoPagamento fromValue(String value) {
		for(TipoPagamento enumValue : TipoPagamento.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
