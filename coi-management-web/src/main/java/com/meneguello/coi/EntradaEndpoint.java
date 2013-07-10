package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Categoria.CATEGORIA;
import static com.meneguello.coi.model.tables.Cheque.CHEQUE;
import static com.meneguello.coi.model.tables.Comissao.COMISSAO;
import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.EntradaParte.ENTRADA_PARTE;
import static com.meneguello.coi.model.tables.EntradaProduto.ENTRADA_PRODUTO;
import static com.meneguello.coi.model.tables.Parte.PARTE;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.Keys;
import com.meneguello.coi.model.tables.records.ChequeRecord;
import com.meneguello.coi.model.tables.records.EntradaRecord;
import com.meneguello.coi.model.tables.records.ParteRecord;
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
			protected List<EntradaList> execute(Executor database) {
				final ArrayList<EntradaList> result = new ArrayList<EntradaList>();
				final Result<Record> resultRecord = database.selectFrom(ENTRADA
							.join(PESSOA).onKey(Keys.ENTRADA_FK_PACIENTE)
						).fetch();
				for (Record record : resultRecord) {
					result.add(buildEntradaList(database, record));
				}
				return result;
			}
		}.execute();
	}
	
	private EntradaList buildEntradaList(Executor database, Record record) {
		final EntradaList entrada = new EntradaList();
		entrada.setId(record.getValue(ENTRADA.ID));
		entrada.setData(record.getValue(ENTRADA.DATA));
		entrada.setCliente(record.getValue(PESSOA.NOME));
		entrada.setValor(record.getValue(ENTRADA.VALOR));
		entrada.setTipo(MeioPagamento.valueOf(record.getValue(ENTRADA.MEIO_PAGAMENTO)).getValue());
		return entrada;
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Entrada read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Entrada>() {
			@Override
			protected Entrada execute(Executor database) {
				final Record record = database.selectFrom(ENTRADA
							.join(PESSOA).onKey(Keys.ENTRADA_FK_PACIENTE)
							.leftOuterJoin(CHEQUE).onKey()
						)
						.where(ENTRADA.ID.eq(id))
						.fetchOne();
				
				final Entrada entrada = buildEntrada(record);
				entrada.setPaciente(buildPessoa(record));
				
				final Result<Record> recordsParte = database.selectFrom(ENTRADA_PARTE
							.join(PARTE).onKey()
							.join(PESSOA).onKey()
						).where(ENTRADA_PARTE.ENTRADA_ID.eq(id))
						.fetch();
				for (Record recordParte : recordsParte) {
					final Parte parte = new Parte();
					parte.setPessoa(buildPessoa(recordParte));
					parte.setDescricao(recordParte.getValue(ENTRADA_PARTE.DESCRICAO));
					parte.setParte(recordParte.getValue(PARTE.DESCRICAO));
					entrada.getPartes().add(parte);
				}
				
				final Result<Record> recordsProduto = database.selectFrom(PRODUTO
							.join(ENTRADA_PRODUTO).onKey()
						)
						.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(record.getValue(ENTRADA.ID)))
						.fetch();
				for (Record recordProduto : recordsProduto) {
					final Produto produto = new Produto();
					produto.setId(recordProduto.getValue(PRODUTO.ID));
					produto.setCodigo(recordProduto.getValue(PRODUTO.CODIGO));
					produto.setDescricao(recordProduto.getValue(PRODUTO.DESCRICAO));
					produto.setCusto(recordProduto.getValue(PRODUTO.CUSTO));
					produto.setPreco(recordProduto.getValue(PRODUTO.PRECO));
					produto.setQuantidade(recordProduto.getValue(ENTRADA_PRODUTO.QUANTIDADE));
					entrada.getProdutos().add(produto);
				}

				return entrada;
			}
		}.execute();
	}
	
	private Entrada buildEntrada(Record record) {
		final Entrada entrada = new Entrada();
		entrada.setId(record.getValue(ENTRADA.ID));
		entrada.setData(record.getValue(ENTRADA.DATA));
		entrada.setValor(record.getValue(ENTRADA.VALOR));
		final MeioPagamento meioPagamento = MeioPagamento.valueOf(record.getValue(ENTRADA.MEIO_PAGAMENTO));
		entrada.setTipo(meioPagamento.getValue());
		if (MeioPagamento.CHEQUE.equals(meioPagamento)) {
			entrada.setCheque(buildCheque(record));
		}
		return entrada;
	}
	
	private Pessoa buildPessoa(Record record) {
		final Pessoa pessoa = new Pessoa();
		pessoa.setId(record.getValue(PESSOA.ID));
		pessoa.setNome(record.getValue(PESSOA.NOME));
		pessoa.setCodigo(record.getValue(PESSOA.CODIGO));
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
				if (MeioPagamento.CHEQUE.equals(meioPagamento)) {
					final Cheque cheque = entrada.getCheque();
					
					final Pessoa cliente = cheque.getCliente();
					if (cliente.getId() == null) {
						createPessoa(database, cliente);
					}
					
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
								new java.sql.Date(cheque.getDataDeposito().getTime()),
								trimToNull(cheque.getObservacao()),
								cliente.getId(),
								paciente.getId()
						)
						.returning(CHEQUE.ID)
						.fetchOne();
					cheque.setId(record.getId());
				}
				
				final EntradaRecord record = database.insertInto(
							ENTRADA, 
							ENTRADA.DATA,
							ENTRADA.VALOR,
							ENTRADA.PACIENTE_ID,
							ENTRADA.MEIO_PAGAMENTO,
							ENTRADA.CHEQUE_ID
						)
						.values(
								new java.sql.Date(entrada.getData().getTime()),
								entrada.getValor(),
								paciente.getId(),
								meioPagamento.name(),
								entrada.getCheque().getId()
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
				for (Parte parte : entrada.getPartes()) {
					if (StringUtils.isNotBlank(parte.getDescricao())) {
						partes.add(parte.getDescricao());
					}
					createParte(database, entrada, parte);
				}
				
				comissoes.removeAll(partes);
				if (!comissoes.isEmpty()) {
					throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity("Comissões ("+ StringUtils.join(comissoes.toArray(), ", ") +") não cadastradas!")
							.build());
				}
				
				return entrada;
			}
		}.execute();
	}
	
	private void createPessoa(Executor database, final Pessoa pessoa) {
		final PessoaRecord pessoaRecord = database.insertInto(
				PESSOA, 
				PESSOA.NOME,
				PESSOA.CODIGO
			)
			.values(
					trimToNull(pessoa.getNome()),
					trimToNull(pessoa.getCodigo())
				)
				.returning(PESSOA.ID)
				.fetchOne();
		
		pessoa.setId(pessoaRecord.getId());
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
				if (!MeioPagamento.CHEQUE.equals(meioPagamento)) {
					entrada.setCheque(new Cheque());
				}
				database.update(ENTRADA)
						.set(ENTRADA.DATA, new java.sql.Date(entrada.getData().getTime()))
						.set(ENTRADA.VALOR, entrada.getValor())
						.set(ENTRADA.PACIENTE_ID, paciente.getId())
						.set(ENTRADA.MEIO_PAGAMENTO, meioPagamento.name())
						.set(ENTRADA.CHEQUE_ID, entrada.getCheque().getId())
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
				for (Parte parte : entrada.getPartes()) {
					if (StringUtils.isNotBlank(parte.getDescricao())) {
						partes.add(parte.getDescricao());
					}
					createParte(database, entrada, parte);
				}
				
				comissoes.removeAll(partes);
				if (!comissoes.isEmpty()) {
					throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity("Comissões ("+ StringUtils.join(comissoes.toArray(), ", ") +") não cadastradas!")
							.build());
				}
				
				return entrada;
			}
		}.execute();
	}
	
	private void createParte(Executor database, final Entrada entrada, Parte parte) {
		final ParteRecord parteRecord = database.selectFrom(PARTE)
				.where(PARTE.DESCRICAO.eq(parte.getParte()))
				.fetchOne();
		
		final Pessoa pessoa = parte.getPessoa();
		if (pessoa.getId() == null && pessoa.getCodigo() != null) {
			createPessoa(database, pessoa);
		}
		
		database.insertInto(ENTRADA_PARTE,
				ENTRADA_PARTE.DESCRICAO,
				ENTRADA_PARTE.ENTRADA_ID,
				ENTRADA_PARTE.PARTE_ID,
				ENTRADA_PARTE.PESSOA_ID
			)
			.values(parte.getDescricao(),
				entrada.getId(),
				parteRecord.getId(),
				parte.getPessoa().getId()
			)
			.execute();
	}
	
	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				deleteProdutos(database, id);
				deletePartes(database, id);
				database.delete(ENTRADA)
						.where(ENTRADA.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}
	
	private void deleteProdutos(Executor database, final Long entradaId) {
		database.delete(ENTRADA_PRODUTO)
				.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(entradaId))
				.execute();
	}

	private void deletePartes(Executor database, final Long entradaId) {
		database.delete(ENTRADA_PARTE)
			.where(ENTRADA_PARTE.ENTRADA_ID.eq(entradaId))
			.execute();
	}

	private void createProduto(Executor database, final Entrada entrada, Produto produto) {
		database.insertInto(ENTRADA_PRODUTO, 
					ENTRADA_PRODUTO.ENTRADA_ID, 
					ENTRADA_PRODUTO.PRODUTO_ID,
					ENTRADA_PRODUTO.QUANTIDADE
				)
				.values(
						entrada.getId(), 
						produto.getId(),
						produto.getQuantidade()
				)
				.execute();
	}

	private List<String> loadComissoes(Executor database,
			final List<Long> produtoIds) {
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
		private Cheque cheque = new Cheque();
		private List<Produto> produtos = new ArrayList<>();
		private List<Parte> partes = new ArrayList<>();
	}
	
	@Data @JsonIgnoreProperties({"partes"})
	private static class Pessoa {
		private Long id;
		private String nome;
		private String codigo;
	}
	
	@Data
	private static class Parte {
		private Pessoa pessoa = new Pessoa();
		private String descricao;
		private String parte;
	}
	
	@Data
	private static class Produto {
		private Long id;
		private String codigo;
		private String descricao;
		private BigDecimal custo = BigDecimal.ZERO;
		private BigDecimal preco = BigDecimal.ZERO;
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
