package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Categoria.CATEGORIA;
import static com.meneguello.coi.model.tables.Cheque.CHEQUE;
import static com.meneguello.coi.model.tables.Comissao.COMISSAO;
import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.EntradaCheque.ENTRADA_CHEQUE;
import static com.meneguello.coi.model.tables.EntradaParte.ENTRADA_PARTE;
import static com.meneguello.coi.model.tables.EntradaProduto.ENTRADA_PRODUTO;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Data;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRXlsExporter;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.joda.time.DateTime;
import org.joda.time.DateTime.Property;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.Executor;
import org.jooq.impl.Factory;

import com.meneguello.coi.model.Keys;
import com.meneguello.coi.model.tables.records.ChequeRecord;
import com.meneguello.coi.model.tables.records.ComissaoRecord;
import com.meneguello.coi.model.tables.records.EntradaRecord;
import com.meneguello.coi.model.tables.records.PessoaRecord;
 
@Path("/entradas")
public class EntradaEndpoint {
	
	private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
	
	private static final Locale LOCALE = new Locale.Builder()
		.setRegion("BR")
		.setLanguage("pt")
		.build();

	@GET
	@Path("/meios")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> listMeiosPagamento() throws Exception {
		final ArrayList<String> result = new ArrayList<>();
		for (MeioPagamento meioPagamento : MeioPagamento.values()) {
			result.add(meioPagamento.getValue());
		}
		return result;
	}
	
	@GET
	@Path("/producao")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response producao() throws Exception {
		ByteArrayOutputStream stream = new FallibleTransaction<ByteArrayOutputStream>() {
			@Override
			protected ByteArrayOutputStream executeFallible(Executor database) throws Exception {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final DateTime now = DateTime.now();
				
				final Map<String, Object> parameters = new HashMap<>();
				parameters.put(JRParameter.REPORT_LOCALE, LOCALE);
				parameters.put("mesReferencia", now.toString("MMMM", LOCALE).toUpperCase());
				final JasperReport jasperReport = JasperCompileManager.compileReport(getClass().getClassLoader().getResourceAsStream("producao.jrxml"));
				
				final Collection<Map<String, ?>> col = new ArrayList<>();
				
				final Property dayOfMonth = now.dayOfMonth();
				final Date firstDayOfMonth = new Date(dayOfMonth.withMinimumValue().getMillis());
				final Date lastDayOfMonth = new Date(dayOfMonth.withMaximumValue().getMillis());
				final Result<EntradaRecord> entradaResult = fetchEntradas(database, firstDayOfMonth, lastDayOfMonth);
				for (EntradaRecord entradaRecord : entradaResult) {
					final Long entradaId = entradaRecord.getValue(ENTRADA.ID);
					final MeioPagamento meioPagamento = MeioPagamento.valueOf(entradaRecord.getValue(ENTRADA.MEIO_PAGAMENTO));
					
					final Result<Record> entradaProdutoResult = fetchEntradaProdutos(database, entradaId);
					for (Record entradaProdutoRecord : entradaProdutoResult) {
						final String categoriaDescricao = entradaProdutoRecord.getValue(CATEGORIA.DESCRICAO);
						final Integer produtoQuantidade = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.QUANTIDADE);
						final BigDecimal produtoValor = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.VALOR);
						final BigDecimal produtoDesconto = entradaProdutoRecord.getValue(ENTRADA_PRODUTO.DESCONTO);
						
						final Long categoriaId = entradaProdutoRecord.getValue(CATEGORIA.ID);
						final Result<ComissaoRecord> comissaoResult = fetchComissoes(database, categoriaId);
						for (Record comissaoRecord : comissaoResult) {
							final Parte comissaoParte = Parte.valueOf(comissaoRecord.getValue(COMISSAO.PARTE));
							final String comissaoDescricao = comissaoRecord.getValue(COMISSAO.DESCRICAO);
							final BigDecimal comissaoPorcentagem = comissaoRecord.getValue(COMISSAO.PORCENTAGEM);
							
							if (!Parte.MEDICO.equals(comissaoParte)) continue; //TODO: remover
							
							final Record entradaParteRecord = fetchPessoaParte(database, entradaId, comissaoParte, comissaoDescricao);
							if (entradaParteRecord != null) {
								String pessoaNome = entradaParteRecord.getValue(PESSOA.NOME);
								
								Map<String, Object> map = new HashMap<>();
								map.put("pessoa", pessoaNome);
								map.put("categoria", categoriaDescricao);
								map.put("valor", produtoValor
										.multiply(new BigDecimal(produtoQuantidade))
										.subtract(produtoDesconto)
										.multiply(ONE_HUNDRED.subtract(meioPagamento.getDesconto()).divide(ONE_HUNDRED))
										.multiply(comissaoPorcentagem.divide(ONE_HUNDRED)));
								col.add(map);
							} else {
								//TODO: Consultório e outras partes
								if (!Parte.CONSULTORIO.equals(comissaoParte)) {
									parameters.put("erro", true);
								}
							}
						}
					}
				}
				
				final JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRMapCollectionDataSource(col));
				
