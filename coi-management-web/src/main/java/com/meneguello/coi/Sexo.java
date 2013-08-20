package com.meneguello.coi;

public enum Sexo {
	
	FEMININO("Feminino"),
	MASCULINO("Masculino");
	
	private String value;

	private Sexo(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static Sexo fromValue(String value) {
		for(Sexo enumValue : Sexo.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
