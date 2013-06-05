package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Cheque.CHEQUE;
import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.EntradaProduto.ENTRADA_PRODUTO;
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
import javax.ws.rs.core.MediaType;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.Keys;
import com.meneguello.coi.model.MeioPagamento;
import com.meneguello.coi.model.tables.records.ChequeRecord;
import com.meneguello.coi.model.tables.records.EntradaRecord;
import com.meneguello.coi.model.tables.records.PessoaRecord;
 
@Path("/entradas")
public class EntradaEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<EntradaList> list() throws Exception {
		return new Transaction<List<EntradaList>>() {
			@Override
			protected List<EntradaList> execute(Executor database) {
				final ArrayList<EntradaList> result = new ArrayList<EntradaList>();
				final Result<Record> resultRecord = database.selectFrom(ENTRADA
							.join(PESSOA).onKey(Keys.ENTRADA_FK_PACIENTE)
						)
						.fetch();
				for (Record record : resultRecord) {
					result.add(buildEntradaList(record));
				}
				return result;
			}
		}.execute();
	}
	
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
	
	private EntradaList buildEntradaList(Record record) {
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
				com.meneguello.coi.model.tables.Pessoa pacienteAlias = PESSOA.as("PACIENTE");
				com.meneguello.coi.model.tables.Pessoa medicoAlias = PESSOA.as("MEDICO");
				com.meneguello.coi.model.tables.Pessoa fisioterapeutaAlias = PESSOA.as("FISIOTERAPEUTA");
				final Record record = database.selectFrom(ENTRADA
							.join(pacienteAlias).on(pacienteAlias.ID.equal(ENTRADA.PACIENTE_ID))
							.join(medicoAlias).on(medicoAlias.ID.equal(ENTRADA.MEDICO_ID))
							.join(fisioterapeutaAlias).on(fisioterapeutaAlias.ID.equal(ENTRADA.FISIOTERAPEUTA_ID))
							.join(CHEQUE).onKey()
						)
						.where(ENTRADA.ID.eq(id))
						.fetchOne();
				
				final Entrada entrada = buildEntrada(record);
				entrada.setPaciente(buildPessoa(record, pacienteAlias));
				entrada.setMedico(buildPessoa(record, medicoAlias));
				entrada.setFisioterapeuta(buildPessoa(record, fisioterapeutaAlias));
				
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
	
	private Pessoa buildPessoa(Record record, com.meneguello.coi.model.tables.Pessoa pessoaAlias) {
		final Pessoa pessoa = new Pessoa();
		pessoa.setId(record.getValue(pessoaAlias.ID));
		pessoa.setNome(record.getValue(pessoaAlias.NOME));
		pessoa.setCodigo(record.getValue(pessoaAlias.CODIGO));
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
		cheque.setCliente(buildPessoa(record, PESSOA));
		return cheque;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Entrada create(final Entrada entrada) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(Executor database) {
				final Pessoa paciente = entrada.getPaciente();
				if (paciente.getId() == null) {
					createPessoa(database, paciente);
				}
				
				final Pessoa medico = entrada.getMedico();
				if (medico.getId() == null) {
					createPessoa(database, medico);
				}
				
				final Pessoa fisioterapeuta = entrada.getFisioterapeuta();
				if (fisioterapeuta.getId() == null && fisioterapeuta.getCodigo() != null) {
					createPessoa(database, fisioterapeuta);
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
							ENTRADA.MEDICO_ID,
							ENTRADA.FISIOTERAPEUTA_ID,
							ENTRADA.MEIO_PAGAMENTO,
							ENTRADA.CHEQUE_ID
						)
						.values(
								new java.sql.Date(entrada.getData().getTime()),
								entrada.getValor(),
								paciente.getId(),
								medico.getId(),
								fisioterapeuta.getId(),
								meioPagamento.name(),
								entrada.getCheque().getId()
						)
						.returning(ENTRADA.ID)
						.fetchOne();
				
				entrada.setId(record.getId());
				for (Produto produto : entrada.getProdutos()) {
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
				
				return null;
			}
		}.execute();
		
		return entrada;
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
		new Transaction<Void>(true) {
			@Override
			public Void execute(Executor database) {
				final Pessoa paciente = entrada.getPaciente();
				if (paciente.getId() == null) {
					final PessoaRecord pessoaRecord = database.insertInto(
								PESSOA, 
								PESSOA.NOME,
								PESSOA.CODIGO
							)
							.values(
									trimToNull(paciente.getNome()),
									trimToNull(paciente.getCodigo())
							)
							.returning(PESSOA.ID)
							.fetchOne();
					
					paciente.setId(pessoaRecord.getId());
				}
				
				final MeioPagamento meioPagamento = MeioPagamento.fromValue(entrada.getTipo());
				if (MeioPagamento.CHEQUE.equals(meioPagamento)) {
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
				
				database.delete(ENTRADA_PRODUTO)
						.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(id))
						.execute();
				
				for (Produto produto : entrada.getProdutos()) {
					database.insertInto(ENTRADA_PRODUTO, 
								ENTRADA_PRODUTO.ENTRADA_ID, 
								ENTRADA_PRODUTO.PRODUTO_ID,
								ENTRADA_PRODUTO.QUANTIDADE
							)
							.values(
									id, 
									produto.getId(),
									produto.getQuantidade()
							)
							.execute();
				}
				
				return null;
			}
		}.execute();
		
		return entrada;
	}
	
	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				database.delete(ENTRADA_PRODUTO)
						.where(ENTRADA_PRODUTO.ENTRADA_ID.eq(id))
						.execute();
				
				database.delete(ENTRADA)
						.where(ENTRADA.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}

	private static class EntradaList {
		
		private Long id;
		
		private Date data;
		
		private String cliente;
		
		private BigDecimal valor;
		
		private String tipo;
		
		public Long getId() {
			return id;
		}
		
		public void setId(Long id) {
			this.id = id;
		}
		
		public Date getData() {
			return data;
		}
		
		public void setData(Date data) {
			this.data = data;
		}
		
		public String getCliente() {
			return cliente;
		}
		
		public void setCliente(String cliente) {
			this.cliente = cliente;
		}
		
		public BigDecimal getValor() {
			return valor;
		}
		
		public void setValor(BigDecimal valor) {
			this.valor = valor;
		}
		
		public String getTipo() {
			return tipo;
		}
		
		public void setTipo(String tipo) {
			this.tipo = tipo;
		}
		
	}
	
	private static class Entrada {
		
		private Long id;
		
		private Date data;
		
		private Pessoa paciente = new Pessoa();
		
		private Pessoa medico = new Pessoa();
		
		private Pessoa fisioterapeuta = new Pessoa();
		
		private BigDecimal valor;
		
		private String tipo;
		
		private Cheque cheque = new Cheque();
		
		private List<Produto> produtos = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getData() {
			return data;
		}

		public void setData(Date data) {
			this.data = data;
		}

		public Pessoa getPaciente() {
			return paciente;
		}

		public void setPaciente(Pessoa paciente) {
			this.paciente = paciente;
		}

		public Pessoa getMedico() {
			return medico;
		}

		public void setMedico(Pessoa medico) {
			this.medico = medico;
		}

		public Pessoa getFisioterapeuta() {
			return fisioterapeuta;
		}

		public void setFisioterapeuta(Pessoa fisioterapeuta) {
			this.fisioterapeuta = fisioterapeuta;
		}

		public BigDecimal getValor() {
			return valor;
		}

		public void setValor(BigDecimal valor) {
			this.valor = valor;
		}
		
		public String getTipo() {
			return tipo;
		}
		
		public void setTipo(String tipo) {
			this.tipo = tipo;
		}

		public Cheque getCheque() {
			return cheque;
		}

		public void setCheque(Cheque cheque) {
			this.cheque = cheque;
		}

		public List<Produto> getProdutos() {
			return produtos;
		}
		
	}
	
	private static class Pessoa {
		
		private Long id;
		
		private String nome;
		
		private String codigo;
		
		private List<Parte> partes = new ArrayList<>();
		
		public Long getId() {
			return id;
		}
		
		public void setId(Long id) {
			this.id = id;
		}
		
		public String getNome() {
			return nome;
		}
		
		public void setNome(String nome) {
			this.nome = nome;
		}
		
		public String getCodigo() {
			return codigo;
		}
		
		public void setCodigo(String codigo) {
			this.codigo = codigo;
		}
		
		public List<Parte> getPartes() {
			return partes;
		}
		
	}
	
	private static class Parte {
		
		private String descricao;
		
		public String getDescricao() {
			return descricao;
		}

		public void setDescricao(String descricao) {
			this.descricao = descricao;
		}
		
	}
	
	private static class Produto {
		
		private Long id;
		
		private String codigo;
		
		private String descricao;
		
		private BigDecimal custo = BigDecimal.ZERO;
		
		private BigDecimal preco = BigDecimal.ZERO;
		
		private Integer quantidade;
		
		public Long getId() {
			return id;
		}
		
		public void setId(Long id) {
			this.id = id;
		}
		
		public String getCodigo() {
			return codigo;
		}
		
		public void setCodigo(String codigo) {
			this.codigo = codigo;
		}
		
		public String getDescricao() {
			return descricao;
		}
		
		public void setDescricao(String descricao) {
			this.descricao = descricao;
		}
		
		public BigDecimal getCusto() {
			return custo;
		}
		
		public void setCusto(BigDecimal custo) {
			this.custo = custo;
		}
		
		public BigDecimal getPreco() {
			return preco;
		}
		
		public void setPreco(BigDecimal preco) {
			this.preco = preco;
		}
		
		public Integer getQuantidade() {
			return quantidade;
		}
		
		public void setQuantidade(Integer quantidade) {
			this.quantidade = quantidade;
		}
		
	}
	
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
		
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumero() {
			return numero;
		}

		public void setNumero(String numero) {
			this.numero = numero;
		}

		public String getConta() {
			return conta;
		}

		public void setConta(String conta) {
			this.conta = conta;
		}

		public String getAgencia() {
			return agencia;
		}

		public void setAgencia(String agencia) {
			this.agencia = agencia;
		}

		public String getBanco() {
			return banco;
		}

		public void setBanco(String banco) {
			this.banco = banco;
		}

		public String getDocumento() {
			return documento;
		}

		public void setDocumento(String documento) {
			this.documento = documento;
		}

		public BigDecimal getValor() {
			return valor;
		}

		public void setValor(BigDecimal valor) {
			this.valor = valor;
		}

		public Date getDataDeposito() {
			return dataDeposito;
		}

		public void setDataDeposito(Date dataDeposito) {
			this.dataDeposito = dataDeposito;
		}

		public String getObservacao() {
			return observacao;
		}

		public void setObservacao(String observacao) {
			this.observacao = observacao;
		}

		public Pessoa getCliente() {
			return cliente;
		}

		public void setCliente(Pessoa cliente) {
			this.cliente = cliente;
		}

	}
	
}
