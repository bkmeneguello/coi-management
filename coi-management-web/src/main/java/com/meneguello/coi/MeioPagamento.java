package com.meneguello.coi;

import java.math.BigDecimal;

public enum MeioPagamento {
	
	DINHEIRO("Dinheiro", BigDecimal.ZERO),
	CARTAO_CREDITO("Cartão de Crédito (venc)", new BigDecimal("3")),
	CARTAO_CREDITO_2X("Cartão de Crédito (2x)", new BigDecimal("3")),
	CARTAO_CREDITO_3X("Cartão de Crédito (3x)", new BigDecimal("3")),
	CARTAO_DEBITO("Cartão de Débito", new BigDecimal("1.5")),
	CHEQUE("Cheque", BigDecimal.ZERO);
	
	private String value;
	
	private BigDecimal desconto;

	private MeioPagamento(String value, BigDecimal desconto) {
		this.value = value;
		this.desconto = desconto;
	}
	
	public String getValue() {
		return value;
	}
	
	public BigDecimal getDesconto() {
		return desconto;
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
