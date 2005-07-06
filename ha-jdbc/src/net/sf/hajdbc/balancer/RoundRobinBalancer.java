/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2004 Paul Ferraro
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
package net.sf.hajdbc.balancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import EDU.oswego.cs.dl.util.concurrent.Sync;

import net.sf.hajdbc.Database;

/**
 * @author  Paul Ferraro
 * @since   1.0
 */
public class RoundRobinBalancer extends AbstractBalancer
{
	private Set databaseSet = new HashSet();
	private List databaseList = new ArrayList();
	
	/**
	 * @see net.sf.hajdbc.balancer.AbstractBalancer#getDatabases()
	 */
	protected Collection getDatabases()
	{
		return this.databaseSet;
	}

	/**
	 * @see net.sf.hajdbc.Balancer#add(net.sf.hajdbc.Database)
	 */
	public boolean add(Database database)
	{
		Sync lock = this.acquireWriteLock();
		
		try
		{
			boolean added = this.databaseSet.add(database);
			
			if (added)
			{
				int weight = database.getWeight().intValue();
				
				for (int i = 0; i < weight; ++i)
				{
					this.databaseList.add(database);
				}
			}
			
			return added;
		}
		finally
		{
			lock.release();
		}
	}

	/**
	 * @see net.sf.hajdbc.Balancer#remove(net.sf.hajdbc.Database)
	 */
	public boolean remove(Database database)
	{
		Sync lock = this.acquireWriteLock();
		
		try
		{
			boolean removed = this.databaseSet.remove(database);
	
			if (removed)
			{
				int weight = database.getWeight().intValue();
	
				for (int i = 0; i < weight; ++i)
				{
					this.databaseList.remove(database);
				}
			}
			
			return removed;
		}
		finally
		{
			lock.release();
		}
	}
	
	/**
	 * @see net.sf.hajdbc.Balancer#next()
	 */
	public Database next()
	{
		Sync lock = this.acquireReadLock();
		
		try
		{
			if (this.databaseList.isEmpty())
			{
				return this.first();
			}
			
			Database database = (Database) this.databaseList.get(0);
			
			if (this.databaseList.size() > 1)
			{
				Collections.rotate(this.databaseList, -1);
			}
	
			return database;
		}
		finally
		{
			lock.release();
		}
	}
}