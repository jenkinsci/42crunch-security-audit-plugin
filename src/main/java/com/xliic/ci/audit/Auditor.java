/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xliic.ci.audit.client.Client;
import com.xliic.ci.audit.client.ClientConstants;
import com.xliic.ci.audit.client.RemoteApi;
import com.xliic.ci.audit.client.RemoteApiMap;
import com.xliic.ci.audit.config.Config;
import com.xliic.ci.audit.config.ConfigReader;
import com.xliic.ci.audit.config.Discovery;
import com.xliic.ci.audit.config.Mapping;
import com.xliic.ci.audit.model.OpenApiFile;
import com.xliic.ci.audit.model.api.Api;
import com.xliic.ci.audit.model.api.ApiCollection;
import com.xliic.ci.audit.model.api.ApiCollections;
import com.xliic.ci.audit.model.api.Maybe;
import com.xliic.ci.audit.model.assessment.AssessmentReport;
import com.xliic.ci.audit.model.assessment.AssessmentResponse;

public class Auditor {
    static int MAX_NAME_LEN = 64;
    private OpenApiFinder finder;
    private Logger logger;
    private String apiKey;
    private ResultCollector resultCollector;

    public Auditor(OpenApiFinder finder, Logger logger, String apiKey) {
        this.finder = finder;
        this.logger = logger;
        this.apiKey = apiKey;
    }

    public void setResultCollector(ResultCollector resultCollector) {
        this.resultCollector = resultCollector;
    }

    public String audit(Workspace workspace, String collectionName, int minScore)
            throws IOException, InterruptedException, AuditException {

        Config config;
        if (workspace.exists(ConfigReader.CONFIG_FILE_NAME)) {
            try {
                config = ConfigReader.read(workspace.read(ConfigReader.CONFIG_FILE_NAME));
            } catch (final IOException e) {
                throw new AuditException("Failed to read config file", e);
            }
        } else {
            config = Config.createDefault();
        }

        final Discovery discovery = config.getAudit().getDiscovery();
        final Mapping mapping = config.getAudit().getMapping();
        final FailureConditions failureConditions = new FailureConditions(minScore, config.getAudit().getFailOn());

        final RemoteApiMap uploaded = new RemoteApiMap();
        // discover and upload apis
        if (discovery.isEnabled()) {
            uploaded.putAll(uploadDiscoveredFiles(workspace, finder, collectionName, discovery.getSearch(), mapping));
        }

        // upload mapped files
        uploaded.putAll(uploadMappedFiles(workspace, mapping));

        HashMap<String, Summary> report = readAssessment(uploaded, failureConditions);

        collectResults(report);
        displayReport(report);

        int totalFiles = report.size();
        int filesWithFailures = countFilesWithFailures(report);

        if (filesWithFailures > 0) {
            return String.format("Detected %d failure(s) in the %d OpenAPI file(s) checked", filesWithFailures,
                    totalFiles);

        }

        if (totalFiles == 0) {
            return "No OpenAPI files found.";
        }

        return null;
    }

    private RemoteApiMap uploadDiscoveredFiles(Workspace workspace, OpenApiFinder finder, String collectionName,
            String[] search, Mapping mapping) throws IOException, InterruptedException, AuditException {
        String[] openapiFilenames = discoverOpenApiFiles(workspace, finder, search, mapping);
        String collectionId = createOrFindCollectionId(makeName(collectionName));
        return uploadFilesToCollection(openapiFilenames, workspace, collectionId);
    }

    private RemoteApiMap uploadMappedFiles(Workspace workspace, Mapping mapping) throws IOException, AuditException {
        RemoteApiMap uploaded = new RemoteApiMap();

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String filename = entry.getKey();
            String apiId = entry.getValue();
            String json = parseFile(filename, workspace);
            Maybe<RemoteApi> api = Client.updateApi(apiId, json, apiKey, logger);
            uploaded.put(filename, api);
        }

