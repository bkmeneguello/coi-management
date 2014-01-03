package com.meneguello.coi;

import static com.meneguello.coi.Utils.asTimestamp;
import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.EntradaProduto.ENTRADA_PRODUTO;
import static com.meneguello.coi.model.tables.Fechamento.FECHAMENTO;
import static com.meneguello.coi.model.tables.FechamentoSaida.FECHAMENTO_SAIDA;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.reverse;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.sum;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
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

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SelectJoinStep;

import com.meneguello.coi.model.tables.records.FechamentoRecord;
import com.meneguello.coi.model.tables.records.FechamentoSaidaRecord;

@Path("fechamentos")
public class FechamentoEndpoint {
	
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
									.sub(select(FECHAMENTO_SAIDA.VALOR.sum())
											.from(FECHAMENTO_SAIDA)
											.where(FECHAMENTO_SAIDA.FECHAMENTO_ID.eq(FECHAMENTO.ID))
											.asField())
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
	
	@GET
	@Path("imprimir")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response print() throws Exception {
		ByteArrayOutputStream stream = new FallibleTransaction<ByteArrayOutputStream>() {
			@Override
			protected ByteArrayOutputStream executeFallible(DSLContext database) throws Exception {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				final Result<FechamentoRecord> fechamentoResult = database
						.selectFrom(FECHAMENTO)
						.orderBy(FECHAMENTO.DATA)
						.fetch();
				
				Timestamp lastDate = null;
				
				final List<Map<String, ?>> fechamentos = new ArrayList<>();
				for (Record fechamentoRecord : fechamentoResult) {
					final Map<String, Object> fechamento = new HashMap<>();
					final Timestamp dataFechamento = fechamentoRecord.getValue(FECHAMENTO.DATA);
					final Timestamp date = dataFechamento;
					
					fechamento.put("data", date);
					fechamento.put("valorDinheiro", fechamentoRecord.getValue(FECHAMENTO.VALOR_DINHEIRO));
					fechamento.put("valorCartao", fechamentoRecord.getValue(FECHAMENTO.VALOR_CARTAO));
					fechamento.put("valorCheque", fechamentoRecord.getValue(FECHAMENTO.VALOR_CHEQUE));
					
					final List<Field<?>> fields = new ArrayList<>();
					fields.add(ENTRADA.MEIO_PAGAMENTO);
					fields.add(database.select(
								sum(ENTRADA_PRODUTO.VALOR.mul(ENTRADA_PRODUTO.QUANTIDADE))
									.sub(sum(ENTRADA_PRODUTO.DESCONTO)))
							.from(ENTRADA_PRODUTO)
							.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(ENTRADA.ID))
							.asField("VALOR"));
					
					final SelectJoinStep<Record> select = database.select(fields).from(ENTRADA);
					
					if (lastDate == null) {
						select.where(ENTRADA.DATA.lt(dataFechamento));						
					} else {
						select.where(ENTRADA.DATA.between(lastDate).and(dataFechamento));
					}
					
					BigDecimal valorDinheiroCaixa = BigDecimal.ZERO;
					BigDecimal valorCartaoCaixa = BigDecimal.ZERO;
					BigDecimal valorChequeCaixa = BigDecimal.ZERO;
					final Result<Record> entradaResult = select.fetch();
					for (Record entradaRecord : entradaResult) {
						switch (MeioPagamento.valueOf(entradaRecord.getValue(ENTRADA.MEIO_PAGAMENTO))) {
						case DINHEIRO:
						case CARTAO_DEBITO:
							valorDinheiroCaixa = valorDinheiroCaixa.add(entradaRecord.getValue("VALOR", BigDecimal.class));
							break;
						case CARTAO_CREDITO:
						case CARTAO_CREDITO_2X:
						case CARTAO_CREDITO_3X:
							valorCartaoCaixa = valorCartaoCaixa.add(entradaRecord.getValue("VALOR", BigDecimal.class));
							break;
						case CHEQUE:
							valorChequeCaixa = valorChequeCaixa.add(entradaRecord.getValue("VALOR", BigDecimal.class));
							break;
						}
					}
					fechamento.put("valorDinheiroCaixa", valorDinheiroCaixa);
					fechamento.put("valorCartaoCaixa", valorCartaoCaixa);
					fechamento.put("valorChequeCaixa", valorChequeCaixa);
					
					fechamentos.add(fechamento);
					
					lastDate = date;
				}
				
				reverse(fechamentos);
				
				final Map<String, Object> parameters = new HashMap<>();
				final JasperReport jasperReport = JasperCompileManager
						.compileReport(getClass().getClassLoader().getResourceAsStream("fechamentos.jrxml"));
				final JasperPrint jasperPrint = JasperFillManager
						.fillReport(jasperReport, parameters, new JRMapCollectionDataSource(fechamentos));
				
				final JRXlsExporter exporter = new JRXlsExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
				exporter.exportReport();
				
				return baos;
			}
		}.execute();
		
		return Response.ok(stream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=fechamentos.xls").build();
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
