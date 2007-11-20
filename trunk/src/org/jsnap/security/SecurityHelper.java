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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jsnap.exception.security.PolicyException;

public class SecurityHelper {
	@SuppressWarnings("unchecked")
	public static AuthenticationPolicy getAuthenticationPolicy(String dbname, String className) throws PolicyException {
		try {
			Class c = Class.forName(className);
			if (AuthenticationPolicy.class.isAssignableFrom(c) == false)
				throw new PolicyException(className + " does not implement the AuthenticationPolicy interface");
			Constructor ctor = c.getConstructor(new Class[]{});
			AuthenticationPolicy policy = (AuthenticationPolicy)ctor.newInstance(new Object[]{});
			policy.setOwnerName(dbname);
			return policy;
		} catch (ClassNotFoundException e) {
			throw new PolicyException(e);
		} catch (NoSuchMethodException e) {
			throw new PolicyException(e);
		} catch (IllegalArgumentException e) {
			throw new PolicyException(e);
		} catch (InstantiationException e) {
			throw new PolicyException(e);
		} catch (IllegalAccessException e) {
			throw new PolicyException(e);
		} catch (InvocationTargetException e) {
			throw new PolicyException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static AccessControlPolicy getAccessControlPolicy(String dbname, String className) throws PolicyException {
		try {
			Class c = Class.forName(className);
			if (AccessControlPolicy.class.isAssignableFrom(c) == false)
				throw new PolicyException(className + " does not implement the AccessControlPolicy interface");
			Constructor ctor = c.getConstructor(new Class[]{});
			AccessControlPolicy policy = (AccessControlPolicy)ctor.newInstance(new Object[]{});
			policy.setOwnerName(dbname);
			return policy;
		} catch (ClassNotFoundException e) {
			throw new PolicyException(e);
		} catch (NoSuchMethodException e) {
			throw new PolicyException(e);
		} catch (IllegalArgumentException e) {
			throw new PolicyException(e);
		} catch (InstantiationException e) {
			throw new PolicyException(e);
		} catch (IllegalAccessException e) {
			throw new PolicyException(e);
		} catch (InvocationTargetException e) {
			throw new PolicyException(e);
		}
	}
}
