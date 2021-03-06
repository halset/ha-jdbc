/*
 * HA-JDBC: High-Availability JDBC
 * Copyright 2004-2009 Paul Ferraro
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.hajdbc.codec.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.sql.SQLException;
import java.text.MessageFormat;

import net.sf.hajdbc.codec.Codec;
import net.sf.hajdbc.codec.CodecFactory;
import net.sf.hajdbc.util.Resources;
import net.sf.hajdbc.util.Strings;

/**
 * Used to decrypt configuration file passwords using a symmetric key stored in a keystore.
 * Use the following command to generate Base-64 encoded encrypted passwords for use in your config file:<br/>
 * <p><code>java -classpath ha-jdbc.jar net.sf.hajdbc.codec.crypto.CipherCodecFactory [password]</code></p>
 * The following system properties can be used to customize the properties of the key and/or keystore:
 * <table>
 * 	<tr>
 * 		<th>Property</th>
 * 		<th>Default</th>
 * 	</tr>
 * 	<tr>
 * 		<td>ha-jdbc.keystore.file</td>
 * 		<td>~/.keystore</td>
 * 	</tr>
 * 	<tr>
 * 		<td>ha-jdbc.keystore.type</td>
 * 		<td>jceks</td>
 * 	</tr>
 * 	<tr>
 * 		<td>ha-jdbc.keystore.password</td>
 * 		<td><em>none</em></td>
 * 	</tr>
 * 	<tr>
 * 		<td>ha-jdbc.key.alias</td>
 * 		<td>ha-jdbc</td>
 * 	</tr>
 * 	<tr>
 * 		<td>ha-jdbc.key.password</td>
 * 		<td><em>required</em><td>
 * 	</tr>
 * </table>
 * @author Paul Ferraro
 */
public class CipherCodecFactory implements CodecFactory, Serializable
{
	private static final long serialVersionUID = -4409167180573651279L;
	
	public static final String DEFAULT_KEYSTORE_FILE = String.format("%s/.keystore", System.getProperty("user.home"));
	public static final String DEFAULT_KEY_ALIAS = "ha-jdbc";
	
	enum Property
	{
		KEYSTORE_FILE("keystore.file", DEFAULT_KEYSTORE_FILE),
		KEYSTORE_TYPE("keystore.type", "jceks"),
		KEYSTORE_PASSWORD("keystore.password", null),
		KEY_ALIAS("key.alias", DEFAULT_KEY_ALIAS),
		KEY_PASSWORD("key.password", null);
		
		final String nameFormat;
		final String name;
		final String defaultValue;
		
		private Property(String name, String defaultValue)
		{
			this.nameFormat = "ha-jdbc.{0}." + name;
			this.name = "ha-jdbc." + name;
			this.defaultValue = defaultValue;
		}
	}
	
	private String getProperty(String id, Property property)
	{
		String value = System.getProperty(MessageFormat.format(property.nameFormat, id));
		
		if (value != null) return value;
		
		String pattern = System.getProperty(property.name, property.defaultValue);
		
		if (pattern == null) return null;
		
		return MessageFormat.format(pattern, id);
	}
	
	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.codec.CodecFactory#createCodec(java.util.Properties)
	 */
	@Override
	public Codec createCodec(String clusterId) throws SQLException
	{
		String type = this.getProperty(clusterId, Property.KEYSTORE_TYPE);
		File file = new File(this.getProperty(clusterId, Property.KEYSTORE_FILE));
		String password = this.getProperty(clusterId, Property.KEYSTORE_PASSWORD);

		String keyAlias = this.getProperty(clusterId, Property.KEY_ALIAS);
		String keyPassword = this.getProperty(clusterId, Property.KEY_PASSWORD);
		
		try
		{
			KeyStore store = KeyStore.getInstance(type);
			
			InputStream input = new FileInputStream(file);
			
			try
			{
				store.load(input, (password != null) ? password.toCharArray() : null);

				return new CipherCodec(store.getKey(keyAlias, (keyPassword != null) ? keyPassword.toCharArray() : null));
			}
			finally
			{
				Resources.close(input);
			}
		}
		catch (GeneralSecurityException e)
		{
			throw new SQLException(e);
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}
	
	public static void main(String... args)
	{
		if (args.length != 2)
		{
			System.err.println(String.format("Usage:%s\tjava %s <cluster-id> <password-to-encrypt>", Strings.NEW_LINE, CipherCodecFactory.class.getName()));
			System.exit(1);
			return;
		}
		
		String clusterId = args[0];
		String value = args[1];
		
		try
		{
			Codec codec = new CipherCodecFactory().createCodec(clusterId);

			System.out.println(codec.encode(value));
		}
		catch (SQLException e)
		{
			e.printStackTrace(System.err);
		}
	}
}
