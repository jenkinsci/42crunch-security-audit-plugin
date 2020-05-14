package com.xliic.oas.bundler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonPointer {
    final Document.Part part;
    final URI target;
    final JsonPath path;

    Document.Part resolvedPart;
    JsonNode resolvedValue;
    JsonPath resolvedPath;
    int indirections = 0;
    // TODO implement support for circular pointer
    // boolean circular = false;

    public JsonPointer(Document.Part part, URI target) throws UnsupportedEncodingException {
        this.part = part;
        this.target = target;
        this.path = parse(target.getFragment());
    }

    private static JsonPath parse(String pointer) throws UnsupportedEncodingException {
        JsonPath result = new JsonPath();

        if (pointer == null || pointer.equals("")) {
            // return empty path
            return result;
        }

        String[] segments = pointer.split("/");
        for (String segment : segments) {
            result.add(URLDecoder.decode(segment.replaceAll("~1", "/").replaceAll("~0", "~"), "UTF-8"));
        }

        if (result.size() > 1 && result.get(0).equals("")) {
            result.remove(0);
            // TODO throw, must start with "" (/)
        }

        return result;
    }

    public JsonNode getValue() {
        return this.resolvedValue;
    }

    public int getIndirections() {
        return indirections;
    }

    public Document.Part getPart() {
        return this.resolvedPart;
    }

    public JsonPath getPath() {
        return this.resolvedPath;
    }

    public String getPointer() throws UnsupportedEncodingException {
        return JsonPath.toPointer(resolvedPath);
    }

    public String getFile() {
        return this.resolvedPart.location.getScheme() + ":" + this.resolvedPart.location.getSchemeSpecificPart();
    }

}