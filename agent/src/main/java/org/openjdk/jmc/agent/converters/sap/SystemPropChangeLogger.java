/*
 * Copyright (c) 2024 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package org.openjdk.jmc.agent.converters.sap;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import org.openjdk.jmc.agent.util.sap.JdkLogging;

public class SystemPropChangeLogger {
	private static final Properties systemProps = AccessController.doPrivileged(new PrivilegedAction<Properties>() {
		public Properties run() {
			return System.getProperties();
		}
	});

	private static final ThreadLocal<String> usedKey = new ThreadLocal<String>();
	private static final ThreadLocal<String> usedValue = new ThreadLocal<String>();

	public static boolean logProperties(Properties props) {
		String key = usedKey.get();
		assert key == null;
		String val = usedValue.get();

		usedKey.remove();
		usedValue.remove();

		if (props == systemProps) {
			String oldVal = props.getProperty(key);

			if (val == null) {
				JdkLogging.log("System properties '" + key + "' with value '" + oldVal + "' removed");
			} else {
				JdkLogging.log("System property '" + key + "' changed from '" + oldVal + "' to '" + val + "'");
			}

			return true;
		}

		return false;
	}

	public static boolean logKey(Object key) {
		assert usedKey.get() == null;

		if (key instanceof String) {
			usedKey.set((String) key);

			return true;
		}

		return false;
	}

	public static boolean logValue(Object value) {
		assert usedValue.get() == null;

		if ((value instanceof String) || (value == null)) {
			usedValue.set((String) value);
		}

		return true;
	}
}
