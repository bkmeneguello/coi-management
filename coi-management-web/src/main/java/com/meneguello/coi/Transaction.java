package com.meneguello.coi;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.Executor;


public abstract class Transaction<T> {
	
	private boolean atomic;
	
	public Transaction() {
		this(false);
	}

	public Transaction(boolean atomic) {
		this.atomic = atomic;
	}
	
	protected abstract T execute(Executor database);
	
	public T execute() throws DataAccessException {
		Connection connection = null;
		T result = null;
		try {
			connection = openConnection();
			connection.setAutoCommit(!atomic);
			result = execute(new Executor(connection, SQLDialect.HSQLDB));
			connection.setAutoCommit(true);
			return result;
		} catch(SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
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