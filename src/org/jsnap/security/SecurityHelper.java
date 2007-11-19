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
