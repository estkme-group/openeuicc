package com.truphone.util;

import java.util.Properties;

public class PropUtil {


	public static String getMandatoryProperty(final Properties props,
			final String key) {
		if (!props.containsKey(key)) {
			throw new IllegalArgumentException("mandatory property missing: "
					+ key);
		}
		return props.getProperty(key);
	}

	public static int getIntProperty(final Properties props, final String key,
			final int defaultValue) {
		if (props.containsKey(key)) {
			try {
				return Integer.decode(props.getProperty(key));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"syntax error in int property: " + key, e);
			}
		} else {
			return defaultValue;
		}
	}

}
