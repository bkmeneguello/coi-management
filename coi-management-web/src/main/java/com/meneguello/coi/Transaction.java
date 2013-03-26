package com.meneguello.coi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.Executor;


public abstract class Transaction<T> {
	
	private boolean atomic;
	
	public Transaction() {
		
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
			loadDriver();
			return DriverManager.getConnection(System.getProperty("database.url"), System.getProperty("database.username"), System.getProperty("database.password"));
		} catch(ClassNotFoundException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	private void loadDriver() throws ClassNotFoundException {
		Class.forName(System.getProperty("database.driver"));
	}

}
