package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Parte.PARTE;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jooq.Result;
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
				Result<ParteRecord> partesRecord = database.fetch(PARTE);
				for (ParteRecord parteRecord : partesRecord) {
					Parte parte = new Parte();
					parte.setDescricao(parteRecord.getDescricao());
					partes.add(parte);
				}
				return partes;
			}
		}.execute();
	}

}

class Parte {
	
	private String descricao;
	
	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
}