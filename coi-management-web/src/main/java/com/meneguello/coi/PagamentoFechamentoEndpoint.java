package com.meneguello.coi;

import static com.meneguello.coi.Utils.asTimestamp;
import static com.meneguello.coi.model.tables.Fechamento.FECHAMENTO;
import static com.meneguello.coi.model.tables.FechamentoSaida.FECHAMENTO_SAIDA;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.sql.Timestamp;
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

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.Result;

import com.meneguello.coi.model.tables.records.FechamentoRecord;
import com.meneguello.coi.model.tables.records.FechamentoSaidaRecord;
 
public class PagamentoFechamentoEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<FechamentoList> list() throws Exception {
		return new Transaction<List<FechamentoList>>() {
			@Override
			protected List<FechamentoList> execute(DSLContext database) {
				final ArrayList<FechamentoList> result = new ArrayList<>();
				final Result<Record3<Long, Timestamp, BigDecimal>> resultRecord = database
						.select(
								FECHAMENTO.ID, 
								FECHAMENTO.DATA,
								FECHAMENTO.VALOR_DINHEIRO
									.add(FECHAMENTO.VALOR_CARTAO)
									.add(FECHAMENTO.VALOR_CHEQUE)
									.as("TOTAL")
						)
						.from(FECHAMENTO)
						.orderBy(FECHAMENTO.DATA.desc())
						.fetch();
				for (Record record : resultRecord) {
					final FechamentoList element = new FechamentoList();
					element.setId(record.getValue(FECHAMENTO.ID));
					element.setData(record.getValue(FECHAMENTO.DATA));
					element.setTotal(record.getValue("TOTAL", BigDecimal.class));
					result.add(element);
				}
				return result;
			}
		}.execute();
	}
	
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Fechamento read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Fechamento>() {
			@Override
			protected Fechamento execute(DSLContext database) {
				final Record record = database
						.selectFrom(FECHAMENTO)
						.where(FECHAMENTO.ID.eq(id))
						.fetchOne();
				
				final Fechamento entidade = new Fechamento();
				entidade.setId(record.getValue(FECHAMENTO.ID));
				entidade.setData(record.getValue(FECHAMENTO.DATA));
				entidade.setValorDinheiro(record.getValue(FECHAMENTO.VALOR_DINHEIRO));
				entidade.setValorCartao(record.getValue(FECHAMENTO.VALOR_CARTAO));
				entidade.setValorCheque(record.getValue(FECHAMENTO.VALOR_CHEQUE));
				
				final Result<FechamentoSaidaRecord> saidasResult = database.selectFrom(FECHAMENTO_SAIDA)
						.where(FECHAMENTO_SAIDA.FECHAMENTO_ID.eq(id))
						.fetch();
				for (Record saidaRecord : saidasResult) {
					Saida saida = new Saida();
					saida.setDescricao(saidaRecord.getValue(FECHAMENTO_SAIDA.DESCRICAO));
					saida.setValor(saidaRecord.getValue(FECHAMENTO_SAIDA.VALOR));
					entidade.getSaidas().add(saida);
				}
				
				return entidade;
			}
		}.execute();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Fechamento create(final Fechamento registro) throws Exception {
		return new Transaction<Fechamento>(true) {
			@Override
			public Fechamento execute(DSLContext database) {
				final FechamentoRecord record = database.insertInto(
							FECHAMENTO,
							FECHAMENTO.DATA,
							FECHAMENTO.VALOR_DINHEIRO,
							FECHAMENTO.VALOR_CARTAO,
							FECHAMENTO.VALOR_CHEQUE
						)
						.values(
								asTimestamp(registro.getData()),
								registro.getValorDinheiro(),
								registro.getValorCartao(),
								registro.getValorCheque()
						)
						.returning(FECHAMENTO.ID)
						.fetchOne();
				
				registro.setId(record.getId());
				
				for (Saida saida : registro.getSaidas()) {
					database.insertInto(
							FECHAMENTO_SAIDA,
							FECHAMENTO_SAIDA.FECHAMENTO_ID,
							FECHAMENTO_SAIDA.DESCRICAO,
							FECHAMENTO_SAIDA.VALOR
					)
					.values(
							registro.getId(),
							saida.getDescricao(),
							saida.getValor()
					)
					.execute();
				}
				
				return registro;
			}
		}.execute();
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Fechamento update(final @PathParam("id") Long id, final Fechamento registro) throws Exception {
		return new Transaction<Fechamento>(true) {
			@Override
			public Fechamento execute(DSLContext database) {
				database.update(FECHAMENTO)
						.set(FECHAMENTO.DATA, asTimestamp(registro.getData()))
						.set(FECHAMENTO.VALOR_DINHEIRO, registro.getValorDinheiro())
						.set(FECHAMENTO.VALOR_CARTAO, registro.getValorCartao())
						.set(FECHAMENTO.VALOR_CHEQUE, registro.getValorCheque())
						.where(FECHAMENTO.ID.eq(id))
						.execute();
				
				database.delete(FECHAMENTO_SAIDA)
						.where(FECHAMENTO_SAIDA.FECHAMENTO_ID.eq(id))
						.execute();
				
				for (Saida saida : registro.getSaidas()) {
					database.insertInto(
							FECHAMENTO_SAIDA,
							FECHAMENTO_SAIDA.FECHAMENTO_ID,
							FECHAMENTO_SAIDA.DESCRICAO,
							FECHAMENTO_SAIDA.VALOR
					)
					.values(
							id,
							saida.getDescricao(),
							saida.getValor()
					)
					.execute();
				}
				
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
				database.delete(FECHAMENTO_SAIDA)
						.where(FECHAMENTO_SAIDA.FECHAMENTO_ID.eq(id))
						.execute();
				
				database.delete(FECHAMENTO)
						.where(FECHAMENTO.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}
	
	@Data
	private static class FechamentoList {
		private Long id;
		private Date data;
		private BigDecimal total = ZERO;
	}
	
	@Data
	private static class Fechamento {
		private Long id;
		private Date data;
		private BigDecimal valorDinheiro = ZERO;
		private BigDecimal valorCartao = ZERO;
		private BigDecimal valorCheque = ZERO;
		private List<Saida> saidas = new ArrayList<>();
	}
	
	@Data
	private static class Saida {
		private String descricao;
		private BigDecimal valor;
	}
	
}
