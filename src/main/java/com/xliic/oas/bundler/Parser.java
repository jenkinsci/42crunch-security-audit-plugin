/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.oas.bundler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xliic.ci.audit.Workspace;

public class Parser {
    ObjectMapper mapper;
    private Workspace workspace;

    public Parser(Workspace workspace) {
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.workspace = workspace;
    }

    public Document parse(String filename)
            throws JsonProcessingException, IOException, URISyntaxException, InterruptedException {
        File file = new File(filename);
        JsonNode root = readTree(filename);
        Document document = new Document(file.getAbsoluteFile().toURI(), root);
        crawl(document, document.root, document.root.node);
        return document;
    }

    private JsonNode readTree(String filename)
            throws JsonMappingException, JsonProcessingException, IOException, InterruptedException {
        return mapper.readTree(workspace.read(filename));
    }

    public void crawl(final Document document, final Document.Part part, final JsonNode node)
            throws JsonProcessingException, IOException, URISyntaxException {
        if (Resolver.isExternalRef(node)) {
            Document.Part newPart = loadNewPart(document, part, node);
            if (newPart != null) {
                crawl(document, newPart, newPart.node);
            }
        } else if (node.isContainerNode()) {
            for (JsonNode child : node) {
                crawl(document, part, child);
            }
        }
    }

    public Document.Part loadNewPart(Document document, Document.Part part, JsonNode node)
            throws JsonProcessingException, IOException, URISyntaxException {
        String ref = node.get("$ref").asText();
        try {
            URI refUri = new URI(ref);
            URI fileUri = Document.getTargetPartUri(part, refUri);
            if (document.parts.containsKey(fileUri)) {
                return null;
            }
            JsonNode root = readTree(new File(fileUri).getAbsolutePath());
            return document.createPart(fileUri, root);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to resolve reference '%s' in '%s': %s", ref, part.location, e));
        }

    }
}