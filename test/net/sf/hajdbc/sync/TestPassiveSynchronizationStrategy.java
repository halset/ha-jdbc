/*
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
package net.sf.hajdbc.sync;

import net.sf.hajdbc.SynchronizationContext;

import org.easymock.EasyMock;

/**
 * @author Paul Ferraro
 *
 */
public class TestPassiveSynchronizationStrategy extends AbstractTestSynchronizationStrategy
{
	public TestPassiveSynchronizationStrategy()
	{
		super(new PassiveSynchronizationStrategy());
	}
	
	/**
	 * @see net.sf.hajdbc.sync.AbstractTestSynchronizationStrategy#testSynchronize()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <D> void testSynchronize()
	{
		SynchronizationContext<D> context = EasyMock.createStrictMock(SynchronizationContext.class);
		
		EasyMock.replay(context);
		
		this.synchronize(context);
		
		EasyMock.verify(context);
	}
}
