package com.meneguello.coi;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.Data;

import org.jooq.impl.Executor;
 
@Path("/partes")
public class ParteEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<PessoaParte> list() throws Exception {
		return new Transaction<List<PessoaParte>>() {
			@Override
			protected List<PessoaParte> execute(Executor database) {
				ArrayList<PessoaParte> partes = new ArrayList<PessoaParte>();
				for (Parte parte : Parte.values()) {
					final PessoaParte pessoaParte = new PessoaParte();
					pessoaParte.setDescricao(parte.getValue());
					partes.add(pessoaParte);
				}
				return partes;
			}
		}.execute();
	}
	
	@GET
	@Path("/comissionadas")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PessoaParte> listComissionadas(final @QueryParam("term") String term) throws Exception {
		return new Transaction<List<PessoaParte>>() {
			@Override
			protected List<PessoaParte> execute(Executor database) {
				ArrayList<PessoaParte> partes = new ArrayList<PessoaParte>();
				for (Parte parte : Parte.values()) {
					if (isNotBlank(term) && !parte.getValue().toLowerCase().startsWith(term.toLowerCase())) {
						continue;
					}
					final PessoaParte pessoaParte = new PessoaParte();
					pessoaParte.setDescricao(parte.getValue());
					partes.add(pessoaParte);
				}
				return partes;
			}
		}.execute();
	}
	
	@Data
	private static class PessoaParte {
		private String descricao;
	}

}