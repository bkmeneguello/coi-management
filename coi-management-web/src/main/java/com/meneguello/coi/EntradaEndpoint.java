package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Entrada.ENTRADA;
import static com.meneguello.coi.model.tables.MeioPagamento.MEIO_PAGAMENTO;
import static com.meneguello.coi.model.tables.Parte.PARTE;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static com.meneguello.coi.model.tables.PessoaParte.PESSOA_PARTE;

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

import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.tables.records.ParteRecord;
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
				final Result<Record5<Long, Timestamp, String, BigDecimal, String>> resultRecord = database.select(
							ENTRADA.ID,
							ENTRADA.DATA,
							PESSOA.NOME,
							ENTRADA.VALOR,
							MEIO_PAGAMENTO.DESCRICAO
						)
						.from(ENTRADA)
						.join(PESSOA).onKey()
						.join(MEIO_PAGAMENTO).onKey()
						.fetch();
				for (Record5<Long, Timestamp, String, BigDecimal, String> record : resultRecord) {
					result.add(buildEntradaList(record));
				}
				return result;
			}
		}.execute();
	}
	
	private EntradaList buildEntradaList(Record5<Long, Timestamp, String, BigDecimal, String> record) {
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
				final PessoaRecord pessoaRecord = database.selectFrom(PESSOA)
						.where(PESSOA.ID.eq(id))
						.fetchOne();
				final Entrada pessoa = buildEntrada(pessoaRecord);
				
				final Result<Record1<String>> recordsParte = database.select(PARTE.DESCRICAO)
						.from(PARTE)
						.join(PESSOA_PARTE).onKey()
						.where(PESSOA_PARTE.PESSOA_ID.eq(pessoaRecord.getId()))
						.fetch();
				for (Record1<String> recordParte : recordsParte) {
					final Parte parte = new Parte();
					parte.setDescricao(recordParte.getValue(PARTE.DESCRICAO));
					pessoa.getPartes().add(parte);
				}

				return pessoa;
			}

			private Entrada buildEntrada(PessoaRecord pessoaRecord) {
				// TODO Auto-generated method stub
				return null;
			}
		}.execute();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Entrada create(final Entrada pessoa) throws Exception {
		final Long id = new Transaction<Long>(true) {
			@Override
			public Long execute(Executor database) {
				final PessoaRecord pessoaRecord = database.insertInto(
						PESSOA, 
						PESSOA.NOME,
						PESSOA.CODIGO
					)
					.values(
							pessoa.getNome(),
							pessoa.getCodigo()
					)
					.returning(PESSOA.ID)
					.fetchOne();
				
				final Long id = pessoaRecord.getId();
				for (Parte parte : pessoa.getPartes()) {
					final ParteRecord parteRecord = database.selectFrom(PARTE)
							.where(PARTE.DESCRICAO.eq(parte.getDescricao()))
							.fetchOne();
					
					database.insertInto(PESSOA_PARTE, 
							PESSOA_PARTE.PESSOA_ID, 
							PESSOA_PARTE.PARTE_ID
						)
						.values(
								id, 
								parteRecord.getId()
						)
						.execute();
				}
				
				return id;
			}
		}.execute();
		
		return read(id);
	}
	
	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Entrada update(final @PathParam("id") Long id, final Entrada pessoa) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(Executor database) {
				database.update(PESSOA)
						.set(PESSOA.NOME, pessoa.getNome())
						.set(PESSOA.CODIGO, pessoa.getCodigo())
						.where(PESSOA.ID.eq(id))
						.execute();
				
				database.delete(PESSOA_PARTE)
						.where(PESSOA_PARTE.PESSOA_ID.eq(id))
						.execute();
				
				for (Parte parte : pessoa.getPartes()) {
					final ParteRecord parteRecord = database.selectFrom(PARTE)
							.where(PARTE.DESCRICAO.eq(parte.getDescricao()))
							.fetchOne();
					
					database.insertInto(PESSOA_PARTE, 
							PESSOA_PARTE.PESSOA_ID, 
							PESSOA_PARTE.PARTE_ID
						)
						.values(
								id, 
								parteRecord.getId()
						)
						.execute();
				}
				
				return null;
			}
		}.execute();
		
		return read(id);
	}
	
	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				database.delete(PESSOA_PARTE)
						.where(PESSOA_PARTE.PESSOA_ID.eq(id))
						.execute();
				
				database.delete(PESSOA)
						.where(PESSOA.ID.eq(id))
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
	
}
