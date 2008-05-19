/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2008 Paul Ferraro
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

import java.sql.PreparedStatement;
import java.util.Map;

import net.sf.hajdbc.Database;

import org.easymock.EasyMock;

/**
 * @author Paul Ferraro
 *
 */
public class TestPreparedStatement extends AbstractTestPreparedStatement<PreparedStatement>
{
	/**
	 * @see net.sf.hajdbc.sql.AbstractTestStatement#getStatementClass()
	 */
	@Override
	protected Class<PreparedStatement> getStatementClass()
	{
		return PreparedStatement.class;
	}
	
	@Override
	protected AbstractStatementInvocationHandler<?, PreparedStatement> getInvocationHandler(Map<Database, PreparedStatement> map) throws Exception
	{
		return new PreparedStatementInvocationHandler(this.connection, this.parent, EasyMock.createMock(Invoker.class), map, this.transactionContext, this.fileSupport, this.sql);
	}
}
