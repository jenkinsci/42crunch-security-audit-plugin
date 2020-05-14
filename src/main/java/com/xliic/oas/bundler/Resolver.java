package com.xliic.oas.bundler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Resolver {

    public static boolean isRef(final JsonNode node) {
        return node != null && node.isObject() && node.has("$ref") && node.get("$ref").isTextual();
    }

    public static boolean isExtendedRef(final JsonNode node) {
        return isRef(node) && node.size() > 1;
    }

    public static boolean isExternalRef(final JsonNode node) {
        return isRef(node) && !node.get("$ref").asText().startsWith("#");
    }

    public static JsonPointer resolveReference(Document.Part part, JsonNode ref)
            throws URISyntaxException, UnsupportedEncodingException {
        URI target = new URI(ref.get("$ref").asText());
        try {
            Document.Part targetPart = Document.getTargetPart(part, target);
            String fragment = target.getFragment();
            if (fragment == null || fragment.equals("")) {
                fragment = "/";
            }
            JsonPointer pointer = new JsonPointer(targetPart, new URI(null, null, fragment));
            resolvePointer(pointer);
            return pointer;
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to resolve reference '%s' in '%s': %s", target, part.location, e));
        }

    }

    public static void resolvePointer(JsonPointer pointer) throws UnsupportedEncodingException, URISyntaxException {
        pointer.resolvedPart = pointer.part;
        pointer.resolvedValue = pointer.part.node;
        pointer.resolvedPath = pointer.path;

        for (int i = 0; i < pointer.path.size(); i++) {
            if (resolveIfRef(pointer)) {
                pointer.resolvedPath.addAll(pointer.path.subList(i, pointer.path.size()));
            }

            String key = pointer.path.get(i);
            pointer.resolvedValue = Util.get(pointer.resolvedValue, key);
            if (pointer.resolvedValue == null) {
                throw new RuntimeException("Unable to resolve: " + pointer.target.getFragment() + " in "
                        + pointer.part.location + " key not found: " + key);
            }
        }

        resolveIfRef(pointer);
    }

    private static boolean resolveIfRef(JsonPointer pointer) throws UnsupportedEncodingException, URISyntaxException {
        if (isRef(pointer.resolvedValue)) {
            JsonPointer resolved = resolveReference(pointer.resolvedPart, pointer.resolvedValue);
            pointer.indirections = pointer.indirections + resolved.getIndirections();
            pointer.resolvedPart = resolved.resolvedPart;
            pointer.resolvedValue = resolved.resolvedValue;
            pointer.resolvedPath = resolved.resolvedPath;
            return true;
        }
        return false;
    }

    public static JsonNode mergeExtendedRef(Serializer serializer, JsonNode ref, JsonNode value) {
        ObjectNode merged = serializer.createObjectNode();
        if (value.isObject()) {
            Iterator<String> refFields = ref.fieldNames();
            while (refFields.hasNext()) {
                String fieldName = refFields.next();
                if (!fieldName.equals("$ref")) {
                    merged.set(fieldName, ref.get(fieldName));
                }
            }

            Iterator<String> valueFields = value.fieldNames();
            while (valueFields.hasNext()) {
                String fieldName = valueFields.next();
                if (!merged.has(fieldName)) {
                    merged.set(fieldName, value.get(fieldName));
                }
            }
            return merged;
        } else {
            // TODO cant merge object and array, trow
        }

        return null;
    }

}