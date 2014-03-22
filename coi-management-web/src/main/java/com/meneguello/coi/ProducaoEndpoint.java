package com.meneguello.coi;

import static com.meneguello.coi.Utils.asTimestamp;
import static com.meneguello.coi.model.tables.Categoria.CATEGORIA;
import static com.meneguello.coi.model.tables.Comissao.COMISSAO;
import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.EntradaParte.ENTRADA_PARTE;
import static com.meneguello.coi.model.tables.EntradaProduto.ENTRADA_PRODUTO;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;
import static com.meneguello.coi.model.tables.ProdutoCusto.PRODUTO_CUSTO;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.sort;
import static org.jooq.impl.DSL.currentDate;
import static org.jooq.impl.DSL.nvl;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

import org.joda.time.DateTime;
import org.joda.time.DateTime.Property;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;

import com.meneguello.coi.model.tables.records.ComissaoRecord;
import com.meneguello.coi.model.tables.records.EntradaRecord;
 
@Path("/producao")
public class ProducaoEndpoint {
	
	private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
	
	private static final String CONTENT_DISPOSITION_VALUE = "attachment; filename=";
	
	private static final String PRODUCAO_ANALITICO = "producao-analitico.xls";

	private static final String PRODUCAO_SINTETICO = "producao-sintetico.xls";

	private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
	
	private static final Locale LOCALE = new Locale.Builder()
		.setRegion("BR")
		.setLanguage("pt")
		.build();

	@GET
	@Path("/sintetico")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response producao() throws Exception {
		final ByteArrayOutputStream stream = new FallibleTransaction<ByteArrayOutputStream>() {
			@Override
			protected ByteArrayOutputStream executeFallible(DSLContext database) throws Exception {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final DateTime now = DateTime.now().minusMonths(1);
				
				final Map<String, Object> parameters = new HashMap<>();
				parameters.put(JRParameter.REPORT_LOCALE, LOCALE);
				parameters.put("mesReferencia", now.toString("MMMM", LOCALE).toUpperCase());
				final JasperReport jasperReport = JasperCompileManager.compileReport(getClass().getClassLoader().getResourceAsStream("producao-sintetico.jrxml"));
				
				final Collection<Map<String, ?>> producaoData = new ArrayList<>();
				final Collection<Map<String, ?>> pessoasData = new ArrayList<>();
				
				final Property dayOfMonth = now.dayOfMonth();
				final Date firstDayOfMonth = new Date(dayOfMonth.withMinimumValue().getMillis());
				final Date lastDayOfMonth = new Date(dayOfMonth.withMaximumValue().getMillis());
				final Result<EntradaRecord> entradaResult = fetchEntradas(database, firstDayOfMonth, lastDayOfMonth);
				for (EntradaRecord entradaRecord : entradaResult) {
					final Long entradaId = entradaRecord.getValue(ENTRADA.ID);
					final MeioPagamento meioPagamento = MeioPagamento.valueOf(entradaRecord.getValue(ENTRADA.MEIO_PAGAMENTO));
					final BigDecimal meioPagamentoDesconto = meioPagamento.getDesconto();
					
					final Result<Record> entradaProdutoResult = fetchEntradaProdutos(database, entradaId);
					for (Record entradaProdutoRecord : entradaProdutoResult) {
						final String categoriaDescricao = entradaProdutoRecord.getValue(CATEGORIA.DESCRICAO);
						final Integer produtoQuantidade = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.QUANTIDADE);
						final BigDecimal produtoValor = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.VALOR);
						final BigDecimal produtoCusto = entradaProdutoRecord.getValue("CUSTO", BigDecimal.class);
						final BigDecimal produtoDesconto = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.DESCONTO);
						
						final Long categoriaId = entradaProdutoRecord.getValue(CATEGORIA.ID);
						final Result<ComissaoRecord> comissaoResult = fetchComissoes(database, categoriaId);
						for (Record comissaoRecord : comissaoResult) {
							final Parte comissaoParte = Parte.valueOf(comissaoRecord.getValue(COMISSAO.PARTE));
							final BigDecimal comissaoPorcentagem = comissaoRecord.getValue(COMISSAO.PORCENTAGEM);
							final BigDecimal valor = calculaValor(produtoValor, produtoCusto, produtoQuantidade, produtoDesconto, meioPagamentoDesconto, comissaoPorcentagem);
							
							if (Parte.CONSULTORIO.equals(comissaoParte)) {
								final String pessoaNome = fetchPessoaParteNome(database, entradaId, Parte.MEDICO);
								producaoData.add(buildRecordSintetico(pessoaNome, categoriaDescricao, valor));
							} else {
								final String pessoaNome = fetchPessoaParteNome(database, entradaId, comissaoParte);
								pessoasData.add(buildRecordSintetico(pessoaNome, categoriaDescricao, valor));
							}
						}
					}
				}
				
