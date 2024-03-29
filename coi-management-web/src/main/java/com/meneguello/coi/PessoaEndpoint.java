package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLIntegrityConstraintViolationException;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectWhereStep;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.meneguello.coi.model.tables.records.PessoaRecord;
import com.sun.jersey.multipart.FormDataParam;
 
@Path("/pessoas")
public class PessoaEndpoint {
	
	private final Logger logger = LoggerFactory.getLogger(PessoaEndpoint.class);
	
	@GET
	@Path("/next")
	@Produces(MediaType.APPLICATION_JSON)
	public Integer next(final @QueryParam("prefix") String prefix) throws Exception {
		return new Transaction<Integer>() {
			@Override
			protected Integer execute(DSLContext database) {
				Record1<Integer> codigoRecord = database.select(PESSOA.CODIGO.max())
						.from(PESSOA)
						.where(PESSOA.PREFIXO.eq(prefix))
						.fetchOne();
				return codigoRecord != null && codigoRecord.value1() != null ? codigoRecord.value1() + 1 : 1;
			}
		}.execute();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Pessoa> list(final @QueryParam("term") String term, final @QueryParam("page") Integer page) throws Exception {
		return new Transaction<List<Pessoa>>() {
			@Override
			protected List<Pessoa> execute(DSLContext database) {
				final ArrayList<Pessoa> pessoas = new ArrayList<Pessoa>();
				final SelectWhereStep<PessoaRecord> select = database.selectFrom(PESSOA);
				if (StringUtils.isNotBlank(term)) {
					select.where(PESSOA.NOME.likeIgnoreCase("%" + term + "%"))
							.or(PESSOA.CODIGO.likeIgnoreCase(term + "%"));
				}
				if (page != null) {
					select.limit(10).offset(10 * page);
				}
				final Result<PessoaRecord> resultPessoaRecord = select
						.orderBy(PESSOA.PREFIXO.asc(), PESSOA.CODIGO.desc())
						.fetch();
				for (PessoaRecord pessoaRecord : resultPessoaRecord) {
					pessoas.add(buildPessoa(pessoaRecord));
				}
				return pessoas;
			}
		}.execute();
	}
	
	private Pessoa buildPessoa(PessoaRecord pessoaRecord) {
		final Pessoa pessoa = new Pessoa();
		pessoa.setId(pessoaRecord.getId());
		pessoa.setNome(pessoaRecord.getNome());
		pessoa.setCodigo(pessoaRecord.getPrefixo(), pessoaRecord.getCodigo());
		return pessoa;
	}
 
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Pessoa read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Pessoa>() {
			@Override
			protected Pessoa execute(DSLContext database) {
				final PessoaRecord pessoaRecord = database.selectFrom(PESSOA)
						.where(PESSOA.ID.eq(id))
						.fetchOne();
				
				return buildPessoa(pessoaRecord);
			}
		}.execute();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pessoa create(final Pessoa pessoa) throws Exception {
		final Long id = new Transaction<Long>(true) {
			@Override
			public Long execute(DSLContext database) {
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
				
				return pessoaRecord.getId();
			}
		}.execute();
		
		return read(id);
	}
	
	@POST
	@Path("/import")
	@Consumes(MULTIPART_FORM_DATA)
	public void fileImport(final @FormDataParam("file") InputStream dados) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(DSLContext database) {
			try (final CSVReader csvReader = new CSVReader(new InputStreamReader(dados, "ISO-8859-1"))) {
					String[] columns = null;
					do {
						try {
							columns = csvReader.readNext();
							if (columns == null) break;
							database.insertInto(
									PESSOA, 
									PESSOA.NOME,
									PESSOA.PREFIXO,
									PESSOA.CODIGO
							)
							.values(
									trimToNull(columns[10]),
									"P", //FIXME: Fixo em Pacientes
									Integer.parseInt(trimToNull(columns[9]))
							)
							.returning(PESSOA.ID)
							.execute();
						} catch (IOException e) {
							logger.error("Falha na importação do registro", e);
						} catch (DataAccessException e) {
							if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
								database.update(PESSOA)
									.set(PESSOA.NOME, trimToNull(columns[10]))
									.where(PESSOA.CODIGO.eq(Integer.parseInt(columns[9])))
									.and(PESSOA.PREFIXO.eq("P")) //TODO: Fixo em Pacientes
									.execute();
							} else {
								logger.error("Falha na importação dos registros", e);
							}
						}
					} while(columns != null);
				} catch (IOException e) {
					logger.error("Falha na importação dos registros", e);
				}
				return null;
			}
		}.execute();
	}
	
	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Pessoa update(final @PathParam("id") Long id, final Pessoa pessoa) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(DSLContext database) {
				database.update(PESSOA)
						.set(PESSOA.NOME, trimToNull(pessoa.getNome()))
						.set(PESSOA.PREFIXO, pessoa.getPrefixo())
						.set(PESSOA.CODIGO, pessoa.getCodigoNumerico())
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
			protected Void execute(DSLContext database) {
				database.delete(PESSOA)
						.where(PESSOA.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}
	
	@Data @JsonIgnoreProperties({"prefixo", "codigoNumerico"})
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