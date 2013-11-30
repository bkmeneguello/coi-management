package com.meneguello.coi;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.api.NotFoundException;

@Provider
public class BadURIExceptionMapper implements ExceptionMapper<NotFoundException> {
	
	public Response toResponse(NotFoundException exception) {
		return Response.status(Response.Status.NOT_FOUND).entity("Erro Interno (404)").build();
	}
	
}