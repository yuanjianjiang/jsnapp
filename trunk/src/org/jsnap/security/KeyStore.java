/************************************************************************
 * This file is part of jsnap.                                          *
 *                                                                      *
 * jsnap is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by *
 * the Free Software Foundation, either version 3 of the License, or    *
 * (at your option) any later version.                                  *
 *                                                                      *
 * jsnap is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 * GNU General Public License for more details.                         *
 *                                                                      *
 * You should have received a copy of the GNU General Public License    *
 * along with jsnap.  If not, see <http://www.gnu.org/licenses/>.       *
 ************************************************************************/

package org.jsnap.security;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.exception.security.KeyStoreInitiateException;
import org.jsnap.util.JUtility;

public final class KeyStore {
	private String keyStorePass;
	private String keyStoreUrl;
	private boolean loaded;

	private static final String KEYSTORE = "javax.net.ssl.keyStore";
	private static final String KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

	public KeyStore() {
		clear();
	}

	public void clear() {
		loaded = false;
		keyStoreUrl = null;
		keyStorePass = null;
		System.setProperty(KEYSTORE, "");
		System.setProperty(KEYSTORE_PASSWORD, "");
	}

	public void load(String keyStoreUrl, String keyStorePass) throws KeyStoreInitiateException {
		File f = new File(JUtility.valueOf(keyStoreUrl, ""));
		if (f.exists() == false)
			throw new KeyStoreInitiateException(new FileNotFoundException(f.getAbsolutePath() + " does not exist"));
		else if (f.isFile() == false)
			throw new KeyStoreInitiateException(new FileNotFoundException(f.getAbsolutePath() + " is not a regular file"));
		String p = JUtility.valueOf(keyStorePass, "");
		if (loaded == false || f.getAbsolutePath().equals(this.keyStoreUrl) == false || p.equals(this.keyStorePass) == false) {
			this.keyStoreUrl = f.getAbsolutePath();
			this.keyStorePass = p;
			this.loaded = true;
			Logger.getLogger(KeyStore.class).log(Level.INFO, "Keystore is at " + f.getAbsolutePath());
			System.setProperty(KEYSTORE, this.keyStoreUrl);
			System.setProperty(KEYSTORE_PASSWORD, this.keyStorePass);
		}
	}
}
