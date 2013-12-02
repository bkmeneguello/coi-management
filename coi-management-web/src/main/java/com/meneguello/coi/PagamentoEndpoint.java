package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Pagamento.PAGAMENTO;
import static com.meneguello.coi.model.tables.PagamentoCategoria.PAGAMENTO_CATEGORIA;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.Response;

import lombok.Data;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRXlsExporter;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.tables.records.PagamentoCategoriaRecord;
import com.meneguello.coi.model.tables.records.PagamentoRecord;
 
@Path("pagamentos")
public class PagamentoEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<PagamentoList> list(final @QueryParam("situacao") String situacao, 
			final @QueryParam("start") Date start, 
			final @QueryParam("end") Date end, 
			final @QueryParam("page") Integer page) throws Exception {
		
		return new Transaction<List<PagamentoList>>() {
			@Override
			protected List<PagamentoList> execute(Executor database) {
				final ArrayList<PagamentoList> result = new ArrayList<PagamentoList>();
				final Result<Record> resultRecord = database
						.selectFrom(PAGAMENTO.join(PAGAMENTO_CATEGORIA).onKey())
						.where(PAGAMENTO.SITUACAO.eq(SituacaoPagamento.fromValue(situacao).name()))
						.and(PAGAMENTO.VENCIMENTO.between(start, end))
						.orderBy(PAGAMENTO_CATEGORIA.DESCRICAO.asc(), PAGAMENTO.VENCIMENTO.desc(), PAGAMENTO.DESCRICAO.asc())
						.limit(10).offset(10 * page)
						.fetch();
				for (Record record : resultRecord) {
					final PagamentoList element = new PagamentoList();
					element.setId(record.getValue(PAGAMENTO.ID));
					element.setVencimento(record.getValue(PAGAMENTO.VENCIMENTO));
					element.setCategoria(record.getValue(PAGAMENTO_CATEGORIA.DESCRICAO));
					element.setDescricao(record.getValue(PAGAMENTO.DESCRICAO));
					element.setValor(record.getValue(PAGAMENTO.VALOR));
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
				
				return buildEntidade(record);
			}
		}.execute();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pagamento create(final Pagamento registro) throws Exception {
		return new Transaction<Pagamento>(true) {
			@Override
			public Pagamento execute(Executor database) {
				final PagamentoCategoriaRecord categoria = database
						.selectFrom(PAGAMENTO_CATEGORIA)
						.where(PAGAMENTO_CATEGORIA.DESCRICAO.eq(registro.getCategoria()))
						.fetchOne();
				
				final SituacaoPagamento situacaoPagamento = SituacaoPagamento.fromValue(registro.getSituacao());
				final FormaPagamento formaPagamento = FormaPagamento.fromValue(registro.getFormaPagamento());
				
				
				for (int i = 0; i < registro.getProjecao(); i++) {
					final Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(registro.getVencimento().getTime());
					calendar.add(Calendar.MONTH, i);
					final Date vencimento = new Date(calendar.getTimeInMillis());
					
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
									vencimento,
									registro.getDescricao(),
									registro.getValor(),
									situacaoPagamento.name(),
									registro.getPagamento() != null ? new Date(registro.getPagamento().getTime()) : null,
									formaPagamento != null ? formaPagamento.name() : null,
									registro.getBanco(),
									registro.getAgencia(),
									registro.getConta(),
									registro.getCheque()
							)
							.returning(PAGAMENTO.ID)
							.fetchOne();
					
					if (registro.getId() == null) {
						registro.setId(record.getId());
					}
				}
				
				return registro;
			}
		}.execute();
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pagamento update(final @PathParam("id") Long id, final Pagamento registro) throws Exception {
		return new Transaction<Pagamento>(true) {
			@Override
			public Pagamento execute(Executor database) {
				final PagamentoCategoriaRecord categoria = database
						.selectFrom(PAGAMENTO_CATEGORIA)
						.where(PAGAMENTO_CATEGORIA.DESCRICAO.eq(registro.getCategoria()))
						.fetchOne();
				
				final SituacaoPagamento situacaoPagamento = SituacaoPagamento.fromValue(registro.getSituacao());
				final FormaPagamento formaPagamento = FormaPagamento.fromValue(registro.getFormaPagamento());
				
				database.update(PAGAMENTO)
						.set(PAGAMENTO.CATEGORIA_ID, categoria.getValue(PAGAMENTO_CATEGORIA.ID))
						.set(PAGAMENTO.VENCIMENTO, new Date(registro.getVencimento().getTime()))
						.set(PAGAMENTO.DESCRICAO, registro.getDescricao())
						.set(PAGAMENTO.VALOR, registro.getValor())
						.set(PAGAMENTO.SITUACAO, situacaoPagamento.name())
						.set(PAGAMENTO.PAGAMENTO_, registro.getPagamento())
						.set(PAGAMENTO.FORMA_PAGAMENTO, formaPagamento != null ? formaPagamento.name() : null)
						.set(PAGAMENTO.BANCO, registro.getBanco())
						.set(PAGAMENTO.AGENCIA, registro.getAgencia())
						.set(PAGAMENTO.CONTA, registro.getConta())
						.set(PAGAMENTO.CHEQUE, registro.getCheque())
						.where(PAGAMENTO.ID.eq(id))
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
			protected Void execute(Executor database) {
				database.delete(PAGAMENTO)
						.where(PAGAMENTO.ID.eq(id))
						.execute();
				
				return null;
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
						.orderBy(PAGAMENTO_CATEGORIA.DESCRICAO)
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
	@Path("categorias/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria readCategoria(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Categoria>() {
			@Override
			protected Categoria execute(Executor database) {
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
	@Path("categorias")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria createCategoria(final Categoria registro) throws Exception {
		return new Transaction<Categoria>(true) {
			@Override
			public Categoria execute(Executor database) {
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
	@Path("categorias/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria updateCategoria(final @PathParam("id") Long id, final Categoria registro) throws Exception {
		return new Transaction<Categoria>(true) {
			@Override
			public Categoria execute(Executor database) {
				database.update(PAGAMENTO_CATEGORIA)
						.set(PAGAMENTO_CATEGORIA.DESCRICAO, registro.getDescricao())
						.where(PAGAMENTO_CATEGORIA.ID.eq(id))
						.execute();
				
				return registro;
			}
		}.execute();
	}

	@DELETE
	@Path("categorias/{id}")
	public void deleteCategoria(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				database.delete(PAGAMENTO_CATEGORIA)
						.where(PAGAMENTO_CATEGORIA.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}
	
	@GET
	@Path("imprimir")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response print(final @QueryParam("situacao") String situacao, 
			final @QueryParam("start") Date start, 
			final @QueryParam("end") Date end) throws Exception {
		
		ByteArrayOutputStream stream = new FallibleTransaction<ByteArrayOutputStream>() {
			@Override
			protected ByteArrayOutputStream executeFallible(Executor database) throws Exception {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				final Result<Record> pagamentoResult = database
						.selectFrom(PAGAMENTO.join(PAGAMENTO_CATEGORIA).onKey())
						.where(PAGAMENTO.SITUACAO.eq(SituacaoPagamento.fromValue(situacao).name()))
						.and(PAGAMENTO.VENCIMENTO.between(start, end))
						.orderBy(PAGAMENTO_CATEGORIA.DESCRICAO.asc(), PAGAMENTO.VENCIMENTO.desc(), PAGAMENTO.DESCRICAO.asc())
						.fetch();
				
				final Collection<Map<String, ?>> pagamentos = new ArrayList<>(pagamentoResult.size());
				for (Record pagamentoRecord : pagamentoResult) {
					final Map<String, Object> pagamento = new HashMap<>();
					pagamento.put("categoria", pagamentoRecord.getValue(PAGAMENTO_CATEGORIA.DESCRICAO));
					pagamento.put("descricao", pagamentoRecord.getValue(PAGAMENTO.DESCRICAO));
					pagamento.put("vencimento", pagamentoRecord.getValue(PAGAMENTO.VENCIMENTO));
					pagamento.put("valor", pagamentoRecord.getValue(PAGAMENTO.VALOR));
					pagamento.put("situacao", SituacaoPagamento.valueOf(pagamentoRecord.getValue(PAGAMENTO.SITUACAO)).getValue());
					pagamento.put("pagamento", pagamentoRecord.getValue(PAGAMENTO.PAGAMENTO_));
					final String formaPagamento = pagamentoRecord.getValue(PAGAMENTO.FORMA_PAGAMENTO);
					pagamento.put("formaPagamento", formaPagamento != null ? FormaPagamento.valueOf(formaPagamento).getValue() : null);
					pagamento.put("banco", pagamentoRecord.getValue(PAGAMENTO.BANCO));
					pagamento.put("agencia", pagamentoRecord.getValue(PAGAMENTO.AGENCIA));
					pagamento.put("conta", pagamentoRecord.getValue(PAGAMENTO.CONTA));
					pagamento.put("cheque", pagamentoRecord.getValue(PAGAMENTO.CHEQUE));
					
					pagamentos.add(pagamento);
				}
				
				final String tipo;
				switch (SituacaoPagamento.fromValue(situacao)) {
				case PENDENTE:
					tipo = "À PAGAR";
					break;
				case PAGO:
					tipo = "PAGAS";
					break;
				default:
					throw new IllegalArgumentException("Situação inválida");
				}
				
				final Map<String, Object> parameters = new HashMap<>();
				parameters.put("tipo", tipo);
				parameters.put("inicio", start);
				parameters.put("fim", end);
				final JasperReport jasperReport = JasperCompileManager
						.compileReport(getClass().getClassLoader().getResourceAsStream("pagamentos.jrxml"));
				final JasperPrint jasperPrint = JasperFillManager
						.fillReport(jasperReport, parameters, new JRMapCollectionDataSource(pagamentos));
				
				final JRXlsExporter exporter = new JRXlsExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
				exporter.exportReport();
				
				return baos;
			}
		}.execute();
		
		return Response.ok(stream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=pagamentos.xls").build();
	}

	private Pagamento buildEntidade(final Record record) {
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

	@Data
	private static class PagamentoList {
		private Long id;
		private Date vencimento;
		private String categoria;
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
		private Integer projecao;
	}
	
}
