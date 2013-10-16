package com.meneguello.coi;

public enum Parte {
	
	CONSULTORIO("Consultório", true),
	MEDICO("Médico", true),
	MEDICO_REALIZADOR("Médico Realizador", true),
	FISIOTERAPEUTA("Fisioterapeuta", true),
	PACIENTE("Paciente", false),
	CLIENTE("Cliente", false);
	
	private String value;
	
	private boolean comissionado;

	private Parte(String value, boolean comissionado) {
		this.value = value;
		this.comissionado = comissionado;
	}
	
	public String getValue() {
		return value;
	}
	
	public boolean isComissionado() {
		return comissionado;
	}
	
	public static Parte fromValue(String value) {
		for(Parte enumValue : Parte.values()) {
			if (enumValue.getValue().equals(value)) {
				return enumValue;
			}
		}
		return null;
	}
}
