package ch.fhnw.swc.mrs.data;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.dbunit.Assertion;
import org.dbunit.DBTestCase;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.NoSuchColumnException;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.xml.sax.InputSource;

import ch.fhnw.swc.mrs.model.Movie;
import ch.fhnw.swc.mrs.model.PriceCategory;
import ch.fhnw.swc.mrs.model.Rental;
import ch.fhnw.swc.mrs.model.User;

public class ITRentalDao extends DBTestCase {
//	the connection string to the in memory database
	private static final String DB_CONNECTION = "jdbc:hsqldb:mem:mrs";

	// the connection string to the database 
//    private static final String DB_CONNECTION = "jdbc:hsqldb:hsql://localhost/mrs";

	/** Class under test: RentalDAO. */
	private RentalDAO dao;
	private IDatabaseTester tester;
	private Connection connection;

	/** Create a new Integration Test object. */
	public ITRentalDao() {
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, HsqlDatabase.DB_DRIVER);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, DB_CONNECTION);
		System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, "sa");
		System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, "");
	}

	@Override
	protected void setUpDatabaseConfig(DatabaseConfig config) {
		config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
	}

	@Override
	protected IDataSet getDataSet() throws Exception {
		InputStream stream = this.getClass().getResourceAsStream("RentalDaoTestData.xml");
		return new FlatXmlDataSetBuilder().build(new InputSource(stream));
	}

	static {
		try {
			new HsqlDatabase().initDB(DB_CONNECTION);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize a DBUnit DatabaseTester object to use in tests.
	 * 
	 * @throws Exception
	 *             whenever something goes wrong.
	 */
	public void setUp() throws Exception {
		super.setUp();
        PriceCategory.init();
        tester = new JdbcDatabaseTester(HsqlDatabase.DB_DRIVER, DB_CONNECTION);
		connection = getConnection().getConnection();
		dao = new SQLRentalDAO(connection);
	}

	public void tearDown() throws Exception {
		connection.close();
		tester.onTearDown();
	}

	public void testDelete() throws Exception {
	    // get daos for Movies and Users in order to ...
	    MovieDAO mdao = new SQLMovieDAO(connection);
		UserDAO udao = new SQLUserDAO(connection);
		
		// ... get corresponding objects from the database, then ...
		User u = udao.getById(99);
		Movie m = mdao.getById(2);
		
		// ... instantiate a rental object with a valid id.
		Rental rental = Rental.materializeRentalFromDB(8, u, m, LocalDate.of(2017, 8, 30));
        // Delete record
		dao.delete(rental);
		
		// compare resulting table in db with expected table.
		assertResult("RentalDaoTestResult.xml");
	}
	
	public void testGetAllMultipleRows() throws Exception {
		List<Rental> rentallist = dao.getAll();
		ITable actualTable = convertToTable(rentallist);

		InputStream stream = this.getClass().getResourceAsStream("RentalDaoTestData.xml");
		IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(stream);
		ITable expectedTable = expectedDataSet.getTable("rentals");

		Assertion.assertEquals(expectedTable, actualTable);
	}

	public void testGetAllSingleRow() throws Exception {
		InputStream stream = this.getClass().getResourceAsStream("RentalDaoSingleRow.xml");
		IDataSet dataSet = new FlatXmlDataSetBuilder().build(stream);
		DatabaseOperation.CLEAN_INSERT.execute(tester.getConnection(), dataSet);

		List<Rental> rentallist = dao.getAll();
		assertEquals(1, rentallist.size());
		Rental ren = rentallist.get(0);
		assertEquals(5, ren.getId());
		assertEquals("Goldfinger", ren.getMovie().getTitle());
		assertEquals("Bond", ren.getUser().getName());
		assertEquals(LocalDate.of(2017, 8, 31), ren.getRentalDate());
	}

	public void testGetAllEmptyTable() throws Exception {
		InputStream stream = this.getClass().getResourceAsStream("RentalDaoEmpty.xml");
		IDataSet dataSet = new XmlDataSet(stream);
		DatabaseOperation.CLEAN_INSERT.execute(tester.getConnection(), dataSet);

		List<Rental> rentallist = dao.getAll();
		assertNotNull(rentallist);
		assertEquals(0, rentallist.size());
	}
	
    public void testSave() throws Exception {
        MovieDAO mdao = new SQLMovieDAO(connection);
        UserDAO udao = new SQLUserDAO(connection);
        
        Movie movie = mdao.getById(3);
        User user = udao.getById(42);
        Rental rental = new Rental(user, movie);
        dao.save(rental);
        Rental actual = dao.getById(rental.getId());
        
        assertEquals(rental.getMovie().getId(), actual.getMovie().getId());
        assertEquals(rental.getUser().getId(), actual.getUser().getId());
        assertEquals(rental.getRentalDate(), actual.getRentalDate());      
    }

    private void assertResult(String resultxml) throws Exception {
        // Fetch database data after deletion
        IDataSet databaseDataSet = tester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("rentals");

        InputStream stream = this.getClass().getResourceAsStream(resultxml);
        IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(stream);
        ITable expectedTable = expectedDataSet.getTable("rentals");
        
        ITable filteredTable = DefaultColumnFilter.includedColumnsTable(actualTable, 
                expectedTable.getTableMetaData().getColumns());

        Assertion.assertEquals(expectedTable, filteredTable);
    }

	@SuppressWarnings("deprecation")
	private ITable convertToTable(List<Rental> rentallist) throws Exception {
		ITableMetaData meta = new TableMetaData();
		DefaultTable t = new DefaultTable(meta);
		int row = 0;
		for (Rental rental : rentallist) {
			t.addRow();
			LocalDate d = rental.getRentalDate();
			t.setValue(row, "id", rental.getId());
			t.setValue(row, "movieid", rental.getMovie().getId());
			t.setValue(row, "clientid", rental.getUser().getId());
			t.setValue(row, "rentaldate", new Date(d.getYear()-1900, d.getMonthValue()-1, d.getDayOfMonth()));
			row++;
		}
		return t;
	}

	private static final class TableMetaData implements ITableMetaData {

		private List<Column> cols = new ArrayList<>();

		TableMetaData() {
			cols.add(new Column("id", DataType.INTEGER));
			cols.add(new Column("movieid", DataType.INTEGER));
			cols.add(new Column("clientid", DataType.INTEGER));
			cols.add(new Column("rentaldate", DataType.DATE));
		}

		@Override
		public int getColumnIndex(String colname) throws DataSetException {
			int index = 0;
			for (Column c : cols) {
				if (c.getColumnName().equals(colname.toLowerCase())) {
					return index;
				}
				index++;
			}
			throw new NoSuchColumnException(getTableName(), colname);
		}

		@Override
		public Column[] getColumns() throws DataSetException {
			return cols.toArray(new Column[4]);
		}

		@Override
		public Column[] getPrimaryKeys() throws DataSetException {
			Column[] cols = new Column[1];
			cols[0] = new Column("id", DataType.INTEGER);
			return cols;
		}

		@Override
		public String getTableName() {
			return "rentals";
		}
	}
}