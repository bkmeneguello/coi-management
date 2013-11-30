package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.Pagamento.PAGAMENTO;
import static com.meneguello.coi.model.tables.PagamentoCategoria.PAGAMENTO_CATEGORIA;

import java.math.BigDecimal;
import java.sql.Date;
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
import javax.ws.rs.core.MediaType;

import lombok.Data;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.tables.records.PagamentoCategoriaRecord;
import com.meneguello.coi.model.tables.records.PagamentoRecord;
 
@Path("pagamentos")
public class PagamentoEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<PagamentoList> list() throws Exception {
		return new Transaction<List<PagamentoList>>() {
			@Override
			protected List<PagamentoList> execute(Executor database) {
				final ArrayList<PagamentoList> result = new ArrayList<PagamentoList>();
				final Result<PagamentoRecord> resultRecord = database
						.selectFrom(PAGAMENTO)
						.fetch();
				for (Record record : resultRecord) {
					final PagamentoList element = new PagamentoList();
					element.setId(record.getValue(PAGAMENTO.ID));
					element.setVencimento(record.getValue(PAGAMENTO.VENCIMENTO));
					element.setDescricao(record.getValue(PAGAMENTO.DESCRICAO));
					element.setValor(record.getValue(PAGAMENTO.VALOR));
					result.add(element);
				}
				return result;
			}
		}.execute();
	}
	
	@GET
	@Path("categorias")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Categoria> categorias() throws Exception {
		return new Transaction<List<Categoria>>() {
			@Override
			protected List<Categoria> execute(Executor database) {
				final ArrayList<Categoria> result = new ArrayList<Categoria>();
				final Result<PagamentoCategoriaRecord> resultRecord = database
						.selectFrom(PAGAMENTO_CATEGORIA)
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
	public Pagamento read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Pagamento>() {
			@Override
			protected Pagamento execute(Executor database) {
				final Record record = database
						.selectFrom(PAGAMENTO.join(PAGAMENTO_CATEGORIA).onKey())
						.where(PAGAMENTO.ID.eq(id))
						.fetchOne();
				
				final Pagamento entidade = new Pagamento();
				entidade.setId(record.getValue(PAGAMENTO.ID));
				entidade.setCategoria(record.getValue(PAGAMENTO_CATEGORIA.DESCRICAO));
				entidade.setVencimento(record.getValue(PAGAMENTO.VENCIMENTO));
				entidade.setDescricao(record.getValue(PAGAMENTO.DESCRICAO));
				entidade.setValor(record.getValue(PAGAMENTO.VALOR));
				entidade.setSituacao(SituacaoPagamento.valueOf(record.getValue(PAGAMENTO.SITUACAO)).getValue());
				entidade.setPagamento(record.getValue(PAGAMENTO.PAGAMENTO_));
				final String formaPagamento = record.getValue(PAGAMENTO.FORMA_PAGAMENTO);
				entidade.setFormaPagamento(formaPagamento != null ? FormaPagamento.valueOf(formaPagamento).getValue() : null);
				entidade.setBanco(record.getValue(PAGAMENTO.BANCO));
				entidade.setAgencia(record.getValue(PAGAMENTO.AGENCIA));
				entidade.setConta(record.getValue(PAGAMENTO.CONTA));
				entidade.setCheque(record.getValue(PAGAMENTO.CHEQUE));
				return entidade;
			}
		}.execute();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pagamento create(final Pagamento entrada) throws Exception {
		return new Transaction<Pagamento>(true) {
			@Override
			public Pagamento execute(Executor database) {
				final PagamentoCategoriaRecord categoria = database
						.selectFrom(PAGAMENTO_CATEGORIA)
						.where(PAGAMENTO_CATEGORIA.DESCRICAO.eq(entrada.getCategoria()))
						.fetchOne();
				
				final SituacaoPagamento situacaoPagamento = SituacaoPagamento.fromValue(entrada.getSituacao());
				final FormaPagamento formaPagamento = FormaPagamento.fromValue(entrada.getFormaPagamento());
				
				final PagamentoRecord record = database.insertInto(
							PAGAMENTO,
							PAGAMENTO.CATEGORIA_ID,
							PAGAMENTO.VENCIMENTO,
							PAGAMENTO.DESCRICAO,
							PAGAMENTO.VALOR,
							PAGAMENTO.SITUACAO,
							PAGAMENTO.PAGAMENTO_,
							PAGAMENTO.FORMA_PAGAMENTO,
							PAGAMENTO.BANCO,
							PAGAMENTO.AGENCIA,
							PAGAMENTO.CONTA,
							PAGAMENTO.CHEQUE
						)
						.values(
								categoria.getValue(PAGAMENTO_CATEGORIA.ID),
								new Date(entrada.getVencimento().getTime()),
								entrada.getDescricao(),
								entrada.getValor(),
								situacaoPagamento.name(),
								entrada.getPagamento() != null ? new Date(entrada.getPagamento().getTime()) : null,
								formaPagamento != null ? formaPagamento.name() : null,
								entrada.getBanco(),
								entrada.getAgencia(),
								entrada.getConta(),
								entrada.getCheque()
						)
						.returning(ENTRADA.ID)
						.fetchOne();
				
				entrada.setId(record.getId());
				
				return entrada;
			}
		}.execute();
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pagamento update(final @PathParam("id") Long id, final Pagamento entrada) throws Exception {
		return new Transaction<Pagamento>(true) {
			@Override
			public Pagamento execute(Executor database) {
				final PagamentoCategoriaRecord categoria = database
						.selectFrom(PAGAMENTO_CATEGORIA)
						.where(PAGAMENTO_CATEGORIA.DESCRICAO.eq(entrada.getCategoria()))
						.fetchOne();
				
				final SituacaoPagamento situacaoPagamento = SituacaoPagamento.fromValue(entrada.getSituacao());
				final FormaPagamento formaPagamento = FormaPagamento.fromValue(entrada.getFormaPagamento());
				
				database.update(PAGAMENTO)
						.set(PAGAMENTO.CATEGORIA_ID, categoria.getValue(PAGAMENTO_CATEGORIA.ID))
						.set(PAGAMENTO.VENCIMENTO, new Date(entrada.getVencimento().getTime()))
						.set(PAGAMENTO.DESCRICAO, entrada.getDescricao())
						.set(PAGAMENTO.VALOR, entrada.getValor())
						.set(PAGAMENTO.SITUACAO, situacaoPagamento.name())
						.set(PAGAMENTO.PAGAMENTO_, entrada.getPagamento())
						.set(PAGAMENTO.FORMA_PAGAMENTO, formaPagamento != null ? formaPagamento.name() : null)
						.set(PAGAMENTO.BANCO, entrada.getBanco())
						.set(PAGAMENTO.AGENCIA, entrada.getAgencia())
						.set(PAGAMENTO.CONTA, entrada.getConta())
						.set(PAGAMENTO.CHEQUE, entrada.getCheque())
						.where(PAGAMENTO.ID.eq(id))
						.execute();
				
				return entrada;
			}
		}.execute();
	}

	@DELETE
	@Path("{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				database.delete(PAGAMENTO)
						.where(PAGAMENTO.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}

	@Data
	private static class PagamentoList {
		private Long id;
		private Date vencimento;
		private String descricao;
		private BigDecimal valor;
	}
	
	@Data
	private static class Categoria {
		private Long id;
		private String descricao;
	}
	
	@Data
	private static class Pagamento {
		private Long id;
		private String categoria;
		private Date vencimento;
		private String descricao;
		private BigDecimal valor;
		private String situacao;
		private Date pagamento;
		private String formaPagamento;
		private String banco;
		private String agencia;
		private String conta;
		private String cheque;
	}
	
}
