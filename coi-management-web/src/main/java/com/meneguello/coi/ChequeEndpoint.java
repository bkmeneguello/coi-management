package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Cheque.CHEQUE;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
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
import com.meneguello.coi.model.tables.records.ChequeRecord;
import com.meneguello.coi.model.tables.records.PessoaRecord;
 
@Path("/cheques")
public class ChequeEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<ChequeList> list() throws Exception {
		return new Transaction<List<ChequeList>>() {
			@Override
			protected List<ChequeList> execute(Executor database) {
				final ArrayList<ChequeList> result = new ArrayList<>();
				final Result<Record> resultRecord = database.selectFrom(CHEQUE
						.join(PESSOA).onKey(Keys.CHEQUE_FK_PACIENTE))
						.fetch();
				for (Record record : resultRecord) {
					result.add(buildChequeList(record));
				}
				return result;
			}
		}.execute();
	}
	
	private ChequeList buildChequeList(Record record) {
		final ChequeList cheque = new ChequeList();
		cheque.setId(record.getValue(CHEQUE.ID));
		cheque.setValor(record.getValue(CHEQUE.VALOR));
		cheque.setData(new java.sql.Date(record.getValue(CHEQUE.DATA_DEPOSITO).getTime()));
		cheque.setPaciente(record.getValue(PESSOA.NOME));
		return cheque;
	}
	
	private Cheque buildCheque(ChequeRecord record) {
		final Cheque cheque = new Cheque();
		cheque.setId(record.getId());
		cheque.setNumero(record.getNumero());
		cheque.setConta(record.getConta());
		cheque.setAgencia(record.getAgencia());
		cheque.setBanco(record.getBanco());
		cheque.setDocumento(record.getDocumento());
		cheque.setValor(record.getValor());
		cheque.setDataDeposito(record.getDataDeposito());
		cheque.setObservacao(record.getObservacao());
		cheque.setCliente(getPessoa(record.getClienteId()));
		cheque.setPaciente(getPessoa(record.getPacienteId()));
		return cheque;
	}
	
	private Pessoa getPessoa(final Long id) {
		return new Transaction<Pessoa>() {
			@Override
			protected Pessoa execute(Executor database) {
				final PessoaRecord pessoaRecord = database.selectFrom(PESSOA)
						.where(PESSOA.ID.eq(id))
						.fetchOne();
				
				final Pessoa pessoa = new Pessoa();
				pessoa.setId(pessoaRecord.getValue(PESSOA.ID));
				pessoa.setNome(pessoaRecord.getValue(PESSOA.NOME));
				pessoa.setCodigo(pessoaRecord.getValue(PESSOA.CODIGO));
				return pessoa;
			}
		}.execute();
	}
 
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Cheque read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Cheque>() {
			@Override
			protected Cheque execute(Executor database) {
				final ChequeRecord record = database.selectFrom(CHEQUE)
						.where(CHEQUE.ID.eq(id))
						.fetchOne();
				return buildCheque(record);
			}
		}.execute();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Cheque create(final Cheque cheque) throws Exception {
		return new Transaction<Cheque>(true) {
			@Override
			public Cheque execute(Executor database) {
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
							cheque.getCliente().getId(),
							cheque.getPaciente().getId()
					)
					.returning(CHEQUE.ID)
					.fetchOne();
				
				cheque.setId(record.getId());
				return cheque;
			}
		}.execute();
	}
	
	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Cheque update(final @PathParam("id") Long id, final Cheque cheque) throws Exception {
		return new Transaction<Cheque>(true) {
			@Override
			public Cheque execute(Executor database) {
				database.update(CHEQUE)
						.set(CHEQUE.NUMERO, trimToNull(cheque.getNumero()))
						.set(CHEQUE.CONTA, trimToNull(cheque.getConta()))
						.set(CHEQUE.AGENCIA, trimToNull(cheque.getAgencia()))
						.set(CHEQUE.BANCO, trimToNull(cheque.getBanco()))
						.set(CHEQUE.DOCUMENTO, trimToNull(cheque.getDocumento()))
						.set(CHEQUE.VALOR, cheque.getValor())
						.set(CHEQUE.DATA_DEPOSITO, new java.sql.Date(cheque.getDataDeposito().getTime()))
						.set(CHEQUE.OBSERVACAO, trimToNull(cheque.getObservacao()))
						.set(CHEQUE.CLIENTE_ID, cheque.getCliente().getId())
						.set(CHEQUE.PACIENTE_ID, cheque.getPaciente().getId())
						.where(CHEQUE.ID.eq(id))
						.execute();
				return cheque;
			}
		}.execute();
	}
	
	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				database.delete(CHEQUE)
						.where(CHEQUE.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}

	private static class ChequeList {
		
		private Long id;
		
		private BigDecimal valor;
		
		private Date data;
		
		private String paciente;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getValor() {
			return valor;
		}

		public void setValor(BigDecimal valor) {
			this.valor = valor;
		}

		public Date getData() {
			return data;
		}

		public void setData(Date data) {
			this.data = data;
		}

		public String getPaciente() {
			return paciente;
		}

		public void setPaciente(String paciente) {
			this.paciente = paciente;
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
		
		private Pessoa cliente;
		
		private Pessoa paciente;

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

		public Pessoa getPaciente() {
			return paciente;
		}

		public void setPaciente(Pessoa paciente) {
			this.paciente = paciente;
		}
		
	}
	
	private static class Pessoa {
		
		private Long id;
		
		private String nome;
		
		private String codigo;

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
		
	}
	
}