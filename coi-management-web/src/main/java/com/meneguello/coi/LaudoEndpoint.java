package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Laudo.LAUDO;
import static com.meneguello.coi.model.tables.LaudoObservacao.LAUDO_OBSERVACAO;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import javax.ws.rs.core.Response;

import lombok.Data;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.joda.time.DateTime;
import org.joda.time.Years;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.Executor;

import com.meneguello.coi.model.Keys;
import com.meneguello.coi.model.tables.records.LaudoObservacaoRecord;
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
	@Path("/print/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response print(final @PathParam("id") Long id) throws Exception {
		ByteArrayOutputStream stream = new FallibleTransaction<ByteArrayOutputStream>() {
			@Override
			protected ByteArrayOutputStream executeFallible(Executor database) throws Exception {
				final LaudoRecord laudoRecord = database.fetchOne(LAUDO, LAUDO.ID.eq(id));
				final PessoaRecord pacienteRecord = database.fetchOne(PESSOA, PESSOA.ID.eq(laudoRecord.getPacienteId()));
				final PessoaRecord medicoRecord = database.fetchOne(PESSOA, PESSOA.ID.eq(laudoRecord.getMedicoId()));
				
				final StatusHormonal statusHormonal = StatusHormonal.valueOf(laudoRecord.getStatusHormonal());

				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				JasperReport jasperReportCapa = JasperCompileManager.compileReport(getClass().getClassLoader().getResourceAsStream("laudo-capa.jrxml"));
				HashMap<String, Object> capaParameters = new HashMap<String, Object>();
				capaParameters.put("paciente", pacienteRecord.getNome());
				capaParameters.put("medico", medicoRecord.getNome());
				capaParameters.put("sexo", Sexo.valueOf(pacienteRecord.getSexo()).getValue());
				capaParameters.put("idade", Years.yearsBetween(new DateTime(pacienteRecord.getDataNascimento().getTime()), DateTime.now()).getYears());
				capaParameters.put("status", statusHormonal.getValue());
				capaParameters.put("data", new Date(DateTime.now().getMillis()));
				JasperPrint jasperPrintCapa = JasperFillManager.fillReport(jasperReportCapa, capaParameters, new JREmptyDataSource());
				
				String laudoNome = null;
				switch (statusHormonal) {
				case PRE_MENOPAUSAL:
					laudoNome = "laudo-pre.jrxml";
					break;
				case TRANSICAO_MENOPAUSAL:
				case POS_MENOPAUSAL:
					laudoNome = "laudo-pos.jrxml";
					break;
				}
				
				JasperReport jasperReportLaudo = JasperCompileManager.compileReport(getClass().getClassLoader().getResourceAsStream(laudoNome));
				JasperPrint jasperPrintLaudo = JasperFillManager.fillReport(jasperReportLaudo, capaParameters, new JREmptyDataSource());
				
				JRPdfExporter exporter = new JRPdfExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT_LIST, Arrays.asList(jasperPrintCapa, jasperPrintLaudo));
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
				exporter.exportReport();
				
				return baos;
			}
		}.execute();
		
		return Response.ok(stream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=laudo.pdf").build();
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
				List<Field<?>> fields = new ArrayList<>(Arrays.asList(LAUDO.fields()));;
				fields.add(PESSOA.DATA_NASCIMENTO);
				fields.add(PESSOA.SEXO);
				final Record record = database.select(fields)
						.from(LAUDO.join(PESSOA).onKey(Keys.LAUDO_FK_PACIENTE))
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
					createPessoa(database, 
							paciente,
							laudo.getDataNascimento(), 
							Sexo.fromValue(laudo.getSexo()));
				} else {
					updatePessoa(database, 
							paciente,
							laudo.getDataNascimento(), 
							Sexo.fromValue(laudo.getSexo()));
				}
				
				final Pessoa medico = laudo.getMedico();
				if (medico.getId() == null) {
					createPessoa(database, medico, null, null);
				}
				
				final StatusHormonal statusHormonal = StatusHormonal.fromValue(laudo.getStatus());
				final ConclusaoLaudo conclusao = ConclusaoLaudo.fromValue(laudo.getConclusao());
				
				final LaudoRecord record = database.insertInto(
							LAUDO, 
							LAUDO.DATA,
							LAUDO.PACIENTE_ID,
							LAUDO.MEDICO_ID,
							LAUDO.STATUS_HORMONAL,
							LAUDO.COLUNA_LOMBAR_L1,
							LAUDO.COLUNA_LOMBAR_L2,
							LAUDO.COLUNA_LOMBAR_L3,
							LAUDO.COLUNA_LOMBAR_L4,
							LAUDO.COLUNA_LOMBAR_DENSIDADE,
							LAUDO.COLUNA_LOMBAR_TSCORE,
							LAUDO.COLUNA_LOMBAR_ZSCORE,
							LAUDO.COLO_FEMUR_DENSIDADE,
							LAUDO.COLO_FEMUR_TSCORE,
							LAUDO.COLO_FEMUR_ZSCORE,
							LAUDO.FEMUR_TOTAL_DENSIDADE,
							LAUDO.FEMUR_TOTAL_TSCORE,
							LAUDO.FEMUR_TOTAL_ZSCORE,
							LAUDO.RADIO_TERCO_DENSIDADE,
							LAUDO.RADIO_TERCO_TSCORE,
							LAUDO.RADIO_TERCO_ZSCORE,
							LAUDO.CORPO_INTEIRO_DENSIDADE,
							LAUDO.CORPO_INTEIRO_ZSCORE,
							LAUDO.CONCLUSAO
						)
						.values(
								new Date(laudo.getData().getTime()),
								paciente.getId(),
								medico.getId(),
								statusHormonal.name(),
								laudo.isColunaLombarL1() ? "S" : "N",
								laudo.isColunaLombarL2() ? "S" : "N",
								laudo.isColunaLombarL3() ? "S" : "N",
								laudo.isColunaLombarL4() ? "S" : "N",
								laudo.getColunaLombarDensidade(),
								laudo.getColunaLombarTScore(),
								laudo.getColunaLombarZScore(),
								laudo.getColoFemurDensidade(),
								laudo.getColoFemurTScore(),
								laudo.getColoFemurZScore(),
								laudo.getFemurTotalDensidade(),
								laudo.getFemurTotalTScore(),
								laudo.getFemurTotalZScore(),
								laudo.getRadioTercoDensidade(),
								laudo.getRadioTercoTScore(),
								laudo.getRadioTercoZScore(),
								laudo.getCorpoInteiroDensidade(),
								laudo.getCorpoInteiroZScore(),
								conclusao.name()
						)
						.returning(LAUDO.ID)
						.fetchOne();
				
				laudo.setId(record.getId());
				
				for (Observacao observacao : laudo.getObservacoes()) {
					database.insertInto(
							LAUDO_OBSERVACAO, 
							LAUDO_OBSERVACAO.LAUDO_ID,
							LAUDO_OBSERVACAO.CODIGO,
							LAUDO_OBSERVACAO.DESCRICAO,
							LAUDO_OBSERVACAO.EXTRA
						)
						.values(
								laudo.getId(),
								observacao.getCodigo(),
								observacao.getDescricao(),
								observacao.getExtra()
						)
						.execute();
				}
				
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
					createPessoa(database, 
							paciente, 
							laudo.getDataNascimento(), 
							Sexo.fromValue(laudo.getSexo()));
				} else {
					updatePessoa(database, 
							paciente,
							laudo.getDataNascimento(), 
							Sexo.fromValue(laudo.getSexo()));
				}
				
				final Pessoa medico = laudo.getMedico();
				if (medico.getId() == null) {
					createPessoa(database, medico, null, null);
				}
				
				final StatusHormonal statusHormonal = StatusHormonal.fromValue(laudo.getStatus());
				final ConclusaoLaudo conclusao = ConclusaoLaudo.fromValue(laudo.getConclusao());
				
				database.update(LAUDO)
						.set(LAUDO.DATA, new java.sql.Date(laudo.getData().getTime()))
						.set(LAUDO.PACIENTE_ID, paciente.getId())
						.set(LAUDO.MEDICO_ID, medico.getId())
						.set(LAUDO.STATUS_HORMONAL, statusHormonal.name())
						.set(LAUDO.COLUNA_LOMBAR_L1, laudo.isColunaLombarL1() ? "S" : "N")
						.set(LAUDO.COLUNA_LOMBAR_L2, laudo.isColunaLombarL2() ? "S" : "N")
						.set(LAUDO.COLUNA_LOMBAR_L3, laudo.isColunaLombarL3() ? "S" : "N")
						.set(LAUDO.COLUNA_LOMBAR_L4, laudo.isColunaLombarL4() ? "S" : "N")
						.set(LAUDO.COLUNA_LOMBAR_DENSIDADE, laudo.getColunaLombarDensidade())
						.set(LAUDO.COLUNA_LOMBAR_TSCORE, laudo.getColunaLombarTScore())
						.set(LAUDO.COLUNA_LOMBAR_ZSCORE, laudo.getColunaLombarZScore())
						.set(LAUDO.COLO_FEMUR_DENSIDADE, laudo.getColoFemurDensidade())
						.set(LAUDO.COLO_FEMUR_TSCORE, laudo.getColoFemurTScore())
						.set(LAUDO.COLO_FEMUR_ZSCORE, laudo.getColoFemurZScore())
						.set(LAUDO.FEMUR_TOTAL_DENSIDADE, laudo.getFemurTotalDensidade())
						.set(LAUDO.FEMUR_TOTAL_TSCORE, laudo.getFemurTotalTScore())
						.set(LAUDO.FEMUR_TOTAL_ZSCORE, laudo.getFemurTotalZScore())
						.set(LAUDO.RADIO_TERCO_DENSIDADE, laudo.getRadioTercoDensidade())
						.set(LAUDO.RADIO_TERCO_TSCORE, laudo.getRadioTercoTScore())
						.set(LAUDO.RADIO_TERCO_ZSCORE, laudo.getRadioTercoZScore())
						.set(LAUDO.CORPO_INTEIRO_DENSIDADE, laudo.getCorpoInteiroDensidade())
						.set(LAUDO.CORPO_INTEIRO_ZSCORE, laudo.getCorpoInteiroZScore())
						.set(LAUDO.CONCLUSAO, conclusao.name())
						.where(LAUDO.ID.eq(id))
						.execute();
				
				database.delete(LAUDO_OBSERVACAO)
						.where(LAUDO_OBSERVACAO.LAUDO_ID.eq(laudo.getId()))
						.execute();
				
				for (Observacao observacao : laudo.getObservacoes()) {
					database.insertInto(
							LAUDO_OBSERVACAO, 
							LAUDO_OBSERVACAO.LAUDO_ID,
							LAUDO_OBSERVACAO.CODIGO,
							LAUDO_OBSERVACAO.DESCRICAO,
							LAUDO_OBSERVACAO.EXTRA
						)
						.values(
								laudo.getId(),
								observacao.getCodigo(),
								observacao.getDescricao(),
								observacao.getExtra()
						)
						.execute();
				}
				
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
				database.delete(LAUDO_OBSERVACAO)
						.where(LAUDO_OBSERVACAO.LAUDO_ID.eq(id))
						.execute();
				
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
		laudo.setDataNascimento(record.getValue(PESSOA.DATA_NASCIMENTO));
		
		final String sexo = record.getValue(PESSOA.SEXO);
		laudo.setSexo(sexo != null ? Sexo.valueOf(sexo).getValue() : null);
		
		laudo.setColunaLombarL1(record.getValue(LAUDO.COLUNA_LOMBAR_L1).equals("S"));
		laudo.setColunaLombarL2(record.getValue(LAUDO.COLUNA_LOMBAR_L2).equals("S"));
		laudo.setColunaLombarL3(record.getValue(LAUDO.COLUNA_LOMBAR_L3).equals("S"));
		laudo.setColunaLombarL4(record.getValue(LAUDO.COLUNA_LOMBAR_L4).equals("S"));
		
		laudo.setColunaLombarDensidade(record.getValue(LAUDO.COLUNA_LOMBAR_DENSIDADE));
		laudo.setColunaLombarTScore(record.getValue(LAUDO.COLUNA_LOMBAR_TSCORE));
		laudo.setColunaLombarZScore(record.getValue(LAUDO.COLUNA_LOMBAR_ZSCORE));
		
		laudo.setColoFemurDensidade(record.getValue(LAUDO.COLO_FEMUR_DENSIDADE));
		laudo.setColoFemurTScore(record.getValue(LAUDO.COLO_FEMUR_TSCORE));
		laudo.setColoFemurZScore(record.getValue(LAUDO.COLO_FEMUR_ZSCORE));
		
		laudo.setFemurTotalDensidade(record.getValue(LAUDO.FEMUR_TOTAL_DENSIDADE));
		laudo.setFemurTotalTScore(record.getValue(LAUDO.FEMUR_TOTAL_TSCORE));
		laudo.setFemurTotalZScore(record.getValue(LAUDO.FEMUR_TOTAL_ZSCORE));
		
		laudo.setRadioTercoDensidade(record.getValue(LAUDO.RADIO_TERCO_DENSIDADE));
		laudo.setRadioTercoTScore(record.getValue(LAUDO.RADIO_TERCO_TSCORE));
		laudo.setRadioTercoZScore(record.getValue(LAUDO.RADIO_TERCO_ZSCORE));
		
		laudo.setCorpoInteiroDensidade(record.getValue(LAUDO.CORPO_INTEIRO_DENSIDADE));
		laudo.setCorpoInteiroZScore(record.getValue(LAUDO.CORPO_INTEIRO_ZSCORE));
		
		laudo.setConclusao(ConclusaoLaudo.valueOf(record.getValue(LAUDO.CONCLUSAO)).getValue());
		
		final Result<LaudoObservacaoRecord> observacoesRecord = database.selectFrom(LAUDO_OBSERVACAO)
				.where(LAUDO_OBSERVACAO.LAUDO_ID.eq(laudo.getId()))
				.orderBy(LAUDO_OBSERVACAO.CODIGO)
				.fetch();
		for (LaudoObservacaoRecord observacaoRecord : observacoesRecord) {
			final Observacao observacao = new Observacao();
			observacao.setCodigo(observacaoRecord.getCodigo());
			observacao.setDescricao(observacaoRecord.getDescricao());
			observacao.setExtra(observacaoRecord.getExtra());
			laudo.getObservacoes().add(observacao);
		}
		
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
	
	private void createPessoa(Executor database, Pessoa pessoa, Date dataNascimento, Sexo sexo) {
		final PessoaRecord pessoaRecord = database.insertInto(
				PESSOA, 
				PESSOA.NOME,
				PESSOA.PREFIXO,
				PESSOA.CODIGO,
				PESSOA.DATA_NASCIMENTO,
				PESSOA.SEXO
			)
			.values(
					trimToNull(pessoa.getNome()),
					pessoa.getPrefixo(),
					pessoa.getCodigoNumerico(),
					dataNascimento != null ? new Date(dataNascimento.getTime()) : null,
					sexo != null ? sexo.name() : null
				)
				.returning(PESSOA.ID)
				.fetchOne();
		
		pessoa.setId(pessoaRecord.getId());
	}
	
	private void updatePessoa(Executor database, Pessoa pessoa, Date dataNascimento, Sexo sexo) {
		database.update(PESSOA)
				.set(PESSOA.DATA_NASCIMENTO, dataNascimento)
				.set(PESSOA.SEXO, sexo.name())
				.where(PESSOA.ID.eq(pessoa.getId()))
				.execute();
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
		private Date dataNascimento;
		private String sexo;
		private boolean colunaLombarL1;
		private boolean colunaLombarL2;
		private boolean colunaLombarL3;
		private boolean colunaLombarL4;
		private BigDecimal colunaLombarDensidade;
		private BigDecimal colunaLombarTScore;
		private BigDecimal colunaLombarZScore;
		private BigDecimal coloFemurDensidade;
		private BigDecimal coloFemurTScore;
		private BigDecimal coloFemurZScore;
		private BigDecimal femurTotalDensidade;
		private BigDecimal femurTotalTScore;
		private BigDecimal femurTotalZScore;
		private BigDecimal radioTercoDensidade;
		private BigDecimal radioTercoTScore;
		private BigDecimal radioTercoZScore;
		private BigDecimal corpoInteiroDensidade;
		private BigDecimal corpoInteiroZScore;
		private String conclusao;
		private List<Observacao> observacoes = new ArrayList<>();
	}
	
	@Data
	private static class Observacao {
		private Integer codigo;
		private String descricao;
		private String extra;
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
