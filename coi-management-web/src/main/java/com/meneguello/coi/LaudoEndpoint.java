package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Laudo.LAUDO;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.sql.Date;
import java.util.ArrayList;
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
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.Keys;
import com.meneguello.coi.model.tables.records.LaudoRecord;
import com.meneguello.coi.model.tables.records.PessoaRecord;
 
@Path("/laudos")
public class LaudoEndpoint {
	
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
	public List<LaudoList> list() throws Exception {
		return new Transaction<List<LaudoList>>() {
			@Override
			protected List<LaudoList> execute(Executor database) {
				final ArrayList<LaudoList> result = new ArrayList<LaudoList>();
				final Result<Record> resultRecord = database.selectFrom(LAUDO
							.join(PESSOA).onKey(Keys.LAUDO_FK_PACIENTE)
						).fetch();
				for (Record record : resultRecord) {
					result.add(buildLaudoList(database, record));
				}
				return result;
			}
		}.execute();
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Laudo read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Laudo>() {
			@Override
			protected Laudo execute(Executor database) {
				final Record record = database.selectFrom(LAUDO)
						.where(LAUDO.ID.eq(id))
						.fetchOne();
				
				final Laudo laudo = buildLaudo(database, record);
	
				return laudo;
			}
		}.execute();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Laudo create(final Laudo laudo) throws Exception {
		return new Transaction<Laudo>(true) {
			@Override
			public Laudo execute(Executor database) {
				final Pessoa paciente = laudo.getPaciente();
				if (paciente.getId() == null) {
					createPessoa(database, paciente);
				}
				
				final Pessoa medico = laudo.getMedico();
				if (medico.getId() == null) {
					createPessoa(database, medico);
				}
				
				final StatusHormonal statusHormonal = StatusHormonal.fromValue(laudo.getStatus());
				
				final LaudoRecord record = database.insertInto(
							LAUDO, 
							LAUDO.DATA,
							LAUDO.PACIENTE_ID,
							LAUDO.MEDICO_ID,
							LAUDO.STATUS_HORMONAL
						)
						.values(
								new Date(laudo.getData().getTime()),
								paciente.getId(),
								medico.getId(),
								statusHormonal.name()
						)
						.returning(LAUDO.ID)
						.fetchOne();
				
				laudo.setId(record.getId());
				
				return laudo;
			}
		}.execute();
	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Laudo update(final @PathParam("id") Long id, final Laudo laudo) throws Exception {
		return new Transaction<Laudo>(true) {
			@Override
			public Laudo execute(Executor database) {
				final Pessoa paciente = laudo.getPaciente();
				if (paciente.getId() == null) {
					createPessoa(database, paciente);
				}
				
				final Pessoa medico = laudo.getPaciente();
				if (medico.getId() == null) {
					createPessoa(database, medico);
				}
				
				final StatusHormonal statusHormonal = StatusHormonal.fromValue(laudo.getStatus());
				
				database.update(LAUDO)
						.set(LAUDO.DATA, new java.sql.Date(laudo.getData().getTime()))
						.set(LAUDO.PACIENTE_ID, paciente.getId())
						.set(LAUDO.MEDICO_ID, medico.getId())
						.set(LAUDO.STATUS_HORMONAL, statusHormonal.name())
						.where(LAUDO.ID.eq(id))
						.execute();
				
				return laudo;
			}
		}.execute();
	}

	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				database.delete(LAUDO)
						.where(LAUDO.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}

	private LaudoList buildLaudoList(Executor database, Record record) {
		final LaudoList laudo = new LaudoList();
		laudo.setId(record.getValue(LAUDO.ID));
		laudo.setData(record.getValue(LAUDO.DATA));
		laudo.setPaciente(record.getValue(PESSOA.NOME));
		laudo.setStatus(StatusHormonal.valueOf(record.getValue(LAUDO.STATUS_HORMONAL)).getValue());
		return laudo;
	}
	
	private Laudo buildLaudo(Executor database, Record record) {
		final Laudo laudo = new Laudo();
		laudo.setId(record.getValue(LAUDO.ID));
		laudo.setData(record.getValue(LAUDO.DATA));
		laudo.setPaciente(getPessoa(database, record.getValue(LAUDO.PACIENTE_ID)));
		laudo.setMedico(getPessoa(database, record.getValue(LAUDO.MEDICO_ID)));
		laudo.setStatus(StatusHormonal.valueOf(record.getValue(LAUDO.STATUS_HORMONAL)).getValue());
		return laudo;
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
	
	@Data
	private static class LaudoList {
		private Long id;
		private Date data;
		private String paciente;
		private String status;
	}
	
	@Data
	private static class Laudo {
		private Long id;
		private Date data;
		private Pessoa paciente = new Pessoa();
		private Pessoa medico = new Pessoa();
		private String status;
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
	
}
