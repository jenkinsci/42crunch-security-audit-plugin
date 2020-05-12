/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.xliic.ci.audit.AuditException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

public class ConfigReader {
    public static final String CONFIG_FILE_NAME = "42c-conf.yaml";

    public static Config read(String data)
            throws RuntimeException, JsonParseException, JsonMappingException, IOException, AuditException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        Set<ValidationMessage> messages = validate(mapper, data);
        if (messages != null && messages.size() > 0) {
            StringBuffer buffer = new StringBuffer();
            for (ValidationMessage message : messages) {
                buffer.append(message.toString());
                buffer.append("\n");
            }
            throw new AuditException(
                    String.format("Config file %s failed schema validation: %s", CONFIG_FILE_NAME, buffer.toString()));
        }

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Config config = mapper.readValue(data, Config.class);

        // default discovery settings
        if (config.getAudit().getDiscovery() == null) {
            config.getAudit().setDiscovery(Discovery.defaultDiscovery());
        }

        // default empty mapping
        if (config.getAudit().getMapping() == null) {
            config.getAudit().setMapping(new Mapping());
        }

        return config;
    }

    public static Set<ValidationMessage> validate(ObjectMapper mapper, String data) throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                .objectMapper(mapper).build();
        JsonSchema schema = factory.getSchema(ConfigReader.class.getClassLoader().getResourceAsStream("schema.json"));

        JsonNode jsonNode = mapper.readTree(data);
        return schema.validate(jsonNode);
    }

}
