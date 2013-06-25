package com.meneguello.coi;

import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;
import static java.lang.Boolean.TRUE;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.core.PackagesResourceConfig;

@WebListener
@ApplicationPath("/rest")
public class CoiApplication extends PackagesResourceConfig implements ServletContextListener {
	
	private static final Logger logger = LoggerFactory.getLogger(CoiApplication.class);

	public CoiApplication() {
		super("com.meneguello.coi");
		getFeatures().put(FEATURE_POJO_MAPPING, TRUE);
		logger.info("Coi Application init");
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		logger.info("Coi Application start");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}
	
}