				final List<JasperPrint> jasperPrint = new ArrayList<>();
				jasperPrint.add(JasperFillManager.fillReport(jasperReport, parameters, new JRMapCollectionDataSource(producaoData)));
				jasperPrint.add(JasperFillManager.fillReport(jasperReport, parameters, new JRMapCollectionDataSource(pessoasData)));
				
				final JRXlsExporter exporter = new JRXlsExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT_LIST, jasperPrint);
				exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, true);
				exporter.setParameter(JRXlsExporterParameter.SHEET_NAMES, new String[]{"Produção", "Comissões"});
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
				exporter.exportReport();
				
				return baos;
			}
		}.execute();
		
		return Response.ok(stream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM).header(CONTENT_DISPOSITION_HEADER, CONTENT_DISPOSITION_VALUE + PRODUCAO_SINTETICO).build();
	}
	
	@GET
	@Path("/analitico")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response analitico() throws Exception {
		final ByteArrayOutputStream stream = new FallibleTransaction<ByteArrayOutputStream>() {
			@Override
			protected ByteArrayOutputStream executeFallible(DSLContext database) throws Exception {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final DateTime now = DateTime.now().minusMonths(1);
				
				final Map<String, Object> parameters = new HashMap<>();
				parameters.put(JRParameter.REPORT_LOCALE, LOCALE);
				parameters.put("mesReferencia", now.toString("MMMM", LOCALE).toUpperCase());
				final JasperReport jasperReport = JasperCompileManager.compileReport(getClass().getClassLoader().getResourceAsStream("producao-analitico.jrxml"));
				
				final List<Map<String, ?>> pessoasData = new ArrayList<>();
				
				final Property dayOfMonth = now.dayOfMonth();
				final Date firstDayOfMonth = new Date(dayOfMonth.withMinimumValue().getMillis());
				final Date lastDayOfMonth = new Date(dayOfMonth.withMaximumValue().getMillis());
				final Result<EntradaRecord> entradaResult = fetchEntradas(database, firstDayOfMonth, lastDayOfMonth);
				for (EntradaRecord entradaRecord : entradaResult) {
					final Long entradaId = entradaRecord.getValue(ENTRADA.ID);
					final MeioPagamento meioPagamento = MeioPagamento.valueOf(entradaRecord.getValue(ENTRADA.MEIO_PAGAMENTO));
					final BigDecimal meioPagamentoDesconto = meioPagamento.getDesconto();
					final Date data = entradaRecord.getValue(ENTRADA.DATA);
					final Long pacienteId = entradaRecord.getValue(ENTRADA.PACIENTE_ID);
					final String nomePaciente = database.select(PESSOA.NOME)
							.from(PESSOA)
							.where(PESSOA.ID.eq(pacienteId))
							.fetchOne(PESSOA.NOME);
					
					final Result<Record> entradaProdutoResult = fetchEntradaProdutos(database, entradaId);
					for (Record entradaProdutoRecord : entradaProdutoResult) {
						final String categoriaDescricao = entradaProdutoRecord.getValue(CATEGORIA.DESCRICAO);
						final String produtoCodigo = entradaProdutoRecord.getValue(PRODUTO.CODIGO);
						final String produtoDescricao = entradaProdutoRecord.getValue(PRODUTO.DESCRICAO);
						final Integer produtoQuantidade = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.QUANTIDADE);
						final BigDecimal produtoValor = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.VALOR);
						final BigDecimal produtoCusto = entradaProdutoRecord.getValue("CUSTO", BigDecimal.class);
						final BigDecimal produtoDesconto = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.DESCONTO);
						
						final Long categoriaId = entradaProdutoRecord.getValue(CATEGORIA.ID);
						final Result<ComissaoRecord> comissaoResult = fetchComissoes(database, categoriaId);
						for (Record comissaoRecord : comissaoResult) {
							final Parte comissaoParte = Parte.valueOf(comissaoRecord.getValue(COMISSAO.PARTE));
							final BigDecimal comissaoPorcentagem = comissaoRecord.getValue(COMISSAO.PORCENTAGEM);
							final BigDecimal valor = calculaValor(produtoValor, produtoCusto, produtoQuantidade, produtoDesconto, meioPagamentoDesconto, comissaoPorcentagem);
							
							if (!Parte.CONSULTORIO.equals(comissaoParte)) {
								final String nomeParte = fetchPessoaParteNome(database, entradaId, comissaoParte);
								pessoasData.add(buildRecordAnalitico(nomeParte, categoriaDescricao, produtoCodigo, produtoDescricao, nomePaciente, data, produtoQuantidade,valor));
							}
						}
					}
				}
				
				sort(pessoasData, new PropertyComparator("valor"));
				sort(pessoasData, new PropertyComparator("paciente"));
				sort(pessoasData, new PropertyComparator("produto"));
				sort(pessoasData, new PropertyComparator("codigo"));
				sort(pessoasData, new PropertyComparator("categoria"));
				sort(pessoasData, new PropertyComparator("data"));
				sort(pessoasData, new PropertyComparator("pessoa"));
				
				final JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRMapCollectionDataSource(pessoasData));
				
				final JRXlsExporter exporter = new JRXlsExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
				exporter.exportReport();
				
				return baos;
			}
		}.execute();
		
		return Response.ok(stream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM).header(CONTENT_DISPOSITION_HEADER, CONTENT_DISPOSITION_VALUE + PRODUCAO_ANALITICO).build();
	}

	private Map<String, Object> buildRecordSintetico(final String fetchPessoaParteNome, final String categoriaDescricao, final BigDecimal valor) {
		final Map<String, Object> map = new HashMap<>();
		map.put("pessoa", fetchPessoaParteNome);
		map.put("categoria", categoriaDescricao);
		map.put("valor", valor);
		return map;
	}
	
	private Map<String, Object> buildRecordAnalitico(final String nomeParte, final String categoriaDescricao, String produtoCodigo, String produtoDescricao, String nomePaciente, Date data, Integer produtoQuantidade, final BigDecimal valor) {
		final Map<String, Object> map = new HashMap<>();
		map.put("pessoa", nomeParte);
		map.put("categoria", categoriaDescricao);
		map.put("codigo", produtoCodigo);
		map.put("produto", produtoDescricao);
		map.put("paciente", nomePaciente);
		map.put("data", data);
		map.put("quantidade", produtoQuantidade);
		map.put("valor", valor);
		return map;
	}

	private BigDecimal calculaValor(BigDecimal produtoValor, BigDecimal produtoCusto, Integer produtoQuantidade, BigDecimal produtoDesconto, BigDecimal meioPagamentoDesconto, BigDecimal comissaoPorcentagem) {
		return produtoValor
				.subtract(produtoCusto)
				.multiply(new BigDecimal(produtoQuantidade))
				.subtract(produtoDesconto)
				.multiply(ONE_HUNDRED.subtract(meioPagamentoDesconto).divide(ONE_HUNDRED))
				.multiply(comissaoPorcentagem.divide(ONE_HUNDRED));
	}

	private String fetchPessoaParteNome(DSLContext database, final Long entradaId, final Parte comissaoParte) {
		return database.select(PESSOA.NOME)
			.from(ENTRADA_PARTE.join(PESSOA).onKey())
			.where(ENTRADA_PARTE.ENTRADA_ID.eq(entradaId))
			.and(ENTRADA_PARTE.PARTE.eq(comissaoParte.name()))
			.fetchOne(PESSOA.NOME);
	}

	private Result<ComissaoRecord> fetchComissoes(DSLContext database, Long categoriaId) {
		return database.selectFrom(COMISSAO)
			.where(COMISSAO.CATEGORIA_ID.eq(categoriaId))
			.fetch();
	}

	private Result<Record> fetchEntradaProdutos(DSLContext database, Long entradaId) {
		final List<Field<?>> fields = new ArrayList<>();
		fields.addAll(Arrays.asList(ENTRADA_PRODUTO.fields()));
		fields.addAll(Arrays.asList(PRODUTO.fields()));
		fields.addAll(Arrays.asList(CATEGORIA.fields()));
		fields.add(database.select(nvl(PRODUTO_CUSTO.CUSTO, ZERO))
				.from(PRODUTO_CUSTO)
				.where(PRODUTO_CUSTO.DATA_INICIO_VIGENCIA.le(currentDate()))
				.and(PRODUTO_CUSTO.DATA_FIM_VIGENCIA.isNull().or(PRODUTO_CUSTO.DATA_FIM_VIGENCIA.ge(currentDate()))
				.and(PRODUTO_CUSTO.PRODUTO_ID.eq(PRODUTO.ID)))
				.asField("CUSTO"));
		return database.select(fields)
				.from(ENTRADA_PRODUTO
					.join(PRODUTO).onKey()
					.join(CATEGORIA).onKey())
			.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(entradaId))
			.fetch();
	}

	private Result<EntradaRecord> fetchEntradas(DSLContext database, Date firstDayOfMonth, Date lastDayOfMonth) {
		return database.selectFrom(ENTRADA)
				.where(ENTRADA.DATA.between(asTimestamp(firstDayOfMonth), asTimestamp(lastDayOfMonth)))
				.orderBy(ENTRADA.DATA)
				.fetch();
	}
	
	private final class PropertyComparator implements Comparator<Map<String, ?>> {
		
		private final String property;

		public PropertyComparator(String property) {
			this.property = property;
		}

		@Override
		@SuppressWarnings("unchecked")
		public int compare(Map<String, ?> o1, Map<String, ?> o2) {
			Object v1 = o1.get(property);
			Object v2 = o2.get(property);
			if (v1 == null) {
				if (v2 == null) {
					return  0;
				}
				return -1;
			}
			if (v1 instanceof Comparable<?> && v2 instanceof Comparable<?>) {
				return ((Comparable<Object>)v1).compareTo(v2);
			}
			return 0;
		}
	}
	
}
