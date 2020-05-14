/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.oas.bundler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Serializer {
    private ObjectMapper mapper;

    public Serializer() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String serialize(Document document) throws JsonProcessingException {
        return mapper.writeValueAsString(document.root.node);
    }

    public ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }
}