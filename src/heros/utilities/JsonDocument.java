/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.utilities;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

public class JsonDocument {

	private DefaultValueMap<String, JsonDocument> documents = new DefaultValueMap<String, JsonDocument>() {
		@Override
		protected JsonDocument createItem(String key) {
			return new JsonDocument();
		}
	};
	private DefaultValueMap<String, JsonArray> arrays = new DefaultValueMap<String, JsonArray>() {
		@Override
		protected JsonArray createItem(String key) {
			return new JsonArray();
		}
	};
	private Map<String, String> keyValuePairs = Maps.newHashMap();

	public JsonDocument doc(String key) {
		return documents.getOrCreate(escape(key));
	}

	public JsonDocument doc(String key, JsonDocument doc) {
		key = escape(key);
		if (documents.containsKey(key))
			throw new IllegalArgumentException("There is already a document registered for key: " + key);
		documents.put(key, doc);
		return doc;
	}

	public JsonArray array(String key) {
		return arrays.getOrCreate(escape(key));
	}

	public void keyValue(String key, String value) {
		keyValuePairs.put(escape(key), escape(value));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		write(builder, 0);
		return builder.toString();
	}

	public void write(StringBuilder builder, int tabs) {
		builder.append("{\n");

		for (Entry<String, String> entry : keyValuePairs.entrySet()) {
			tabs(tabs + 1, builder);
			builder.append("\"" + entry.getKey() + "\": \"" + entry.getValue() + "\",\n");
		}

		for (Entry<String, JsonArray> entry : arrays.entrySet()) {
			tabs(tabs + 1, builder);
			builder.append("\"" + entry.getKey() + "\": ");
			entry.getValue().write(builder, tabs + 1);
			builder.append(",\n");
		}

		for (Entry<String, JsonDocument> entry : documents.entrySet()) {
			tabs(tabs + 1, builder);
			builder.append("\"" + entry.getKey() + "\": ");
			entry.getValue().write(builder, tabs + 1);
			builder.append(",\n");
		}

		if (!keyValuePairs.isEmpty() || !arrays.isEmpty() || !documents.isEmpty())
			builder.delete(builder.length() - 2, builder.length() - 1);

		tabs(tabs, builder);
		builder.append("}");
	}

	static void tabs(int tabs, StringBuilder builder) {
		for (int i = 0; i < tabs; i++)
			builder.append("\t");
	}

	static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
				if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
					String ss = Integer.toHexString(ch);
					sb.append("\\u");
					for (int k = 0; k < 4 - ss.length(); k++) {
						sb.append('0');
					}
					sb.append(ss.toUpperCase());
				} else {
					sb.append(ch);
				}
			}
		}
		return sb.toString();
	}
}