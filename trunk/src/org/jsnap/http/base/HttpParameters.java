package org.jsnap.http.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class HttpParameters {
	private final HashMap<String, Set<String>> parameters;

	public HttpParameters() {
		parameters = new HashMap<String, Set<String>>();
	}

	public void put(String name, String value) {
		Set<String> set = parameters.get(name);
		if (set == null) {
			set = new HashSet<String>();
			parameters.put(name, set);
		}
		set.add(value);
	}

	public String get(String name) {
		String value = null;
		Set<String> set = parameters.get(name);
		if (set != null)
			value = set.iterator().next();
		return value;
	}

	public Set<String> getAll(String name) {
		return parameters.get(name);
	}
}
