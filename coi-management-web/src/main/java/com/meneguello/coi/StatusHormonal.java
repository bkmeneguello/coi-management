package com.meneguello.coi;

public enum StatusHormonal {
	
	PRE_MENOPAUSAL("Pré-Menopausal"),
	POS_MENOPAUSAL("Pós-Menopausal"),
	TRANSICAO_MENOPAUSAL("Transição Menopausal");

	private String value;

	private StatusHormonal(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static StatusHormonal fromValue(String value) {
		for(StatusHormonal enumValue : StatusHormonal.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
