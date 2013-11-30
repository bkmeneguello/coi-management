package com.meneguello.coi;


public enum SituacaoPagamento {
	
	PENDENTE("Pendente"),
	PAGO("Pago");
	
	private String value;
	
	private SituacaoPagamento(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static SituacaoPagamento fromValue(String value) {
		for(SituacaoPagamento enumValue : SituacaoPagamento.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
