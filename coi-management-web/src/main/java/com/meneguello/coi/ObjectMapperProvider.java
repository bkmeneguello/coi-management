package com.meneguello.coi;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

	private ObjectMapper objectMapper;

	public ObjectMapperProvider() {
		objectMapper = new ObjectMapper();
		//objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
		//objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ"));
	}

	@Override
	public ObjectMapper getContext(Class<?> type) {
		return objectMapper;
	}

}