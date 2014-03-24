package com.meneguello.coi;

import static com.meneguello.coi.Utils.asSQLDate;
import static com.meneguello.coi.model.tables.Laudo.LAUDO;
import static com.meneguello.coi.model.tables.LaudoComparacao.LAUDO_COMPARACAO;
import static com.meneguello.coi.model.tables.LaudoComparacaoOpcao.LAUDO_COMPARACAO_OPCAO;
import static com.meneguello.coi.model.tables.LaudoComparacaoValue.LAUDO_COMPARACAO_VALUE;
import static com.meneguello.coi.model.tables.LaudoObservacao.LAUDO_OBSERVACAO;
import static com.meneguello.coi.model.tables.LaudoObservacaoOpcao.LAUDO_OBSERVACAO_OPCAO;
import static com.meneguello.coi.model.tables.LaudoObservacaoValue.LAUDO_OBSERVACAO_VALUE;
import static com.meneguello.coi.model.tables.Pessoa.PESSOA;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;

import com.meneguello.coi.model.Keys;
import com.meneguello.coi.model.tables.records.LaudoComparacaoOpcaoRecord;
import com.meneguello.coi.model.tables.records.LaudoComparacaoRecord;
import com.meneguello.coi.model.tables.records.LaudoComparacaoValueRecord;
import com.meneguello.coi.model.tables.records.LaudoObservacaoOpcaoRecord;
import com.meneguello.coi.model.tables.records.LaudoObservacaoRecord;
import com.meneguello.coi.model.tables.records.LaudoObservacaoValueRecord;
import com.meneguello.coi.model.tables.records.LaudoRecord;
import com.meneguello.coi.model.tables.records.PessoaRecord;
 
@Path("/laudos")
public class LaudoEndpoint {
	
	@GET
	@Path("/observacoes")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ObservacaoList> listObservacoes() throws Exception {
		return new Transaction<List<ObservacaoList>>() {
			@Override
			protected List<ObservacaoList> execute(DSLContext database) {
				final List<ObservacaoList> result =  new ArrayList<>();
				final Result<LaudoObservacaoOpcaoRecord> opcaoResult = database.selectFrom(LAUDO_OBSERVACAO_OPCAO)
						.orderBy(LAUDO_OBSERVACAO_OPCAO.CODIGO)
						.fetch();
				for (LaudoObservacaoOpcaoRecord opcaoRecord : opcaoResult) {
					final ObservacaoList observacaoList = new ObservacaoList();
					observacaoList.setCodigo(opcaoRecord.getValue(LAUDO_OBSERVACAO_OPCAO.CODIGO));
					observacaoList.setDescricao(opcaoRecord.getValue(LAUDO_OBSERVACAO_OPCAO.DESCRICAO));
					observacaoList.setRotulo(opcaoRecord.getValue(LAUDO_OBSERVACAO_OPCAO.ROTULO));
					result.add(observacaoList);
				}
				return result;
			}
		}.execute();
	}
	
