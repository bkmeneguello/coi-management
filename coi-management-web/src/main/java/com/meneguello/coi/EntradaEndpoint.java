package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.EntradaProduto.ENTRADA_PRODUTO;
import static com.meneguello.coi.model.tables.MeioPagamento.MEIO_PAGAMENTO;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;

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

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.tables.records.EntradaRecord;
import com.meneguello.coi.model.tables.records.MeioPagamentoRecord;
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
							.join(PESSOA).onKey()
							.join(MEIO_PAGAMENTO).onKey()
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
		return new Transaction<List<String>>() {
			@Override
			protected List<String> execute(Executor database) {
				final ArrayList<String> result = new ArrayList<>();
				final Result<MeioPagamentoRecord> resultRecord = database.selectFrom(MEIO_PAGAMENTO)
						.fetch();
				for (MeioPagamentoRecord record : resultRecord) {
					result.add(record.getDescricao());
				}
				return result;
			}
		}.execute();
	}
	
	private EntradaList buildEntradaList(Record record) {
		final EntradaList entrada = new EntradaList();
		entrada.setId(record.getValue(ENTRADA.ID));
		entrada.setData(record.getValue(ENTRADA.DATA));
		entrada.setCliente(record.getValue(PESSOA.NOME));
		entrada.setValor(record.getValue(ENTRADA.VALOR));
		entrada.setTipo(record.getValue(MEIO_PAGAMENTO.DESCRICAO));
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
							.join(PESSOA).onKey()
							.join(MEIO_PAGAMENTO).onKey()
						)
						.where(ENTRADA.ID.eq(id))
						.fetchOne();
				final Entrada entrada = buildEntrada(record);
				
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
		entrada.setPaciente(buildPessoa(record));
		entrada.setValor(record.getValue(ENTRADA.VALOR));
		entrada.setTipo(record.getValue(MEIO_PAGAMENTO.DESCRICAO));
		return entrada;
	}
	
	private Pessoa buildPessoa(Record record) {
		final Pessoa pessoa = new Pessoa();
		pessoa.setId(record.getValue(PESSOA.ID));
		pessoa.setNome(record.getValue(PESSOA.NOME));
		pessoa.setCodigo(record.getValue(PESSOA.CODIGO));
		return pessoa;
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
					final PessoaRecord pessoaRecord = database.insertInto(
								PESSOA, 
								PESSOA.NOME,
								PESSOA.CODIGO
							)
							.values(
									paciente.getNome(),
									paciente.getCodigo()
							)
							.returning(PESSOA.ID)
							.fetchOne();
					
					paciente.setId(pessoaRecord.getId());
				}
				
				final MeioPagamentoRecord meioPagamentoRecord = database.selectFrom(MEIO_PAGAMENTO)
						.where(MEIO_PAGAMENTO.DESCRICAO.eq(entrada.getTipo()))
						.fetchOne();
				
				final EntradaRecord record = database.insertInto(
							ENTRADA, 
							ENTRADA.DATA,
							ENTRADA.VALOR,
							ENTRADA.PACIENTE_ID,
							ENTRADA.MEIO_PAGAMENTO_ID
						)
						.values(
								new Timestamp(entrada.getData().getTime()),
								entrada.getValor(),
								paciente.getId(),
								meioPagamentoRecord.getId()
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
									paciente.getNome(),
									paciente.getCodigo()
							)
							.returning(PESSOA.ID)
							.fetchOne();
					
					paciente.setId(pessoaRecord.getId());
				}
				
				final MeioPagamentoRecord meioPagamentoRecord = database.selectFrom(MEIO_PAGAMENTO)
						.where(MEIO_PAGAMENTO.DESCRICAO.eq(entrada.getTipo()))
						.fetchOne();
				
				database.update(ENTRADA)
						.set(ENTRADA.DATA, new Timestamp(entrada.getData().getTime()))
						.set(ENTRADA.VALOR, entrada.getValor())
						.set(ENTRADA.PACIENTE_ID, paciente.getId())
						.set(ENTRADA.MEIO_PAGAMENTO_ID, meioPagamentoRecord.getId())
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
		
		private Pessoa paciente;
		
		private BigDecimal valor;
		
		private String tipo;
		
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
	
}
