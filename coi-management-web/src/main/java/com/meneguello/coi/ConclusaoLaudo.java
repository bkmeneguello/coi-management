package com.meneguello.coi;

public enum ConclusaoLaudo {

	NORMAL("Normal"),
	OSTEOPENIA("Osteopenia"),
	OSTEOPOROSE("Osteoporose"),
	EPERADO_PARA_IDADE("Dentro do esperado para a idade"),
	BAIXA_DENSIDADE_OSSEA("Baixa densidade óssea para a idade");
	
	private String value;

	private ConclusaoLaudo(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static ConclusaoLaudo fromValue(String value) {
		for(ConclusaoLaudo enumValue : ConclusaoLaudo.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
