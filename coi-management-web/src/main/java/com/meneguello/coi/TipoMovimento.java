package com.meneguello.coi;

public enum TipoMovimento {
	
	ENTRADA("Entrada"),
	BAIXA("Baixa");

	private String value;

	private TipoMovimento(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static TipoMovimento fromValue(String value) {
		for(TipoMovimento enumValue : TipoMovimento.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
