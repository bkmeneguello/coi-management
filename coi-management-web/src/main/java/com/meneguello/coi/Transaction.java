package com.meneguello.coi;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;


public abstract class Transaction<T> {
	
	private boolean atomic;
	
	public Transaction() {
		this(false);
	}

	public Transaction(boolean atomic) {
		this.atomic = atomic;
	}
	
	protected abstract T execute(DSLContext database);
	
	public T execute() throws DataAccessException {
		try(Connection connection = openConnection()) {
			try {
				connection.setAutoCommit(!atomic);
				final T result = execute(DSL.using(connection, SQLDialect.HSQLDB));
				connection.setAutoCommit(true);
				return result;
			} catch(Exception e) {
				connection.rollback();
				throw e;
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	private Connection openConnection() throws SQLException {
		try {
			DataSource ds = (DataSource) new InitialContext().lookup("java:/comp/env/jdbc/dataSource");
			return ds.getConnection();
		} catch(NamingException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

}
