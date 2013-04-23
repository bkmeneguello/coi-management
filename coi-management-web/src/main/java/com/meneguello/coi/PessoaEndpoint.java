package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Pessoa.PESSOA;

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

import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.tables.records.PessoaRecord;
 
@Path("/pessoas")
public class PessoaEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Pessoa> list() throws Exception {
		return new Transaction<List<Pessoa>>() {
			@Override
			protected List<Pessoa> execute(Executor database) {
				final ArrayList<Pessoa> pessoas = new ArrayList<Pessoa>();
				final Result<PessoaRecord> resultPessoaRecord = database.fetch(PESSOA);
				for (PessoaRecord pessoaRecord : resultPessoaRecord) {
					final Pessoa pessoa = new Pessoa();
					pessoa.setId(pessoaRecord.getId());
					pessoa.setNome(pessoaRecord.getNome());
					pessoa.setCodigo(pessoaRecord.getCodigo());
					pessoas.add(pessoa);
				}
				return pessoas;
			}
		}.execute();
	}
 
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Pessoa read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Pessoa>() {
			@Override
			protected Pessoa execute(Executor database) {
				final PessoaRecord pessoaRecord = database.selectFrom(PESSOA)
						.where(PESSOA.ID.eq(id))
						.fetchOne();
				final Pessoa pessoa = new Pessoa();
				pessoa.setId(pessoaRecord.getId());
				pessoa.setNome(pessoaRecord.getNome());
				pessoa.setCodigo(pessoaRecord.getCodigo());

				return pessoa;
			}
		}.execute();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pessoa create(final Pessoa pessoa) throws Exception {
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
				
				return pessoaRecord.getId();
			}
		}.execute();
		
		return read(id);
	}
	
	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pessoa update(final @PathParam("id") Long id, final Pessoa pessoa) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(Executor database) {
				database.update(PESSOA)
					.set(PESSOA.NOME, pessoa.getNome())
					.set(PESSOA.CODIGO, pessoa.getCodigo())
					.where(PESSOA.ID.eq(id))
					.execute();
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
				database.delete(PESSOA)
					.where(PESSOA.ID.eq(id))
					.execute();
				return null;
			}
		}.execute();
	}

}

class Pessoa {
	
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