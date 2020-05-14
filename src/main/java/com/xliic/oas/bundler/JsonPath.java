package com.xliic.oas.bundler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class JsonPath extends ArrayList<String> {
    public JsonPath(String... keys) {
        super();
        for (String key : keys) {
            add(key);
        }
    }

    public JsonPath(JsonPath path) {
        super(path);
    }

    JsonPath withKey(String key) {
        JsonPath copy = new JsonPath(this);
        copy.add(key);
        return copy;
    }

    public static String toPointer(JsonPath path) throws UnsupportedEncodingException {
        ArrayList<String> result = new ArrayList<String>();
        for (String key : path) {
            String tildaEncoded = key.replaceAll("~", "~0").replaceAll("\\/", "~1");
            result.add(encodeURIComponent(tildaEncoded));
        }

        return "#/" + String.join("/", result);
    }

    public String toPointer() throws UnsupportedEncodingException {
        return JsonPath.toPointer(this);
    }

    private static String encodeURIComponent(String segment) throws UnsupportedEncodingException {
        return URLEncoder.encode(segment, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!")
                .replaceAll("\\%27", "'").replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");

    }

    public boolean isSubPathOf(JsonPath path) {
        if (path.size() > this.size()) {
            return false;
        }

        for (int i = 0; i < path.size(); i++) {
            if (!path.get(i).equals(this.get(i))) {
                return false;
            }
        }
        return true;
    }
}