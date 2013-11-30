package com.meneguello.coi;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {
	
	public Response toResponse(Throwable e) {
		e.printStackTrace();
		return Response.status(Response.Status.NOT_FOUND).entity(e.getClass().getName() + " " + e.getLocalizedMessage()).build();
	}
	
}