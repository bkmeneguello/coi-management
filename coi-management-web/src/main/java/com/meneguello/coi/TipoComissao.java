package com.meneguello.coi;

public enum TipoComissao {
	
	PERCENTUAL("Percentual"),
	VALOR("Valor");

	private String value;

	private TipoComissao(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static TipoComissao fromValue(String value) {
		for(TipoComissao enumValue : TipoComissao.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
