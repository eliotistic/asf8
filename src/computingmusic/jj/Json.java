package computingmusic.jj;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader: objects become LinkedHashMap, arrays ArrayList, numbers Long or Double,
 * plus String, Boolean and null.  Written here so the project needs no JSON library.
 *
 * Complexity: O(n) in the length of the text.
 */
public final class Json {
	private final String s;
	private int i;

	private Json(String text) {
		s = text;
	}

	public static Object parse(String text) {
		Json j = new Json(text);
		j.skip();
		Object v = j.value();
		j.skip();
		if (j.i != j.s.length()) {
			throw j.error("trailing text");
		}
		return v;
	}

	/** Write a value (Map, List, String, Number, Boolean, null, arrays of int) as compact JSON.  Complexity: O(size). */
	public static String write(Object v) {
		StringBuilder b = new StringBuilder();
		write(b, v);
		return b.toString();
	}

	private static void write(StringBuilder b, Object v) {
		if (v == null) {
			b.append("null");
		} else if (v instanceof Map) {
			b.append('{');
			boolean first = true;
			for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
				if (!first) {
					b.append(',');
				}
				first = false;
				quote(b, String.valueOf(e.getKey()));
				b.append(':');
				write(b, e.getValue());
			}
			b.append('}');
		} else if (v instanceof Iterable) {
			b.append('[');
			boolean first = true;
			for (Object o : (Iterable<?>) v) {
				if (!first) {
					b.append(',');
				}
				first = false;
				write(b, o);
			}
			b.append(']');
		} else if (v instanceof int[]) {
			b.append('[');
			int[] a = (int[]) v;
			for (int i = 0; i < a.length; i++) {
				b.append(i == 0 ? "" : ",").append(a[i]);
			}
			b.append(']');
		} else if (v instanceof Number || v instanceof Boolean) {
			b.append(v);
		} else {
			quote(b, String.valueOf(v));
		}
	}

	private static void quote(StringBuilder b, String s) {
		b.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"': b.append("\\\""); break;
			case '\\': b.append("\\\\"); break;
			case '\n': b.append("\\n"); break;
			case '\r': b.append("\\r"); break;
			case '\t': b.append("\\t"); break;
			default:
				if (c < 0x20) {
					b.append(String.format("\\u%04x", (int) c));
				} else {
					b.append(c);
				}
			}
		}
		b.append('"');
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> object(Object v) {
		return (Map<String, Object>) v;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> array(Object v) {
		return (List<Object>) v;
	}

	private IllegalArgumentException error(String why) {
		return new IllegalArgumentException("JSON: " + why + " at offset " + i);
	}

	private void skip() {
		while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
	}

	private Object value() {
		if (i >= s.length()) {
			throw error("unexpected end");
		}
		char c = s.charAt(i);
		switch (c) {
		case '{':
			return obj();
		case '[':
			return arr();
		case '"':
			return str();
		case 't':
			return literal("true", Boolean.TRUE);
		case 'f':
			return literal("false", Boolean.FALSE);
		case 'n':
			return literal("null", null);
		default:
			return num();
		}
	}

	private Object literal(String word, Object v) {
		if (!s.startsWith(word, i)) {
			throw error("bad literal");
		}
		i += word.length();
		return v;
	}

	private Map<String, Object> obj() {
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		i++;
		skip();
		if (s.charAt(i) == '}') {
			i++;
			return m;
		}
		while (true) {
			skip();
			String k = str();
			skip();
			if (s.charAt(i++) != ':') {
				throw error("expected ':'");
			}
			skip();
			m.put(k, value());
			skip();
			char c = s.charAt(i++);
			if (c == '}') {
				return m;
			}
			if (c != ',') {
				throw error("expected ',' or '}'");
			}
		}
	}

	private List<Object> arr() {
		List<Object> a = new ArrayList<Object>();
		i++;
		skip();
		if (s.charAt(i) == ']') {
			i++;
			return a;
		}
		while (true) {
			skip();
			a.add(value());
			skip();
			char c = s.charAt(i++);
			if (c == ']') {
				return a;
			}
			if (c != ',') {
				throw error("expected ',' or ']'");
			}
		}
	}

	private String str() {
		if (s.charAt(i) != '"') {
			throw error("expected a string");
		}
		i++;
		StringBuilder b = new StringBuilder();
		while (true) {
			char c = s.charAt(i++);
			if (c == '"') {
				return b.toString();
			}
			if (c != '\\') {
				b.append(c);
				continue;
			}
			char e = s.charAt(i++);
			switch (e) {
			case 'n': b.append('\n'); break;
			case 't': b.append('\t'); break;
			case 'r': b.append('\r'); break;
			case 'b': b.append('\b'); break;
			case 'f': b.append('\f'); break;
			case 'u':
				b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
				i += 4;
				break;
			default: b.append(e);
			}
		}
	}

	private Object num() {
		int start = i;
		while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
			i++;
		}
		String t = s.substring(start, i);
		if (t.isEmpty()) {
			throw error("unexpected character '" + s.charAt(start) + "'");
		}
		if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0) {
			return Double.valueOf(t);
		}
		return Long.valueOf(t);
	}
}
