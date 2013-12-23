package com.meneguello.coi;

import static com.meneguello.coi.Utils.asSQLDate;
import static com.meneguello.coi.model.tables.Movimento.MOVIMENTO;
import static com.meneguello.coi.model.tables.MovimentoProduto.MOVIMENTO_PRODUTO;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.Data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import com.meneguello.coi.model.tables.records.MovimentoRecord;
 
@Path("/estoque")
public class EstoqueEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<EstoqueList> list() throws Exception {
		return new Transaction<List<EstoqueList>>() {
			@Override
			protected List<EstoqueList> execute(DSLContext database) {
				final ArrayList<EstoqueList> result = new ArrayList<EstoqueList>();
				final Result<MovimentoRecord> resultRecord = database.selectFrom(MOVIMENTO)
						.orderBy(MOVIMENTO.DATA)
						.fetch();
				for (Record record : resultRecord) {
					result.add(buildEstoqueList(database, record));
				}
				return result;
			}
		}.execute();
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Movimento read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Movimento>() {
			@Override
			protected Movimento execute(DSLContext database) {
				final Record record = database.fetchOne(MOVIMENTO, MOVIMENTO.ID.eq(id));
				return buildMovimento(database, record);
			}
		}.execute();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Movimento create(final Movimento movimento) throws Exception {
		return new Transaction<Movimento>(true) {
			@Override
			public Movimento execute(DSLContext database) {
				final TipoMovimento tipoMovimento = TipoMovimento.fromValue(movimento.getTipo());
				
				final MovimentoRecord record = database.insertInto(
							MOVIMENTO, 
							MOVIMENTO.DATA,
							MOVIMENTO.TIPO
						)
						.values(
								asSQLDate(movimento.getData()),
								tipoMovimento.name()
						)
						.returning(MOVIMENTO.ID)
						.fetchOne();
				
				movimento.setId(record.getId());
				
				for (Produto produto : movimento.getProdutos()) {
					database.insertInto(
							MOVIMENTO_PRODUTO, 
							MOVIMENTO_PRODUTO.MOVIMENTO_ID,
							MOVIMENTO_PRODUTO.PRODUTO_ID,
							MOVIMENTO_PRODUTO.QUANTIDADE
						)
						.values(
								movimento.getId(),
								produto.getId(),
								produto.getQuantidade()
						)
						.execute();
				}
				
				return movimento;
			}
		}.execute();
	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Movimento update(final @PathParam("id") Long id, final Movimento movimento) throws Exception {
		return new Transaction<Movimento>(true) {
			@Override
			public Movimento execute(DSLContext database) {
				final TipoMovimento tipoMovimento = TipoMovimento.fromValue(movimento.getTipo());
				
				database.update(MOVIMENTO)
						.set(MOVIMENTO.DATA, asSQLDate(movimento.getData()))
						.set(MOVIMENTO.TIPO, tipoMovimento.name())
						.where(MOVIMENTO.ID.eq(id))
						.execute();
				
				database.delete(MOVIMENTO_PRODUTO)
						.where(MOVIMENTO_PRODUTO.MOVIMENTO_ID.eq(movimento.getId()))
						.execute();
				
				for (Produto produto : movimento.getProdutos()) {
					database.insertInto(
							MOVIMENTO_PRODUTO, 
							MOVIMENTO_PRODUTO.MOVIMENTO_ID,
							MOVIMENTO_PRODUTO.PRODUTO_ID,
							MOVIMENTO_PRODUTO.QUANTIDADE
						)
						.values(
								movimento.getId(),
								produto.getId(),
								produto.getQuantidade()
						)
						.execute();
				}
				
				return movimento;
			}
		}.execute();
	}

	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(DSLContext database) {
				database.delete(MOVIMENTO_PRODUTO)
						.where(MOVIMENTO_PRODUTO.MOVIMENTO_ID.eq(id))
						.execute();
				
				database.delete(MOVIMENTO)
						.where(MOVIMENTO.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}

	private EstoqueList buildEstoqueList(DSLContext database, Record record) {
		final EstoqueList laudo = new EstoqueList();
		laudo.setId(record.getValue(MOVIMENTO.ID));
		laudo.setData(record.getValue(MOVIMENTO.DATA));
		laudo.setTipo(TipoMovimento.valueOf(record.getValue(MOVIMENTO.TIPO)).getValue());
		return laudo;
	}
	
	private Movimento buildMovimento(DSLContext database, Record record) {
		final Movimento movimento = new Movimento();
		movimento.setId(record.getValue(MOVIMENTO.ID));
		movimento.setData(record.getValue(MOVIMENTO.DATA));
		movimento.setTipo(TipoMovimento.valueOf(record.getValue(MOVIMENTO.TIPO)).getValue());
		
		final Result<Record> resultMovimentoProdutoRecord = database.selectFrom(MOVIMENTO_PRODUTO.join(PRODUTO).onKey())
				.where(MOVIMENTO_PRODUTO.MOVIMENTO_ID.eq(movimento.getId()))
				.fetch();
		for (Record movimentoProdutoRecord : resultMovimentoProdutoRecord) {
			movimento.getProdutos().add(buildProduto(movimentoProdutoRecord));
		}
		
		return movimento;
	}
	
	private Produto buildProduto(Record record) {
		final Produto produto = new Produto();
		produto.setId(record.getValue(PRODUTO.ID));
		produto.setCodigo(record.getValue(PRODUTO.CODIGO));
		produto.setDescricao(record.getValue(PRODUTO.DESCRICAO));
		produto.setQuantidade(record.getValue(MOVIMENTO_PRODUTO.QUANTIDADE));
		return produto;
	}

	@Data
	private static class EstoqueList {
		private Long id;
		private Date data;
		private String tipo;
	}
	
	@Data
	private static class Movimento {
		private Long id;
		private Date data;
		private String tipo;
		private List<Produto> produtos = new ArrayList<>();
	}
	
	@Data @JsonIgnoreProperties({"custo", "preco", "estocavel"})
	private static class Produto {
		private Long id;
		private String codigo;
		private String descricao;
		private Integer quantidade;		
	}
	
}