	@GET
	@Path("/comparacoes")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ComparacaoList> listComparacoes() throws Exception {
		return new Transaction<List<ComparacaoList>>() {
			@Override
			protected List<ComparacaoList> execute(DSLContext database) {
				final List<ComparacaoList> result =  new ArrayList<>();
				final Result<LaudoComparacaoOpcaoRecord> opcaoResult = database.selectFrom(LAUDO_COMPARACAO_OPCAO)
						.orderBy(LAUDO_COMPARACAO_OPCAO.CODIGO)
						.fetch();
				for (LaudoComparacaoOpcaoRecord opcaoRecord : opcaoResult) {
					final ComparacaoList comparacaoList = new ComparacaoList();
					comparacaoList.setCodigo(opcaoRecord.getValue(LAUDO_COMPARACAO_OPCAO.CODIGO));
					comparacaoList.setDescricao(opcaoRecord.getValue(LAUDO_COMPARACAO_OPCAO.DESCRICAO));
					comparacaoList.setRotulo(opcaoRecord.getValue(LAUDO_COMPARACAO_OPCAO.ROTULO));
					result.add(comparacaoList);
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
	
	@GET
	@Path("/print/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response print(final @PathParam("id") Long id) throws Exception {
		class LaudoPair {
			public LaudoPair(String name, ByteArrayOutputStream stream) {
				this.name = name;
				this.stream = stream;
			}
			public String name;
			public ByteArrayOutputStream stream;
		}
		
		LaudoPair laudo = new FallibleTransaction<LaudoPair>() {
			@Override
			protected LaudoPair executeFallible(DSLContext database) throws Exception {
				final LaudoRecord laudoRecord = database.fetchOne(LAUDO, LAUDO.ID.eq(id));
				final PessoaRecord pacienteRecord = database.fetchOne(PESSOA, PESSOA.ID.eq(laudoRecord.getPacienteId()));
				final PessoaRecord medicoRecord = database.fetchOne(PESSOA, PESSOA.ID.eq(laudoRecord.getMedicoId()));
				
				final StatusHormonal statusHormonal = StatusHormonal.valueOf(laudoRecord.getStatusHormonal());

				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				final boolean l1 = "S".equals(laudoRecord.getColunaLombarL1());
				final boolean l2 = "S".equals(laudoRecord.getColunaLombarL2());
				final boolean l3 = "S".equals(laudoRecord.getColunaLombarL3());
				final boolean l4 = "S".equals(laudoRecord.getColunaLombarL4());
				final String vertebras = vertebras(l1, l2, l3, l4);
				
				final Map<String, Object> laudoParameters = new HashMap<>();
				laudoParameters.put("paciente", pacienteRecord.getNome());
				laudoParameters.put("medico", medicoRecord.getNome());
				laudoParameters.put("sexo", Sexo.valueOf(pacienteRecord.getSexo()).getValue());
				laudoParameters.put("idade", Years.yearsBetween(new DateTime(pacienteRecord.getDataNascimento().getTime()), DateTime.now()).getYears());
				laudoParameters.put("status", statusHormonal.getValue());
				laudoParameters.put("data", new Date(DateTime.now().getMillis()));
				laudoParameters.put("vertebras", vertebras);
				laudoParameters.put("colunaLombarDensidade", laudoRecord.getColunaLombarDensidade());
				laudoParameters.put("coloFemurDensidade", laudoRecord.getColoFemurDensidade());
				laudoParameters.put("femurTotalDensidade", laudoRecord.getFemurTotalDensidade());
				laudoParameters.put("radioTercoDensidade", laudoRecord.getRadioTercoDensidade());
				laudoParameters.put("colunaLombarZScore", laudoRecord.getColunaLombarZscore());
				laudoParameters.put("coloFemurZScore", laudoRecord.getColoFemurZscore());
				laudoParameters.put("femurTotalZScore", laudoRecord.getFemurTotalZscore());
				laudoParameters.put("radioTercoZScore", laudoRecord.getRadioTercoZscore());
				laudoParameters.put("conclusao", ConclusaoLaudo.valueOf(laudoRecord.getConclusao()).getValue().toUpperCase());
				laudoParameters.put("observacoes", observacoes(database, laudoRecord.getId()));
				laudoParameters.put("comparacoes", comparacoes(database, laudoRecord.getId()));
				
				String laudoNome = null;
				switch (statusHormonal) {
				case PRE_MENOPAUSAL:
					laudoParameters.put("corpoInteiroDensidade", laudoRecord.getCorpoInteiroDensidade());
					laudoParameters.put("corpoInteiroZScore", laudoRecord.getCorpoInteiroZscore());
					laudoNome = "laudo-pre.jrxml";
					break;
				case TRANSICAO_MENOPAUSAL:
				case POS_MENOPAUSAL:
					laudoParameters.put("colunaLombarTScore", laudoRecord.getColunaLombarTscore());
					laudoParameters.put("coloFemurTScore", laudoRecord.getColoFemurTscore());
					laudoParameters.put("femurTotalTScore", laudoRecord.getFemurTotalTscore());
					laudoParameters.put("radioTercoTScore", laudoRecord.getRadioTercoTscore());
					laudoParameters.put("colunaLombarRisco", risco(laudoRecord.getColunaLombarTscore()));
					laudoParameters.put("femurRisco", risco(laudoRecord.getColoFemurTscore().max(laudoRecord.getFemurTotalTscore())));
					laudoParameters.put("radioTercoRisco", risco(laudoRecord.getRadioTercoTscore()));
					laudoNome = "laudo-pos.jrxml";
					break;
				}
				
				final JasperReport jasperReportLaudo = JasperCompileManager.compileReport(getClass().getClassLoader().getResourceAsStream(laudoNome));
				final JasperPrint jasperPrintLaudo = JasperFillManager.fillReport(jasperReportLaudo, laudoParameters, new JREmptyDataSource());
				
				final JRPdfExporter exporter = new JRPdfExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrintLaudo);
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
				exporter.exportReport();
				
				final String name = String.format("%s-%s-%tF", pacienteRecord.getValue(PESSOA.NOME), pacienteRecord.getValue(PESSOA.CODIGO), laudoRecord.getValue(LAUDO.DATA));
				return new LaudoPair(name, baos);
			}
			
			private String vertebras(boolean l1, boolean l2, boolean l3, boolean l4) {
				if (l1 && l2 && l3 && l4) return "(L1-L4)";
				if (l1 && !l2 && !l3 && l4) return "(L1-L4)-(L2-L3)";
				if (l1 && l2 && !l3 && l4) return "(L1-L4)-L3";
				if (l1 && !l2 && l3 && l4) return "(L1-L4)-L2";
				if (l1 && l2 && l3 && !l4) return "(L1-L3)";
				if (l1 && !l2 && l3 && !l4) return "(L1-L3)-L2";
				if (l1 && l2 && !l3 && !l4) return "(L1-L2)";
				if (l1 && !l2 && !l3 && !l4) return "(L1)";
				
				if (!l1 && l2 && l3 && l4) return "(L2-L4)";
				if (!l1 && l2 && !l3 && l4) return "(L2-L4)-L3";
				if (!l1 && l2 && l3 && !l4) return "(L2-L3)";
				if (!l1 && l2 && !l3 && !l4) return "(L2)";
				
				if (!l1 && !l2 && l3 && l4) return "(L3-L4)";
				if (!l1 && !l2 && l3 && !l4) return "(L3)";
				
				if (!l1 && !l2 && !l3 && l4) return "(L4)";
				
				return (l1 ? "L1 " : "") + (l2 ? "L2 " : "") + (l3 ? "L3 " : "") + (l4 ? "L4 " : "");
			}

			private BigDecimal risco(BigDecimal tscore) {
				if (tscore.compareTo(new BigDecimal(-1)) < 0) {
					return new BigDecimal(String.valueOf(Math.pow(2, tscore.abs().doubleValue())));
				}
				return null;
			}

			private Collection<Map<String, Object>> observacoes(DSLContext database, Long laudoId) {
				final Result<Record> observacoesResult = database
						.selectFrom(LAUDO_OBSERVACAO.join(LAUDO_OBSERVACAO_OPCAO).onKey())
						.where(LAUDO_OBSERVACAO.LAUDO_ID.eq(laudoId))
						.orderBy(LAUDO_OBSERVACAO_OPCAO.CODIGO)
						.fetch();
				
				final List<Map<String, Object>> observacoes = new ArrayList<>();
				for (Record observacaoRecord : observacoesResult) {
					final Map<String, Object> observacao = new HashMap<>();
					observacao.put("codigo", observacaoRecord.getValue(LAUDO_OBSERVACAO_OPCAO.CODIGO));
					String descricao = observacaoRecord.getValue(LAUDO_OBSERVACAO_OPCAO.DESCRICAO);
					final Result<LaudoObservacaoValueRecord> valueResult = database
							.selectFrom(LAUDO_OBSERVACAO_VALUE)
							.where(LAUDO_OBSERVACAO_VALUE.LAUDO_OBSERVACAO_ID.eq(observacaoRecord.getValue(LAUDO_OBSERVACAO.ID)))
							.fetch();
					for (LaudoObservacaoValueRecord valueRecord : valueResult) {
						descricao = descricao.replace("{"+ valueRecord.getValue(LAUDO_OBSERVACAO_VALUE.NOME) +"}", trimToEmpty(valueRecord.getValue(LAUDO_OBSERVACAO_VALUE.VALOR)));
					}
					observacao.put("descricao", descricao);
					observacoes.add(observacao);
				}
				return observacoes;
			}
			
			private Collection<Map<String, Object>> comparacoes(DSLContext database, Long laudoId) {
				final Result<Record> comparacoesResult = database
						.selectFrom(LAUDO_COMPARACAO.join(LAUDO_COMPARACAO_OPCAO).onKey())
						.where(LAUDO_COMPARACAO.LAUDO_ID.eq(laudoId))
						.orderBy(LAUDO_COMPARACAO_OPCAO.CODIGO)
						.fetch();
				
				final List<Map<String, Object>> comparacoes = new ArrayList<>();
				for (Record comparacaoRecord : comparacoesResult) {
					final Map<String, Object> comparacao = new HashMap<>();
					comparacao.put("codigo", comparacaoRecord.getValue(LAUDO_COMPARACAO_OPCAO.CODIGO));
					String descricao = comparacaoRecord.getValue(LAUDO_COMPARACAO_OPCAO.DESCRICAO);
					final Result<LaudoComparacaoValueRecord> valueResult = database
							.selectFrom(LAUDO_COMPARACAO_VALUE)
							.where(LAUDO_COMPARACAO_VALUE.LAUDO_COMPARACAO_ID.eq(comparacaoRecord.getValue(LAUDO_COMPARACAO.ID)))
							.fetch();
					for (LaudoComparacaoValueRecord valueRecord : valueResult) {
						descricao = descricao.replace("{"+ valueRecord.getValue(LAUDO_COMPARACAO_VALUE.NOME) +"}", trimToEmpty(valueRecord.getValue(LAUDO_COMPARACAO_VALUE.VALOR)));
					}
					comparacao.put("descricao", descricao);
					comparacoes.add(comparacao);
				}
				return comparacoes;
			}
		}.execute();
		
		return Response.ok(laudo.stream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename="+ laudo.name +".pdf").build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<LaudoList> list(final @QueryParam("page") Integer page) throws Exception {
		return new Transaction<List<LaudoList>>() {
			@Override
			protected List<LaudoList> execute(DSLContext database) {
				final ArrayList<LaudoList> result = new ArrayList<LaudoList>();
				final Result<Record> resultRecord = database.selectFrom(LAUDO
							.join(PESSOA).onKey(Keys.LAUDO_FK_PACIENTE)
						)
						.limit(10).offset(10 * (page != null ? page : 0))
						.fetch();
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
			protected Laudo execute(DSLContext database) {
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
			public Laudo execute(DSLContext database) {
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
								asSQLDate(laudo.getData()),
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
				
				insertObservacoes(database, laudo);
				insertComparacoes(database, laudo);
				
				return laudo;
			}
		}.execute();
	}

	protected void insertComparacoes(DSLContext database, Laudo laudo) {
		for (Comparacao comparacao : laudo.getComparacoes()) {
			final Long opcaoId = database.select(LAUDO_COMPARACAO_OPCAO.ID)
					.from(LAUDO_COMPARACAO_OPCAO)
					.where(LAUDO_COMPARACAO_OPCAO.CODIGO.eq(comparacao.getCodigo()))
					.fetchOne(LAUDO_COMPARACAO_OPCAO.ID);
			final LaudoComparacaoRecord comparacaoRecord = database.insertInto(
					LAUDO_COMPARACAO, 
					LAUDO_COMPARACAO.LAUDO_ID,
					LAUDO_COMPARACAO.LAUDO_COMPARACAO_OPCAO_ID
				)
				.values(
						laudo.getId(),
						opcaoId
				)
				.returning(LAUDO_COMPARACAO.ID)
				.fetchOne();
			for (Valor valor : comparacao.getValores()) {
				database.insertInto(
						LAUDO_COMPARACAO_VALUE,
						LAUDO_COMPARACAO_VALUE.LAUDO_COMPARACAO_ID,
						LAUDO_COMPARACAO_VALUE.NOME,
						LAUDO_COMPARACAO_VALUE.VALOR
					)
					.values(
							comparacaoRecord.getValue(LAUDO_COMPARACAO.ID),
							valor.getNome(),
							valor.getValor()
					)
					.execute();
			}
		}
	}

	protected void insertObservacoes(DSLContext database, Laudo laudo) {
		for (Observacao observacao : laudo.getObservacoes()) {
			final Long opcaoId = database.select(LAUDO_OBSERVACAO_OPCAO.ID)
					.from(LAUDO_OBSERVACAO_OPCAO)
					.where(LAUDO_OBSERVACAO_OPCAO.CODIGO.eq(observacao.getCodigo()))
					.fetchOne(LAUDO_OBSERVACAO_OPCAO.ID);
			final LaudoObservacaoRecord observacaoRecord = database.insertInto(
					LAUDO_OBSERVACAO, 
					LAUDO_OBSERVACAO.LAUDO_ID,
					LAUDO_OBSERVACAO.LAUDO_OBSERVACAO_OPCAO_ID
				)
				.values(
						laudo.getId(),
						opcaoId
				)
				.returning(LAUDO_OBSERVACAO.ID)
				.fetchOne();
			for (Valor valor : observacao.getValores()) {
				database.insertInto(
						LAUDO_OBSERVACAO_VALUE,
						LAUDO_OBSERVACAO_VALUE.LAUDO_OBSERVACAO_ID,
						LAUDO_OBSERVACAO_VALUE.NOME,
						LAUDO_OBSERVACAO_VALUE.VALOR
					)
					.values(
							observacaoRecord.getValue(LAUDO_OBSERVACAO.ID),
							valor.getNome(),
							valor.getValor()
					)
					.execute();
			}
		}
	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Laudo update(final @PathParam("id") Long id, final Laudo laudo) throws Exception {
		return new Transaction<Laudo>(true) {
			@Override
			public Laudo execute(DSLContext database) {
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
						.set(LAUDO.DATA, asSQLDate(laudo.getData()))
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
				
				deleteObservacoes(database, laudo.getId());
				insertObservacoes(database, laudo);
				
				deleteComparacoes(database, laudo.getId());
				insertComparacoes(database, laudo);
				
				return laudo;
			}
		}.execute();
	}

	protected void deleteComparacoes(DSLContext database, Long laudoId) {
		for (LaudoComparacaoRecord record : database.selectFrom(LAUDO_COMPARACAO).where(LAUDO_COMPARACAO.LAUDO_ID.eq(laudoId)).fetch()) {
			database.delete(LAUDO_COMPARACAO_VALUE)
					.where(LAUDO_COMPARACAO_VALUE.LAUDO_COMPARACAO_ID.eq(record.getValue(LAUDO_COMPARACAO.ID)))
					.execute();
		}
		
		database.delete(LAUDO_COMPARACAO)
				.where(LAUDO_COMPARACAO.LAUDO_ID.eq(laudoId))
				.execute();
	}

	protected void deleteObservacoes(DSLContext database, Long laudoId) {
		for (LaudoObservacaoRecord record : database.selectFrom(LAUDO_OBSERVACAO).where(LAUDO_OBSERVACAO.LAUDO_ID.eq(laudoId)).fetch()) {
			database.delete(LAUDO_OBSERVACAO_VALUE)
					.where(LAUDO_OBSERVACAO_VALUE.LAUDO_OBSERVACAO_ID.eq(record.getValue(LAUDO_OBSERVACAO.ID)))
					.execute();
		}
		
		database.delete(LAUDO_OBSERVACAO)
				.where(LAUDO_OBSERVACAO.LAUDO_ID.eq(laudoId))
				.execute();
	}

	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(DSLContext database) {
				deleteObservacoes(database, id);
				deleteComparacoes(database, id);
				
				database.delete(LAUDO)
						.where(LAUDO.ID.eq(id))
						.execute();
				
				return null;
			}
		}.execute();
	}

	private LaudoList buildLaudoList(DSLContext database, Record record) {
		final LaudoList laudo = new LaudoList();
		laudo.setId(record.getValue(LAUDO.ID));
		laudo.setData(record.getValue(LAUDO.DATA));
		laudo.setPaciente(record.getValue(PESSOA.NOME));
		laudo.setStatus(StatusHormonal.valueOf(record.getValue(LAUDO.STATUS_HORMONAL)).getValue());
		return laudo;
	}
	
	private Laudo buildLaudo(DSLContext database, Record record) {
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
		
		final Result<Record> observacoesRecord = database
				.selectFrom(LAUDO_OBSERVACAO.join(LAUDO_OBSERVACAO_OPCAO).onKey())
				.where(LAUDO_OBSERVACAO.LAUDO_ID.eq(laudo.getId()))
				.orderBy(LAUDO_OBSERVACAO_OPCAO.CODIGO)
				.fetch();
		for (Record observacaoRecord : observacoesRecord) {
			final Observacao observacao = new Observacao();
			observacao.setCodigo(observacaoRecord.getValue(LAUDO_OBSERVACAO_OPCAO.CODIGO));
			observacao.setDescricao(observacaoRecord.getValue(LAUDO_OBSERVACAO_OPCAO.DESCRICAO));
			final Result<LaudoObservacaoValueRecord> valorResult = database.selectFrom(LAUDO_OBSERVACAO_VALUE)
					.where(LAUDO_OBSERVACAO_VALUE.LAUDO_OBSERVACAO_ID.eq(observacaoRecord.getValue(LAUDO_OBSERVACAO.ID)))
					.fetch();
			for (LaudoObservacaoValueRecord valorRecord : valorResult) {
				Valor valor = new Valor();
				valor.setNome(valorRecord.getValue(LAUDO_OBSERVACAO_VALUE.NOME));
				valor.setValor(valorRecord.getValue(LAUDO_OBSERVACAO_VALUE.VALOR));
				observacao.getValores().add(valor);
			}
			laudo.getObservacoes().add(observacao);
		}
		
		final Result<Record> comparacoesRecord = database
				.selectFrom(LAUDO_COMPARACAO.join(LAUDO_COMPARACAO_OPCAO).onKey())
				.where(LAUDO_COMPARACAO.LAUDO_ID.eq(laudo.getId()))
				.orderBy(LAUDO_COMPARACAO_OPCAO.CODIGO)
				.fetch();
		for (Record comparacaoRecord : comparacoesRecord) {
			final Comparacao comparacao = new Comparacao();
			comparacao.setCodigo(comparacaoRecord.getValue(LAUDO_COMPARACAO_OPCAO.CODIGO));
			comparacao.setDescricao(comparacaoRecord.getValue(LAUDO_COMPARACAO_OPCAO.DESCRICAO));
			final Result<LaudoComparacaoValueRecord> valorResult = database.selectFrom(LAUDO_COMPARACAO_VALUE)
					.where(LAUDO_COMPARACAO_VALUE.LAUDO_COMPARACAO_ID.eq(comparacaoRecord.getValue(LAUDO_COMPARACAO.ID)))
					.fetch();
			for (LaudoComparacaoValueRecord valorRecord : valorResult) {
				Valor valor = new Valor();
				valor.setNome(valorRecord.getValue(LAUDO_COMPARACAO_VALUE.NOME));
				valor.setValor(valorRecord.getValue(LAUDO_COMPARACAO_VALUE.VALOR));
				comparacao.getValores().add(valor);
			}
			laudo.getComparacoes().add(comparacao);
		}
		
		return laudo;
	}
	
	@GET
	@Path("/anterior/{codigoPaciente:[a-zA-Z]-\\d+}")
	@Produces(MediaType.APPLICATION_JSON)
	public LaudoAnterior getLaudoAnterior(final @PathParam("codigoPaciente") String codigoPaciente) {
		return new FallibleTransaction<LaudoAnterior>() {
			@Override
			protected LaudoAnterior executeFallible(DSLContext database) {
				String[] split = codigoPaciente.split("-");
				String prefixo = split[0];
				int codigo = Integer.parseInt(split[1]);
				final Record record = database
						.selectFrom(LAUDO.join(PESSOA).onKey(Keys.LAUDO_FK_PACIENTE))
						.where(PESSOA.PREFIXO.eq(prefixo))
						.and(PESSOA.CODIGO.eq(codigo))
						.orderBy(LAUDO.DATA.desc())
						.limit(1)
						.fetchOne();
				if (record == null) throw new WebApplicationException(Status.NOT_FOUND);
				
				LaudoAnterior laudo = buildLaudoAnterior(database, record);
				laudo.setId(record.getValue(PESSOA.PREFIXO) + "-" + record.getValue(PESSOA.CODIGO));
				return laudo;
			}
		}.execute();
	}
	
	@GET
	@Path("/anterior/{id:\\d+}")
	@Produces(MediaType.APPLICATION_JSON)
	public LaudoAnterior getLaudoAnterior(final @PathParam("id") Long id) {
		return new FallibleTransaction<LaudoAnterior>() {
			@Override
			protected LaudoAnterior executeFallible(DSLContext database) {
				final Record recordAtual = database
						.selectFrom(LAUDO.join(PESSOA).onKey(Keys.LAUDO_FK_PACIENTE))
						.where(LAUDO.ID.eq(id))
						.fetchOne();
				
				final Record record = database
						.selectFrom(LAUDO.join(PESSOA).onKey(Keys.LAUDO_FK_PACIENTE))
						.where(PESSOA.PREFIXO.eq(recordAtual.getValue(PESSOA.PREFIXO)))
						.and(PESSOA.CODIGO.eq(recordAtual.getValue(PESSOA.CODIGO)))
						.and(LAUDO.DATA.lt(recordAtual.getValue(LAUDO.DATA)))
						.orderBy(LAUDO.DATA.desc())
						.limit(1)
						.fetchOne();
				if (record == null) throw new WebApplicationException(Status.NOT_FOUND);
				
				LaudoAnterior laudo = buildLaudoAnterior(database, record);
				laudo.setId(id.toString());
				return laudo;
			}
		}.execute();
	}
	
	private LaudoAnterior buildLaudoAnterior(DSLContext database, Record record) {
		final LaudoAnterior laudo = new LaudoAnterior();
		laudo.setDataAnt(record.getValue(LAUDO.DATA));
		
		laudo.setColunaLombarL1Ant(record.getValue(LAUDO.COLUNA_LOMBAR_L1).equals("S"));
		laudo.setColunaLombarL2Ant(record.getValue(LAUDO.COLUNA_LOMBAR_L2).equals("S"));
		laudo.setColunaLombarL3Ant(record.getValue(LAUDO.COLUNA_LOMBAR_L3).equals("S"));
		laudo.setColunaLombarL4Ant(record.getValue(LAUDO.COLUNA_LOMBAR_L4).equals("S"));
		
		laudo.setColunaLombarDensidadeAnt(record.getValue(LAUDO.COLUNA_LOMBAR_DENSIDADE));
		laudo.setColunaLombarTScoreAnt(record.getValue(LAUDO.COLUNA_LOMBAR_TSCORE));
		laudo.setColunaLombarZScoreAnt(record.getValue(LAUDO.COLUNA_LOMBAR_ZSCORE));
		
		laudo.setFemurTotalDensidadeAnt(record.getValue(LAUDO.FEMUR_TOTAL_DENSIDADE));
		laudo.setFemurTotalTScoreAnt(record.getValue(LAUDO.FEMUR_TOTAL_TSCORE));
		laudo.setFemurTotalZScoreAnt(record.getValue(LAUDO.FEMUR_TOTAL_ZSCORE));
		
		return laudo;
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
	
	private void createPessoa(DSLContext database, Pessoa pessoa, Date dataNascimento, Sexo sexo) {
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
					asSQLDate(dataNascimento),
					sexo != null ? sexo.name() : null
				)
				.returning(PESSOA.ID)
				.fetchOne();
		
		pessoa.setId(pessoaRecord.getId());
	}
	
	private void updatePessoa(DSLContext database, Pessoa pessoa, Date dataNascimento, Sexo sexo) {
		database.update(PESSOA)
				.set(PESSOA.DATA_NASCIMENTO, asSQLDate(dataNascimento))
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
		private List<Comparacao> comparacoes = new ArrayList<>();
	}
	
	@Data
	private static class LaudoAnterior {
		private String id;
		private Date dataAnt;
		private boolean colunaLombarL1Ant;
		private boolean colunaLombarL2Ant;
		private boolean colunaLombarL3Ant;
		private boolean colunaLombarL4Ant;
		private BigDecimal colunaLombarDensidadeAnt;
		private BigDecimal colunaLombarTScoreAnt;
		private BigDecimal colunaLombarZScoreAnt;
		private BigDecimal femurTotalDensidadeAnt;
		private BigDecimal femurTotalTScoreAnt;
		private BigDecimal femurTotalZScoreAnt;
	}
	
	@Data
	private static class Observacao {
		private Integer codigo;
		private String descricao;
		private List<Valor> valores = new ArrayList<>();
	}
	
	@Data
	private static class ObservacaoList {
		private Integer codigo;
		private String descricao;
		private String rotulo;
	}
	
	@Data
	private static class Comparacao {
		private Integer codigo;
		private String descricao;
		private List<Valor> valores = new ArrayList<>();
	}
	
	@Data
	private static class ComparacaoList {
		private Integer codigo;
		private String descricao;
		private String rotulo;
	}
	
	@Data
	private static class Valor {
		private String nome;
		private String valor;
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
