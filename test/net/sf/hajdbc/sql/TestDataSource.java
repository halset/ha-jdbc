/**
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import net.sf.hajdbc.Balancer;
import net.sf.hajdbc.Database;
import net.sf.hajdbc.DatabaseCluster;
import net.sf.hajdbc.LockManager;
import net.sf.hajdbc.util.reflect.ProxyFactory;

import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings({ "unchecked", "nls" })
@Test
public class TestDataSource implements javax.sql.DataSource
{
	private DatabaseCluster cluster = EasyMock.createStrictMock(DatabaseCluster.class);
	private Balancer balancer = EasyMock.createStrictMock(Balancer.class);
	private javax.sql.DataSource dataSource1 = EasyMock.createStrictMock(javax.sql.DataSource.class);
	private javax.sql.DataSource dataSource2 = EasyMock.createStrictMock(javax.sql.DataSource.class);
	private LockManager lockManager = EasyMock.createStrictMock(LockManager.class);
	private Lock lock = EasyMock.createStrictMock(Lock.class);
	
	private Database database1 = new MockDataSourceDatabase("1", this.dataSource1);
	private Database database2 = new MockDataSourceDatabase("2", this.dataSource2);
	private Set<Database> databaseSet;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private javax.sql.DataSource dataSource;

	@BeforeClass
	void init()
	{
		Map<Database, javax.sql.DataSource> map = new TreeMap<Database, javax.sql.DataSource>();
		map.put(this.database1, this.dataSource1);
		map.put(this.database2, this.dataSource2);
		
		this.databaseSet = map.keySet();
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		this.replay();
		
		this.dataSource = ProxyFactory.createProxy(javax.sql.DataSource.class, new DataSourceInvocationHandler(this.cluster));

		this.verify();
		this.reset();
	}
	
	private Object[] objects()
	{
		return new Object[] { this.cluster, this.balancer, this.dataSource1, this.dataSource2, this.lockManager, this.lock };
	}
	
	void replay()
	{
		EasyMock.replay(this.objects());
	}
	
	void verify()
	{
		EasyMock.verify(this.objects());
	}

	@AfterMethod
	void reset()
	{
		EasyMock.reset(this.objects());
	}
	
	public void testGetConnection() throws SQLException
	{
		Connection connection1 = EasyMock.createStrictMock(Connection.class);
		Connection connection2 = EasyMock.createStrictMock(Connection.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getLockManager()).andReturn(this.lockManager);
		EasyMock.expect(this.lockManager.readLock(LockManager.GLOBAL)).andReturn(this.lock);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
			
		EasyMock.expect(this.dataSource1.getConnection()).andReturn(connection1);
		EasyMock.expect(this.dataSource2.getConnection()).andReturn(connection2);
		
		this.replay();
		
		Connection result = this.getConnection();
		
		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == connection1;
		assert proxy.getObject(this.database2) == connection2;
	}
	
	/**
	 * @see javax.sql.DataSource#getConnection()
	 */
	@Override
	public Connection getConnection() throws SQLException
	{
		return this.dataSource.getConnection();
	}

	@DataProvider(name = "string-string")
	Object[][] connectProvider()
	{
		return new Object[][] { new Object[] { "", "" } };
	}

	@Test(dataProvider = "string-string")
	public void testGetConnection(String user, String password) throws SQLException
	{
		Connection connection1 = EasyMock.createStrictMock(Connection.class);
		Connection connection2 = EasyMock.createStrictMock(Connection.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getLockManager()).andReturn(this.lockManager);
		EasyMock.expect(this.lockManager.readLock(LockManager.GLOBAL)).andReturn(this.lock);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.dataSource1.getConnection(user, password)).andReturn(connection1);
		EasyMock.expect(this.dataSource2.getConnection(user, password)).andReturn(connection2);
		
		this.replay();
		
		Connection result = this.getConnection(user, password);
		
		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == connection1;
		assert proxy.getObject(this.database2) == connection2;
	}
	
	/**
	 * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
	 */
	@Override
	public Connection getConnection(String user, String password) throws SQLException
	{
		return this.dataSource.getConnection(user, password);
	}

	public void testGetLogWriter()
	{
		PrintWriter writer = new PrintWriter(System.out);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		try
		{
			EasyMock.expect(this.dataSource1.getLogWriter()).andReturn(writer);
			
			this.replay();
			
			PrintWriter result = this.getLogWriter();
			
			this.verify();
			
			assert result == writer;
		}
		catch (SQLException e)
		{
			assert false : e;
		}
	}
	
	/**
	 * @see javax.sql.CommonDataSource#getLogWriter()
	 */
	@Override
	public PrintWriter getLogWriter() throws SQLException
	{
		return this.dataSource.getLogWriter();
	}

	public void testGetLoginTimeout()
	{
		int timeout = 1;
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		try
		{
			EasyMock.expect(this.dataSource1.getLoginTimeout()).andReturn(timeout);
			
			this.replay();
			
			int result = this.getLoginTimeout();
	
			this.verify();
			
			assert result == timeout;
		}
		catch (SQLException e)
		{
			assert false : e;
		}
	}
	
	/**
	 * @see javax.sql.CommonDataSource#getLoginTimeout()
	 */
	@Test
	public int getLoginTimeout() throws SQLException
	{
		return this.dataSource.getLoginTimeout();
	}

	@DataProvider(name = "writer")
	Object[][] writerProvider()
	{
		return new Object[][] { new Object[] { new PrintWriter(new StringWriter()) } };
	}

	/**
	 * @see javax.sql.CommonDataSource#setLogWriter(java.io.PrintWriter)
	 */
	@Test(dataProvider = "writer")
	public void setLogWriter(PrintWriter writer) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.dataSource1.setLogWriter(writer);
		this.dataSource2.setLogWriter(writer);

		this.replay();
		
		this.dataSource.setLogWriter(writer);
		
		this.verify();
	}

	@DataProvider(name = "int")
	Object[][] timeoutProvider()
	{
		return new Object[][] { new Object[] { 0 } };
	}

	/**
	 * @see javax.sql.CommonDataSource#setLoginTimeout(int)
	 */
	@Test(dataProvider = "int")
	public void setLoginTimeout(int timeout) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.dataSource1.setLoginTimeout(timeout);
		this.dataSource2.setLoginTimeout(timeout);

		this.replay();
		
		this.dataSource.setLoginTimeout(timeout);
		
		this.verify();
	}

	public void testIsWrapperFor() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.dataSource1.isWrapperFor(Object.class)).andReturn(true);
		
		this.replay();
		
		boolean result = this.isWrapperFor(Object.class);

		this.verify();
		
		assert result;
	}
	
	/**
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Override
	public boolean isWrapperFor(Class<?> targetClass) throws SQLException
	{
		return this.dataSource.isWrapperFor(targetClass);
	}

	public void testUnwrap() throws SQLException
	{
		try
		{
			Object object = Object.class.newInstance();
			
			EasyMock.expect(this.cluster.isActive()).andReturn(true);
			
			EasyMock.expect(this.dataSource1.unwrap(Object.class)).andReturn(object);
			
			this.replay();

			Object result = this.unwrap(Object.class);

			this.verify();
			
			assert result == object;
		}
		catch (InstantiationException e)
		{
			assert false : e;
		}
		catch (IllegalAccessException e)
		{
			assert false : e;
		}
	}
	
	/**
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Override
	public <T> T unwrap(Class<T> targetClass) throws SQLException
	{
		return this.dataSource.unwrap(targetClass);
	}
}
