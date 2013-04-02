package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Versao.VERSAO;
import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;
import static java.lang.Boolean.TRUE;
import static org.jooq.impl.Factory.max;
import static org.jooq.util.hsqldb.information_schema.Tables.TABLES;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;

import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.impl.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.core.PackagesResourceConfig;

@WebListener
@ApplicationPath("/rest")
public class CoiApplication extends PackagesResourceConfig implements ServletContextListener {
	
	private final class RecuperaVersaoSistema extends Transaction<Integer> {
		@Override
		protected Integer execute(Executor database) {
			final Record1<Integer> recordVersao = database.select(max(VERSAO.MAIOR))
					.from(VERSAO)
					.fetchOne();
			return (Integer) recordVersao.getValue("max");
		}
	}

	private final class ChecaTabelaVersao extends Transaction<Boolean> {
		@Override
		protected Boolean execute(Executor database) {
			final Record recordTables = database.selectFrom(TABLES).where(TABLES.TABLE_NAME.eq("VERSAO")).fetchOne();
			return recordTables != null;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(CoiApplication.class);

	public CoiApplication() {
		super("com.meneguello.coi");
		getFeatures().put(FEATURE_POJO_MAPPING, TRUE);
		logger.info("Coi Application init");
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		logger.info("Coi Application start");
		Integer versao = obtemVersaoInstalada();
	}

	private Integer obtemVersaoInstalada() {
		final boolean schemaInstalled = new ChecaTabelaVersao().execute();
		if (schemaInstalled) {
			final Integer versao = new RecuperaVersaoSistema().execute();
			logger.info("Versão atual: {}", versao);
			return versao;
		} else {
			logger.error("Sistema não instalado");
			return 0;
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}
	
}