        return uploaded;
    }

    private HashMap<String, Summary> readAssessment(RemoteApiMap uploaded, FailureConditions failureConditions)
            throws IOException {
        HashMap<String, Summary> report = new HashMap<String, Summary>();
        for (Map.Entry<String, Maybe<RemoteApi>> entry : uploaded.entrySet()) {
            String filename = entry.getKey();
            Maybe<RemoteApi> api = entry.getValue();
            Maybe<AssessmentResponse> assessment = Client.readAssessment(api, apiKey, logger);
            Summary summary = checkAssessment(api, assessment, failureConditions);
            report.put(filename, summary);
        }
        return report;
    }

    private AssessmentReport decodeReport(String data) throws JsonParseException, JsonMappingException, IOException {
        byte[] decoded = Base64.getDecoder().decode(data);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(decoded, AssessmentReport.class);
    }

    private void collectResults(Map<String, Summary> report) {
        if (this.resultCollector != null) {
            report.forEach((filename, summary) -> {
                String reportUrl = null;
                if (summary.api.isOk()) {
                    reportUrl = String.format("%s/apis/%s/security-audit-report", ClientConstants.PLATFORM_URL,
                            summary.api.getResult().apiId);
                }
                this.resultCollector.collect(filename, summary.failures, reportUrl);
            });
        }
    }

    private void displayReport(Map<String, Summary> report) {
        report.forEach((filename, summary) -> {
            logger.log(String.format("Audited %s, the API score is %d", filename, summary.score));
            if (summary.failures.length > 0) {
                for (String failure : summary.failures) {
                    logger.log("    " + failure);
                }
            } else {
                logger.log("    No blocking issues found.");
            }
            if (summary.api.isOk()) {
                logger.log("    Details:");
                logger.log(String.format("    %s/apis/%s/security-audit-report", ClientConstants.PLATFORM_URL,
                        summary.api.getResult().apiId));
            }
            logger.log("");
        });
    }

    public int countFilesWithFailures(Map<String, Summary> report) {
        int failures = 0;
        for (Summary summary : report.values()) {
            if (summary.failures.length > 0) {
                failures++;
            }
        }
        return failures;
    }

    private String[] discoverOpenApiFiles(Workspace workspace, OpenApiFinder finder, String[] search, Mapping mapping)
            throws IOException, InterruptedException, AuditException {
        ArrayList<String> found = new ArrayList<String>();

        String[] filenames = findOpenapiFiles(workspace, finder, search);
        logger.log(String.format("Files matching search criteria: %s", String.join(", ", filenames)));
        for (String filename : filenames) {
            if (isOpenApiFile(filename, workspace) && !mapping.containsKey(filename)) {
                found.add(filename);
            }
        }
        logger.log(String.format("Discovered OpenAPI files: %s", String.join(", ", found)));
        return found.toArray(new String[found.size()]);
    }

    private String[] findOpenapiFiles(Workspace workspace, OpenApiFinder finder, String[] search)
            throws IOException, InterruptedException, AuditException {
        finder.setPatterns(search);
        String[] openApiFiles = finder.find();
        return openApiFiles;
    }

    private static boolean isOpenApiFile(String filename, Workspace workspace)
            throws JsonParseException, JsonMappingException, IOException, InterruptedException {
        ObjectMapper mapper;
        if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OpenApiFile openApiFile = mapper.readValue(workspace.read(filename), OpenApiFile.class);
        return openApiFile.isOpenApi();
    }

    private String createOrFindCollectionId(String collectionName) throws AuditException, IOException {

        // check existing collections to see if collection with collectionName already
        // exists
        Maybe<ApiCollections> collections = Client.listCollections(apiKey, logger);
        if (collections.isError()) {
            throw new AuditException("Unable to list collection: " + collections.getError().getMessage());
        }
        for (ApiCollections.ApiCollection collection : collections.getResult().list) {
            if (collection.desc.name.equals(collectionName)) {
                return collection.desc.id;
            }
        }

        // no collection collectionName found, create a new one
        Maybe<ApiCollections.ApiCollection> collection = Client.createCollection(collectionName, apiKey, logger);
        if (collection.isError()) {
            throw new AuditException("Unable to create collection: " + collection.getError().getMessage());
        }
        return collection.getResult().desc.id;
    }

    private RemoteApiMap uploadFilesToCollection(String[] filenames, Workspace workspace, String collectionId)
            throws IOException, AuditException {
        RemoteApiMap uploaded = new RemoteApiMap();

        purgeCollection(collectionId);
        for (String filename : filenames) {
            String json = parseFile(filename, workspace);
            String apiName = makeName(filename);
            Maybe<RemoteApi> api = Client.createApi(collectionId, apiName, json, apiKey, logger);
            uploaded.put(filename, api);
        }

        return uploaded;
    }

    private void purgeCollection(String collectionId) throws IOException, AuditException {
        Maybe<ApiCollection> collection = Client.listCollection(collectionId, apiKey, logger);
        if (collection.isError()) {
            throw new AuditException("Unable to read collection: " + collection.getError().getMessage());
        }
        for (Api api : collection.getResult().list) {
            Maybe<String> deleted = Client.deleteApi(api.desc.id, apiKey, logger);
            if (deleted.isError()) {
                throw new AuditException("Unable to delete collection: " + deleted.getError().getMessage());
            }
        }
    }

    private String makeName(String filename) {
        String mangled = filename.replaceAll("[^A-Za-z0-9_\\-\\.\\ ]", "-");
        if (mangled.length() > MAX_NAME_LEN) {
            return mangled.substring(0, MAX_NAME_LEN);
        }
        return mangled;
    }

    private String parseFile(String filename, Workspace workspace) throws AuditException {
        try {
            String data = workspace.read(filename);

            if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                Object obj = yamlReader.readValue(data, Object.class);

                ObjectMapper jsonWriter = new ObjectMapper();
                return jsonWriter.writeValueAsString(obj);
            }
            return data;
        } catch (Exception e) {
            throw new AuditException(String.format("Failed to parse file: %s %s", filename, e), e);
        }
    }

    private Summary checkAssessment(Maybe<RemoteApi> api, Maybe<AssessmentResponse> assessment,
            FailureConditions conditions) throws JsonParseException, JsonMappingException, IOException {
        if (assessment.isError()) {
            return new Summary(api, 0, new String[] { assessment.getError().getMessage() });
        }

        AssessmentReport assessmentReport = decodeReport(assessment.getResult().data);
        FailureChecker checker = new FailureChecker();
        ArrayList<String> failures = checker.checkAssessment(assessment.getResult(), assessmentReport, conditions);
        return new Summary(api, Math.round(assessment.getResult().attr.data.grade), failures.toArray(new String[0]));
    }

}
