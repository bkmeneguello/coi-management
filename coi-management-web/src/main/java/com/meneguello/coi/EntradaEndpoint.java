package com.meneguello.coi;

import static com.meneguello.coi.Utils.asSQLDate;
import static com.meneguello.coi.Utils.asTimestamp;
import static com.meneguello.coi.model.tables.Cheque.CHEQUE;
import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.EntradaCheque.ENTRADA_CHEQUE;
import static com.meneguello.coi.model.tables.EntradaParte.ENTRADA_PARTE;
import static com.meneguello.coi.model.tables.EntradaProduto.ENTRADA_PRODUTO;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;
import static com.meneguello.coi.model.tables.ProdutoCusto.PRODUTO_CUSTO;
import static java.math.BigDecimal.ZERO;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.jooq.impl.DSL.currentDate;
import static org.jooq.impl.DSL.nvl;
import static org.jooq.impl.DSL.sum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

import lombok.Data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;

import com.meneguello.coi.model.Keys;
import com.meneguello.coi.model.tables.records.ChequeRecord;
import com.meneguello.coi.model.tables.records.EntradaRecord;
import com.meneguello.coi.model.tables.records.PessoaRecord;
 
@Path("/entradas")
public class EntradaEndpoint {
	
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
	@Produces(MediaType.APPLICATION_JSON)
	public List<EntradaList> list() throws Exception {
		return new Transaction<List<EntradaList>>() {
			@Override
			protected List<EntradaList> execute(DSLContext database) {
				final ArrayList<EntradaList> result = new ArrayList<EntradaList>();
				final List<Field<?>> fields = new ArrayList<>();
				fields.addAll(Arrays.asList(ENTRADA.fields()));				
				fields.addAll(Arrays.asList(PESSOA.fields()));				
				fields.add(database.select(
							sum(ENTRADA_PRODUTO.VALOR.mul(ENTRADA_PRODUTO.QUANTIDADE))
								.sub(sum(ENTRADA_PRODUTO.DESCONTO)))
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
			protected Entrada execute(DSLContext database) {
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
				
				final List<Field<?>> fields = new ArrayList<>();
				fields.addAll(Arrays.asList(PRODUTO.fields()));
				fields.addAll(Arrays.asList(ENTRADA_PRODUTO.fields()));
				fields.add(database
					.select(nvl(PRODUTO_CUSTO.CUSTO, ZERO))
					.from(PRODUTO_CUSTO)
					.where(PRODUTO_CUSTO.DATA_INICIO_VIGENCIA.le(currentDate()))
					.and(PRODUTO_CUSTO.DATA_FIM_VIGENCIA.isNull().or(PRODUTO_CUSTO.DATA_FIM_VIGENCIA.ge(currentDate()))
					.and(PRODUTO_CUSTO.PRODUTO_ID.eq(PRODUTO.ID)))
					.asField("CUSTO"));
				
				final Result<Record> recordsProduto = database.select(fields)
							.from(PRODUTO
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
			public Entrada execute(DSLContext database) {
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
								asTimestamp(entrada.getData()),
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
				
				for (PessoaParte parte : entrada.getPartes()) {
					createParte(database, entrada, parte);
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
			public Entrada execute(DSLContext database) {
				final Pessoa paciente = entrada.getPaciente();
				if (paciente.getId() == null && paciente.getCodigo() != null) {
					createPessoa(database, paciente);
				}
				
				final MeioPagamento meioPagamento = MeioPagamento.fromValue(entrada.getTipo());
				
				database.update(ENTRADA)
						.set(ENTRADA.DATA, asTimestamp(entrada.getData()))
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
				
				deletePartes(database, id);
				
				for (PessoaParte parte : entrada.getPartes()) {
					createParte(database, entrada, parte);
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
			protected Void execute(DSLContext database) {
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

	private EntradaList buildEntradaList(DSLContext database, Record record) {
		final EntradaList entrada = new EntradaList();
		entrada.setId(record.getValue(ENTRADA.ID));
		entrada.setData(new java.util.Date(record.getValue(ENTRADA.DATA).getTime()));
		entrada.setCliente(record.getValue(PESSOA.NOME));
		entrada.setValor((BigDecimal) record.getValue("VALOR"));
		entrada.setTipo(MeioPagamento.valueOf(record.getValue(ENTRADA.MEIO_PAGAMENTO)).getValue());
		return entrada;
	}
	
	private Entrada buildEntrada(DSLContext database, Record record) {
		final Entrada entrada = new Entrada();
		entrada.setId(record.getValue(ENTRADA.ID));
		entrada.setData(record.getValue(ENTRADA.DATA));
		entrada.setPaciente(getPessoa(database, record.getValue(ENTRADA.PACIENTE_ID)));
		entrada.setTipo(MeioPagamento.valueOf(record.getValue(ENTRADA.MEIO_PAGAMENTO)).getValue());
		return entrada;
	}
	
	private Pessoa getPessoa(DSLContext database, final Long id) {
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

	private void createCheque(DSLContext database, Cheque cheque, Pessoa emissor, Pessoa beneficiario) {
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
					asSQLDate(cheque.getDataDeposito()),
					trimToNull(cheque.getObservacao()),
					emissor.getId(),
					beneficiario.getId()
			)
			.returning(CHEQUE.ID)
			.fetchOne();
		cheque.setId(record.getId());
	}
	
	private void createPessoa(DSLContext database, Pessoa pessoa) {
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
	
	private void createParte(DSLContext database, Entrada entrada, PessoaParte pessoaParte) {
		final Parte parte = Parte.fromValue(pessoaParte.getParte());
		final Pessoa pessoa = pessoaParte.getPessoa();
		if (pessoa.getId() == null && pessoa.getCodigo() != null) {
			createPessoa(database, pessoa);
		}
		
		database.insertInto(ENTRADA_PARTE,
				ENTRADA_PARTE.ENTRADA_ID,
				ENTRADA_PARTE.PARTE,
				ENTRADA_PARTE.PESSOA_ID
			)
			.values(entrada.getId(),
				parte.name(),
				pessoaParte.getPessoa().getId()
			)
			.execute();
	}
	
	private void deleteProdutos(DSLContext database, Long entradaId) {
		database.delete(ENTRADA_PRODUTO)
				.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(entradaId))
				.execute();
	}

	private void deletePartes(DSLContext database, Long entradaId) {
		database.delete(ENTRADA_PARTE)
			.where(ENTRADA_PARTE.ENTRADA_ID.eq(entradaId))
			.execute();
	}

	private void createProduto(DSLContext database, Entrada entrada, Produto produto) {
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

	private Produto buildProduto(Record recordProduto) {
		final Produto produto = new Produto();
		produto.setId(recordProduto.getValue(PRODUTO.ID));
		produto.setCodigo(recordProduto.getValue(PRODUTO.CODIGO));
		produto.setDescricao(recordProduto.getValue(PRODUTO.DESCRICAO));
		produto.setCusto(recordProduto.getValue("CUSTO", BigDecimal.class));
		produto.setPreco(recordProduto.getValue(ENTRADA_PRODUTO.VALOR));
		produto.setDesconto(recordProduto.getValue(ENTRADA_PRODUTO.DESCONTO));
		produto.setQuantidade(recordProduto.getValue(ENTRADA_PRODUTO.QUANTIDADE));
		return produto;
	}

	private PessoaParte buildParte(Record recordParte) {
		final PessoaParte parte = new PessoaParte();
		parte.setPessoa(buildPessoa(recordParte));
		parte.setParte(Parte.valueOf(recordParte.getValue(ENTRADA_PARTE.PARTE)).getValue());
		return parte;
	}

	private void createEntradaCheque(DSLContext database, final Entrada entrada, Cheque cheque) {
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

	private void deleteEntradaCheque(DSLContext database, Long entradaId) {
		database.delete(ENTRADA_CHEQUE)
			.where(ENTRADA_CHEQUE.ENTRADA_ID.eq(entradaId))
			.execute();
	}

	@Data
	private static class EntradaList {
		private Long id;
		private java.util.Date data;
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
		private String parte;
	}
	
	@Data @JsonIgnoreProperties({"estocavel", "custos"})
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
