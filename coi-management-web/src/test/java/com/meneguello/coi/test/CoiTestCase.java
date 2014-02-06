package com.meneguello.coi.test;

import static org.dbunit.database.DatabaseConfig.PROPERTY_DATATYPE_FACTORY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.dbunit.DefaultOperationListener;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public abstract class CoiTestCase {

	protected WebDriver driver;
	
	protected IDatabaseTester databaseTester;

	@Before
	public void setUp() throws Exception {
		databaseTester = new JdbcDatabaseTester("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost/coi", "sa", "");
		databaseTester.setDataSet(createDataSet());
		databaseTester.setTearDownOperation(DatabaseOperation.DELETE_ALL);
		databaseTester.setOperationListener(new DefaultOperationListener() {
			@Override
			public void connectionRetrieved(IDatabaseConnection connection) {
				super.connectionRetrieved(connection);
				connection.getConfig().setProperty(PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
			}
		});
		databaseTester.onSetup();
		
		driver = new ChromeDriver();
		driver.get("http://admin:admin@localhost:8080/coi-management-web/");
    }
	
	protected IDataSet createDataSet() throws Exception {
		final IDatabaseConnection connection = databaseTester.getConnection();
		return connection.createDataSet();
	}

	@After
	public void tearDown() throws Exception {
		if (databaseTester != null)
			databaseTester.onTearDown();
		
		if (driver != null)
			driver.quit();
	}

	protected String path(String name) throws IOException, FileNotFoundException {
		return file(name).getAbsolutePath();
	}

	protected File file(String name) throws IOException, FileNotFoundException {
		File file = File.createTempFile("coi", null);
		IOUtils.copy(getClass().getResourceAsStream(name), new FileOutputStream(file));
		return file;
	}

	protected InputStream stream(String resource) {
		return getClass().getResourceAsStream(resource);
	}
	
}
