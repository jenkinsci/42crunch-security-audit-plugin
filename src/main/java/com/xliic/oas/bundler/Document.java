package com.xliic.oas.bundler;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

public class Document {
    Part root;
    Parts parts = new Parts();
    URI base;

    public Document(URI location, JsonNode root) {
        Path parent = new File(location.getPath()).toPath().getParent();
        if (parent == null) {
            throw new RuntimeException("Unable to find parent folder of: " + location);
        }
        this.base = parent.toUri();
        this.root = new Part(location, root);
        this.parts.put(location, this.root);
    }

    public static Part getTargetPart(Part part, URI refUri) throws URISyntaxException {
        URI targetPartUri = getTargetPartUri(part, refUri);
        return part.getDocument().getPart(targetPartUri);
    }

    public static URI getTargetPartUri(Part part, URI refUri) throws URISyntaxException {
        URI locationWithFragment = part.location.resolve(refUri);
        File file = new File(locationWithFragment.getPath());
        return file.toURI();
    }

    public Part createPart(URI location, JsonNode node) {
        Part part = new Part(location, node);
        parts.put(location, part);
        return part;
    }

    public Part getPart(URI location) {
        File file = new File(location.getPath());
        return parts.get(file.toURI());
    }

    public class Part {
        public final URI location;
        public final JsonNode node;

        private Part(URI location, JsonNode node) {
            this.location = location;
            this.node = node;
        }

        public URI resolve(URI ref) {
            return this.location.resolve(ref);
        }

        public Document getDocument() {
            return Document.this;
        }

        public URI getFilename() {
            return Document.this.base.relativize(this.location);
        }
    }

    @SuppressWarnings("serial")
    public static class Parts extends HashMap<URI, Part> {

    }
}
