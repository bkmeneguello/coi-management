package com.meneguello.coi;

import static com.meneguello.coi.model.tables.PagamentoCategoria.PAGAMENTO_CATEGORIA;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.Data;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import com.meneguello.coi.model.tables.records.PagamentoCategoriaRecord;
 
public class PagamentoCategoriaEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Categoria> list(final @QueryParam("page") Integer page) throws Exception {
		return new Transaction<List<Categoria>>() {
			@Override
			protected List<Categoria> execute(DSLContext database) {
				final ArrayList<Categoria> result = new ArrayList<Categoria>();
				final Result<PagamentoCategoriaRecord> resultRecord = database
						.selectFrom(PAGAMENTO_CATEGORIA)
						.orderBy(PAGAMENTO_CATEGORIA.DESCRICAO)
						.limit(10).offset(10 * (page != null ? page : 0))
						.fetch();
				for (Record record : resultRecord) {
					final Categoria element = new Categoria();
					element.setId(record.getValue(PAGAMENTO_CATEGORIA.ID));
					element.setDescricao(record.getValue(PAGAMENTO_CATEGORIA.DESCRICAO));
					result.add(element);
				}
				return result;
			}
		}.execute();
	}
	
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Categoria>() {
			@Override
			protected Categoria execute(DSLContext database) {
				final Record record = database
						.selectFrom(PAGAMENTO_CATEGORIA)
						.where(PAGAMENTO_CATEGORIA.ID.eq(id))
						.fetchOne();
				
				final Categoria entidade = new Categoria();
				entidade.setId(record.getValue(PAGAMENTO_CATEGORIA.ID));
				entidade.setDescricao(record.getValue(PAGAMENTO_CATEGORIA.DESCRICAO));
				return entidade;
			}
		}.execute();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria create(final Categoria registro) throws Exception {
		return new Transaction<Categoria>(true) {
			@Override
			public Categoria execute(DSLContext database) {
				final PagamentoCategoriaRecord record = database.insertInto(
							PAGAMENTO_CATEGORIA,
							PAGAMENTO_CATEGORIA.DESCRICAO
						)
						.values(registro.getDescricao())
						.returning(PAGAMENTO_CATEGORIA.ID)
						.fetchOne();
				
				registro.setId(record.getId());
				
				return registro;
			}
		}.execute();
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria update(final @PathParam("id") Long id, final Categoria registro) throws Exception {
		return new Transaction<Categoria>(true) {
			@Override
			public Categoria execute(DSLContext database) {
				database.update(PAGAMENTO_CATEGORIA)
						.set(PAGAMENTO_CATEGORIA.DESCRICAO, registro.getDescricao())
						.where(PAGAMENTO_CATEGORIA.ID.eq(id))
						.execute();
				
				return registro;
			}
		}.execute();
	}

	@DELETE
	@Path("{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(DSLContext database) {
				database.delete(PAGAMENTO_CATEGORIA)
						.where(PAGAMENTO_CATEGORIA.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}
	
	@Data
	private static class Categoria {
		private Long id;
		private String descricao;
	}
	
}
