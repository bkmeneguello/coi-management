package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Parte.PARTE;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.tables.records.ParteRecord;
 
@Path("/partes")
public class ParteEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Parte> list() throws Exception {
		return new Transaction<List<Parte>>() {
			@Override
			protected List<Parte> execute(Executor database) {
				ArrayList<Parte> partes = new ArrayList<Parte>();
				Result<ParteRecord> partesRecord = database.selectFrom(PARTE)
						.fetch();
				for (ParteRecord parteRecord : partesRecord) {
					Parte parte = new Parte();
					parte.setDescricao(parteRecord.getDescricao());
					partes.add(parte);
				}
				return partes;
			}
		}.execute();
	}
	
	@GET
	@Path("/comissionadas")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Parte> listComissionadas(final @QueryParam("term") String term) throws Exception {
		return new Transaction<List<Parte>>() {
			@Override
			protected List<Parte> execute(Executor database) {
				ArrayList<Parte> partes = new ArrayList<Parte>();
				SelectConditionStep<ParteRecord> select = database.selectFrom(PARTE)
				.where(PARTE.COMISSIONADO.eq("S"));
				
				if (StringUtils.isNotBlank(term)) {
					select.and(PARTE.DESCRICAO.likeIgnoreCase(term + "%"));
				}
				
				Result<ParteRecord> partesRecord = select
						.fetch();
				for (ParteRecord parteRecord : partesRecord) {
					Parte parte = new Parte();
					parte.setDescricao(parteRecord.getDescricao());
					partes.add(parte);
				}
				return partes;
			}
		}.execute();
	}
	
	@Data
	private static class Parte {
		private String descricao;
	}

}