				final JRXlsExporter exporter = new JRXlsExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
				exporter.exportReport();
				
				return baos;
			}

			private Record fetchPessoaParte(Executor database, final Long entradaId, final Parte comissaoParte,
					final String comissaoDescricao) {
				return database.selectFrom(ENTRADA_PARTE
						.join(PESSOA).onKey())
					.where(ENTRADA_PARTE.ENTRADA_ID.eq(entradaId))
					.and(ENTRADA_PARTE.PARTE.eq(comissaoParte.name()))
					.and(comissaoDescricao == null ? ENTRADA_PARTE.DESCRICAO.isNull() : ENTRADA_PARTE.DESCRICAO.eq(comissaoDescricao))
					.fetchOne();
			}

			private Result<ComissaoRecord> fetchComissoes(Executor database, Long categoriaId) {
				return database.selectFrom(COMISSAO)
					.where(COMISSAO.CATEGORIA_ID.eq(categoriaId))
					.fetch();
			}

			private Result<Record> fetchEntradaProdutos(Executor database, Long entradaId) {
				return database.selectFrom(ENTRADA_PRODUTO
						.join(PRODUTO).onKey()
						.join(CATEGORIA).onKey())
					.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(entradaId))
					.fetch();
			}

			private Result<EntradaRecord> fetchEntradas(Executor database, Date firstDayOfMonth, Date lastDayOfMonth) {
				return database.selectFrom(ENTRADA)
						.where(ENTRADA.DATA.between(firstDayOfMonth, lastDayOfMonth))
						.orderBy(ENTRADA.DATA)
						.fetch();
			}
		}.execute();
		
		return Response.ok(stream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=producao.xls").build();
	}

	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<EntradaList> list() throws Exception {
		return new Transaction<List<EntradaList>>() {
			@Override
			protected List<EntradaList> execute(Executor database) {
				final ArrayList<EntradaList> result = new ArrayList<EntradaList>();
				final List<Field<?>> fields = new ArrayList<>();
				fields.addAll(Arrays.asList(ENTRADA.fields()));				
				fields.addAll(Arrays.asList(PESSOA.fields()));				
				fields.add(database.select(
							Factory.sum(ENTRADA_PRODUTO.VALOR.mul(ENTRADA_PRODUTO.QUANTIDADE))
								.sub(Factory.sum(ENTRADA_PRODUTO.DESCONTO)))
						.from(ENTRADA_PRODUTO)
						.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(ENTRADA.ID))
						.asField("VALOR"));
				final Result<Record> resultRecord = database.select(fields)
						.from(ENTRADA.join(PESSOA).onKey())
						.fetch();
				for (Record record : resultRecord) {
					result.add(buildEntradaList(database, record));
				}
				return result;
			}
		}.execute();
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Entrada read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Entrada>() {
			@Override
			protected Entrada execute(Executor database) {
				final Record record = database.selectFrom(ENTRADA
							.join(PESSOA).onKey()
						)
						.where(ENTRADA.ID.eq(id))
						.fetchOne();
				
				final Entrada entrada = buildEntrada(database, record);
				
				final Result<Record> recordsParte = database.selectFrom(ENTRADA_PARTE
							.join(PESSOA).onKey()
						).where(ENTRADA_PARTE.ENTRADA_ID.eq(id))
						.fetch();
				for (Record recordParte : recordsParte) {
					entrada.getPartes().add(buildParte(recordParte));
				}
				
				final Result<Record> recordsProduto = database.selectFrom(PRODUTO
							.join(ENTRADA_PRODUTO).onKey()
						)
						.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(record.getValue(ENTRADA.ID)))
						.fetch();
				for (Record recordProduto : recordsProduto) {
					entrada.getProdutos().add(buildProduto(recordProduto));
				}
				
				final Result<Record> recordsCheque = database.selectFrom(CHEQUE
							.join(ENTRADA_CHEQUE).onKey()
							.join(PESSOA).onKey(Keys.CHEQUE_FK_CLIENTE)
						)
						.where(ENTRADA_CHEQUE.ENTRADA_ID.eq(record.getValue(ENTRADA.ID)))
						.fetch();
				for (Record recordCheque : recordsCheque) {
					entrada.getCheques().add(buildCheque(recordCheque));
				}
	
				return entrada;
			}
		}.execute();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Entrada create(final Entrada entrada) throws Exception {
		return new Transaction<Entrada>(true) {
			@Override
			public Entrada execute(Executor database) {
				final Pessoa paciente = entrada.getPaciente();
				if (paciente.getId() == null) {
					createPessoa(database, paciente);
				}
				
				final MeioPagamento meioPagamento = MeioPagamento.fromValue(entrada.getTipo());
				
				final EntradaRecord record = database.insertInto(
							ENTRADA, 
							ENTRADA.DATA,
							ENTRADA.PACIENTE_ID,
							ENTRADA.MEIO_PAGAMENTO
						)
						.values(
								new Date(entrada.getData().getTime()),
								paciente.getId(),
								meioPagamento.name()
						)
						.returning(ENTRADA.ID)
						.fetchOne();
				
				entrada.setId(record.getId());
				
				final List<Long> produtoIds = new ArrayList<>();
				for (Produto produto : entrada.getProdutos()) {
					produtoIds.add(produto.getId());
					createProduto(database, entrada, produto);
				}
				
				final List<String> comissoes = loadComissoes(database, produtoIds);
				
				final List<String> partes = new ArrayList<>();
				for (PessoaParte parte : entrada.getPartes()) {
					if (StringUtils.isNotBlank(parte.getDescricao())) {
						partes.add(parte.getDescricao());
					}
					createParte(database, entrada, parte);
				}
				
				comissoes.removeAll(partes);
				if (!comissoes.isEmpty()) {
					throw new WebApplicationException(status(INTERNAL_SERVER_ERROR)
							.entity("Comissões ("+ join(comissoes.toArray(), ", ") +") não cadastradas!")
							.build());
				}
				
				if (MeioPagamento.CHEQUE.equals(meioPagamento)) {
					for (Cheque cheque : entrada.getCheques()) {
						final Pessoa emissor = cheque.getCliente();
						if (emissor.getId() == null && isNotBlank(emissor.getCodigo())) {
							createPessoa(database, emissor);
						}
						
						createCheque(database, cheque, emissor, paciente);
						createEntradaCheque(database, entrada, cheque);
					}
				}
				
				return entrada;
			}
		}.execute();
	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Entrada update(final @PathParam("id") Long id, final Entrada entrada) throws Exception {
		return new Transaction<Entrada>(true) {
			@Override
			public Entrada execute(Executor database) {
				final Pessoa paciente = entrada.getPaciente();
				if (paciente.getId() == null && paciente.getCodigo() != null) {
					createPessoa(database, paciente);
				}
				
				final MeioPagamento meioPagamento = MeioPagamento.fromValue(entrada.getTipo());
				
				database.update(ENTRADA)
						.set(ENTRADA.DATA, new Date(entrada.getData().getTime()))
						.set(ENTRADA.PACIENTE_ID, paciente.getId())
						.set(ENTRADA.MEIO_PAGAMENTO, meioPagamento.name())
						.where(ENTRADA.ID.eq(id))
						.execute();
				
				deleteProdutos(database, id);
				
				final List<Long> produtoIds = new ArrayList<>();
				for (Produto produto : entrada.getProdutos()) {
					produtoIds.add(produto.getId());
					createProduto(database, entrada, produto);
				}
				
				final List<String> comissoes = loadComissoes(database, produtoIds);
				
				deletePartes(database, id);
				
				final List<String> partes = new ArrayList<>();
				for (PessoaParte parte : entrada.getPartes()) {
					if (StringUtils.isNotBlank(parte.getDescricao())) {
						partes.add(parte.getDescricao());
					}
					createParte(database, entrada, parte);
				}
				
				comissoes.removeAll(partes);
				if (!comissoes.isEmpty()) {
					throw new WebApplicationException(status(INTERNAL_SERVER_ERROR)
							.entity("Comissões ("+ StringUtils.join(comissoes.toArray(), ", ") +") não cadastradas!")
							.build());
				}
				
				deleteEntradaCheque(database, entrada.getId());
				if (MeioPagamento.CHEQUE.equals(meioPagamento)) {
					for (Cheque cheque : entrada.getCheques()) {
						final Pessoa emissor = cheque.getCliente();
						if (emissor.getId() == null && isNotBlank(emissor.getCodigo())) {
							createPessoa(database, emissor);
						}
						
						if (cheque.getId() == null) {
							createCheque(database, cheque, emissor, paciente);
						}
						
						createEntradaCheque(database, entrada, cheque);
					}
				}
				
				return entrada;
			}
		}.execute();
	}

	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				deleteProdutos(database, id);
				deletePartes(database, id);
				deleteEntradaCheque(database, id);
				database.delete(ENTRADA)
						.where(ENTRADA.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}

	private EntradaList buildEntradaList(Executor database, Record record) {
		final EntradaList entrada = new EntradaList();
		entrada.setId(record.getValue(ENTRADA.ID));
		entrada.setData(record.getValue(ENTRADA.DATA));
		entrada.setCliente(record.getValue(PESSOA.NOME));
		entrada.setValor((BigDecimal) record.getValue("VALOR"));
		entrada.setTipo(MeioPagamento.valueOf(record.getValue(ENTRADA.MEIO_PAGAMENTO)).getValue());
		return entrada;
	}
	
	private Entrada buildEntrada(Executor database, Record record) {
		final Entrada entrada = new Entrada();
		entrada.setId(record.getValue(ENTRADA.ID));
		entrada.setData(record.getValue(ENTRADA.DATA));
		entrada.setPaciente(getPessoa(database, record.getValue(ENTRADA.PACIENTE_ID)));
		entrada.setTipo(MeioPagamento.valueOf(record.getValue(ENTRADA.MEIO_PAGAMENTO)).getValue());
		return entrada;
	}
	
	private Pessoa getPessoa(Executor database, final Long id) {
		final PessoaRecord pessoaRecord = database.selectFrom(PESSOA)
				.where(PESSOA.ID.eq(id))
				.fetchOne();
		
		final Pessoa pessoa = new Pessoa();
		pessoa.setId(pessoaRecord.getValue(PESSOA.ID));
		pessoa.setNome(pessoaRecord.getValue(PESSOA.NOME));
		pessoa.setCodigo(pessoaRecord.getValue(PESSOA.PREFIXO), pessoaRecord.getValue(PESSOA.CODIGO));
		return pessoa;
	}
	
	private Pessoa buildPessoa(Record record) {
		final Pessoa pessoa = new Pessoa();
		pessoa.setId(record.getValue(PESSOA.ID));
		pessoa.setNome(record.getValue(PESSOA.NOME));
		pessoa.setCodigo(record.getValue(PESSOA.PREFIXO), record.getValue(PESSOA.CODIGO));
		return pessoa;
	}
	
	private Cheque buildCheque(Record record) {
		final Cheque cheque = new Cheque();
		cheque.setId(record.getValue(CHEQUE.ID));
		cheque.setNumero(record.getValue(CHEQUE.NUMERO));
		cheque.setConta(record.getValue(CHEQUE.CONTA));
		cheque.setAgencia(record.getValue(CHEQUE.AGENCIA));
		cheque.setBanco(record.getValue(CHEQUE.BANCO));
		cheque.setDocumento(record.getValue(CHEQUE.DOCUMENTO));
		cheque.setValor(record.getValue(CHEQUE.VALOR));
		cheque.setDataDeposito(record.getValue(CHEQUE.DATA_DEPOSITO));
		cheque.setObservacao(record.getValue(CHEQUE.OBSERVACAO));
		cheque.setCliente(buildPessoa(record));
		return cheque;
	}

	private void createCheque(Executor database, Cheque cheque, Pessoa emissor, Pessoa beneficiario) {
		final ChequeRecord record = database.insertInto(
				CHEQUE, 
				CHEQUE.NUMERO,
				CHEQUE.CONTA,
				CHEQUE.AGENCIA,
				CHEQUE.BANCO,
				CHEQUE.DOCUMENTO,
				CHEQUE.VALOR,
				CHEQUE.DATA_DEPOSITO,
				CHEQUE.OBSERVACAO,
				CHEQUE.CLIENTE_ID,
				CHEQUE.PACIENTE_ID
			)
			.values(
					trimToNull(cheque.getNumero()),
					trimToNull(cheque.getConta()),
					trimToNull(cheque.getAgencia()),
					trimToNull(cheque.getBanco()),
					trimToNull(cheque.getDocumento()),
					cheque.getValor(),
					new Date(cheque.getDataDeposito().getTime()),
					trimToNull(cheque.getObservacao()),
					emissor.getId(),
					beneficiario.getId()
			)
			.returning(CHEQUE.ID)
			.fetchOne();
		cheque.setId(record.getId());
	}
	
	private void createPessoa(Executor database, Pessoa pessoa) {
		final PessoaRecord pessoaRecord = database.insertInto(
				PESSOA, 
				PESSOA.NOME,
				PESSOA.PREFIXO,
				PESSOA.CODIGO
			)
			.values(
					trimToNull(pessoa.getNome()),
					pessoa.getPrefixo(),
					pessoa.getCodigoNumerico()
				)
				.returning(PESSOA.ID)
				.fetchOne();
		
		pessoa.setId(pessoaRecord.getId());
	}
	
	private void createParte(Executor database, Entrada entrada, PessoaParte pessoaParte) {
		final Parte parte = Parte.fromValue(pessoaParte.getParte());
		final Pessoa pessoa = pessoaParte.getPessoa();
		if (pessoa.getId() == null && pessoa.getCodigo() != null) {
			createPessoa(database, pessoa);
		}
		
		database.insertInto(ENTRADA_PARTE,
				ENTRADA_PARTE.DESCRICAO,
				ENTRADA_PARTE.ENTRADA_ID,
				ENTRADA_PARTE.PARTE,
				ENTRADA_PARTE.PESSOA_ID
			)
			.values(pessoaParte.getDescricao(),
				entrada.getId(),
				parte.name(),
				pessoaParte.getPessoa().getId()
			)
			.execute();
	}
	
	private void deleteProdutos(Executor database, Long entradaId) {
		database.delete(ENTRADA_PRODUTO)
				.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(entradaId))
				.execute();
	}

	private void deletePartes(Executor database, Long entradaId) {
		database.delete(ENTRADA_PARTE)
			.where(ENTRADA_PARTE.ENTRADA_ID.eq(entradaId))
			.execute();
	}

	private void createProduto(Executor database, Entrada entrada, Produto produto) {
		database.insertInto(ENTRADA_PRODUTO, 
					ENTRADA_PRODUTO.ENTRADA_ID, 
					ENTRADA_PRODUTO.PRODUTO_ID,
					ENTRADA_PRODUTO.VALOR,
					ENTRADA_PRODUTO.DESCONTO,
					ENTRADA_PRODUTO.QUANTIDADE
				)
				.values(
						entrada.getId(), 
						produto.getId(),
						produto.getPreco(),
						produto.getDesconto(),
						produto.getQuantidade()
				)
				.execute();
	}

	private List<String> loadComissoes(Executor database, List<Long> produtoIds) {
		final List<String> comissoes = new ArrayList<>();
		if (!produtoIds.isEmpty()) {
			final Result<Record1<String>> comissaoRecords = database.select(COMISSAO.DESCRICAO)
				.from(PRODUTO
					.join(CATEGORIA).onKey()
					.join(COMISSAO).onKey(Keys.COMISSAO_FK_CATEGORIA)
				)
				.where(PRODUTO.ID.in(produtoIds))
				.fetch();
			for (Record1<String> comissaoRecord : comissaoRecords) {
				final String descricao = comissaoRecord.getValue(COMISSAO.DESCRICAO);
				if (StringUtils.isNotBlank(descricao)) {
					comissoes.add(descricao);
				}
			}
		}
		return comissoes;
	}

	private Produto buildProduto(Record recordProduto) {
		final Produto produto = new Produto();
		produto.setId(recordProduto.getValue(PRODUTO.ID));
		produto.setCodigo(recordProduto.getValue(PRODUTO.CODIGO));
		produto.setDescricao(recordProduto.getValue(PRODUTO.DESCRICAO));
		produto.setCusto(recordProduto.getValue(PRODUTO.CUSTO));
		produto.setPreco(recordProduto.getValue(ENTRADA_PRODUTO.VALOR));
		produto.setDesconto(recordProduto.getValue(ENTRADA_PRODUTO.DESCONTO));
		produto.setQuantidade(recordProduto.getValue(ENTRADA_PRODUTO.QUANTIDADE));
		return produto;
	}

	private PessoaParte buildParte(Record recordParte) {
		final PessoaParte parte = new PessoaParte();
		parte.setPessoa(buildPessoa(recordParte));
		parte.setDescricao(recordParte.getValue(ENTRADA_PARTE.DESCRICAO));
		parte.setParte(Parte.valueOf(recordParte.getValue(ENTRADA_PARTE.PARTE)).getValue());
		return parte;
	}

	private void createEntradaCheque(Executor database, final Entrada entrada, Cheque cheque) {
		database.insertInto(ENTRADA_CHEQUE,
				ENTRADA_CHEQUE.ENTRADA_ID,
				ENTRADA_CHEQUE.CHEQUE_ID
			)
			.values(
				entrada.getId(),
				cheque.getId()
			)
			.execute();
	}

	private void deleteEntradaCheque(Executor database, Long entradaId) {
		database.delete(ENTRADA_CHEQUE)
			.where(ENTRADA_CHEQUE.ENTRADA_ID.eq(entradaId))
			.execute();
	}

	@Data
	private static class EntradaList {
		private Long id;
		private Date data;
		private String cliente;
		private BigDecimal valor;
		private String tipo;
	}
	
	@Data
	private static class Entrada {
		private Long id;
		private Date data;
		private Pessoa paciente = new Pessoa();
		private BigDecimal valor;
		private String tipo;
		private List<Produto> produtos = new ArrayList<>();
		private List<PessoaParte> partes = new ArrayList<>();
		private List<Cheque> cheques = new ArrayList<>();
	}
	
	@Data @JsonIgnoreProperties({"partes", "prefixo", "codigoNumerico"})
	private static class Pessoa {
		private Long id;
		private String nome;
		private String codigo;
		public void setCodigo(String codigo) {
			if (!Pattern.matches("\\p{Upper}-\\d+", codigo)) 
				throw new WebApplicationException(status(INTERNAL_SERVER_ERROR)
						.entity("Código inválido")
						.build());
			this.codigo = codigo;
		}
		public void setCodigo(String prefixo, Integer codigo) {
			setCodigo(prefixo + "-" + codigo.toString());
		}
		public Integer getCodigoNumerico() {
			return Integer.parseInt(getCodigo().substring(2));
		}
		public String getPrefixo() {
			return getCodigo().substring(0, 1);
		}
	}
	
	@Data
	private static class PessoaParte {
		private Pessoa pessoa = new Pessoa();
		private String descricao;
		private String parte;
	}
	
	@Data @JsonIgnoreProperties({"estocavel"})
	private static class Produto {
		private Long id;
		private String codigo;
		private String descricao;
		private BigDecimal custo = BigDecimal.ZERO;
		private BigDecimal preco = BigDecimal.ZERO;
		private BigDecimal desconto = BigDecimal.ZERO;
		private Integer quantidade;
	}
	
	@Data
	private static class Cheque {
		private Long id;
		private String numero;
		private String conta;
		private String agencia;
		private String banco;
		private String documento;
		private BigDecimal valor;
		private Date dataDeposito;
		private String observacao;
		private Pessoa cliente = new Pessoa();
	}
	
}
