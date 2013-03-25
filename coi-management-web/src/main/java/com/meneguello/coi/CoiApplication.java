package com.meneguello.coi;

import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;
import static java.lang.Boolean.TRUE;
import javax.ws.rs.ApplicationPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;

@ApplicationPath("/rest")
public class CoiApplication extends PackagesResourceConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(CoiApplication.class);

	public CoiApplication() {
		super("com.meneguello.coi");
		getFeatures().put(FEATURE_POJO_MAPPING, TRUE);
		logger.info("Coi Application start");
	}
	
}