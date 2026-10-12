package lab;

import java.util.*;

/** Minimal JSON codec for an offline JDK-only lab. Not a replacement for Jackson in production. */
public final class Json {
    private Json() {}
    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringJoiner out = new StringJoiner(",", "{", "}");
            map.forEach((k, v) -> out.add(stringify(String.valueOf(k)) + ":" + stringify(v)));
            return out.toString();
        }
        if (value instanceof Iterable<?> iter) {
            StringJoiner out = new StringJoiner(",", "[", "]");
            for (Object item : iter) out.add(stringify(item));
            return out.toString();
        }
        if (value.getClass().isArray()) {
            StringJoiner out = new StringJoiner(",", "[", "]");
            for (int i=0; i<java.lang.reflect.Array.getLength(value); i++)
                out.add(stringify(java.lang.reflect.Array.get(value, i)));
            return out.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }
    private static String escape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            switch(c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {if (c<0x20) out.append(String.format("\\u%04x",(int)c)); else out.append(c);}
            }
        }
        return out.toString();
    }
    public static Object parse(String s) { Parser p = new Parser(s); Object v=p.value(); p.ws(); if(p.pos!=s.length()) throw new IllegalArgumentException("Extra JSON text"); return v; }
    @SuppressWarnings("unchecked")
    public static Map<String,Object> object(Object v) {if (!(v instanceof Map<?,?>)) throw new IllegalArgumentException("Expected object"); return (Map<String,Object>)v; }
    @SuppressWarnings("unchecked")
    public static List<Object> array(Object v) {if (!(v instanceof List<?>)) throw new IllegalArgumentException("Expected array");return (List<Object>)v;}
    public static String str(Object v) {return v==null?"":String.valueOf(v);}
    public static Map<String,Object> map(Object... pairs) {
        Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;
    }
    private static final class Parser {
        final String s; int pos=0;
        Parser(String s){this.s=Objects.requireNonNull(s);}
        void ws(){while(pos<s.length() && Character.isWhitespace(s.charAt(pos)))pos++;}
        char next(){if(pos>=s.length())throw new IllegalArgumentException("JSON truncated");return s.charAt(pos++);}
        char peek(){ws();if(pos>=s.length())throw new IllegalArgumentException("JSON truncated");return s.charAt(pos);}
        Object value(){char c=peek();return switch(c){case '{'->obj();case '['->arr();case '"'->string();case 't'->lit("true",true);case 'f'->lit("false",false);case 'n'->lit("null",null);default->number();};}
        Object lit(String token,Object val){if(!s.startsWith(token,pos))throw new IllegalArgumentException("Invalid JSON literal");pos+=token.length();return val;}
        Map<String,Object> obj(){next();Map<String,Object> result=new LinkedHashMap<>();if(peek()=='}'){next();return result;}while(true){if(peek()!='"')throw new IllegalArgumentException("Object key must be string");String key=string();if(nextNonSpace()!=':')throw new IllegalArgumentException("Expected colon");result.put(key,value());char c=nextNonSpace();if(c=='}')return result;if(c!=',')throw new IllegalArgumentException("Expected comma");}}
        List<Object> arr(){next();List<Object> a=new ArrayList<>();if(peek()==']'){next();return a;}while(true){a.add(value());char c=nextNonSpace();if(c==']')return a;if(c!=',')throw new IllegalArgumentException("Expected comma");}}
        char nextNonSpace(){ws();return next();}
        String string(){if(next()!='"')throw new IllegalArgumentException("Expected quote");StringBuilder b=new StringBuilder();while(true){char c=next();if(c=='"')return b.toString();if(c=='\\'){char e=next();switch(e){case '"','\\','/'->b.append(e);case 'b'->b.append('\b');case 'f'->b.append('\f');case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'u'->{if(pos+4>s.length())throw new IllegalArgumentException("Bad Unicode escape");b.append((char)Integer.parseInt(s.substring(pos,pos+4),16));pos+=4;}default->throw new IllegalArgumentException("Bad escape");}}else{if(c<0x20)throw new IllegalArgumentException("Invalid control char");b.append(c);}}}
        Number number(){ws();int start=pos;if(pos<s.length() && s.charAt(pos)=='-')pos++;if(pos>=s.length())throw new IllegalArgumentException("Bad number");if(s.charAt(pos)=='0')pos++;else{if(!Character.isDigit(s.charAt(pos)))throw new IllegalArgumentException("Bad number");while(pos<s.length() && Character.isDigit(s.charAt(pos)))pos++;}if(pos<s.length()&&s.charAt(pos)=='.'){pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;}if(pos<s.length()&&(s.charAt(pos)=='e'||s.charAt(pos)=='E')){pos++;if(pos<s.length()&&(s.charAt(pos)=='+'||s.charAt(pos)=='-'))pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;}String raw=s.substring(start,pos);try {if(raw.contains(".")||raw.contains("e")||raw.contains("E")) return Double.valueOf(raw); return Long.valueOf(raw);}catch(NumberFormatException e){throw new IllegalArgumentException("Bad number");}}
    }
}
