/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import com.xliic.cicd.audit.AuditException;
import com.xliic.cicd.audit.AuditResults;
import com.xliic.cicd.audit.Auditor;
import com.xliic.cicd.audit.Logger;
import com.xliic.cicd.audit.Secret;
import com.xliic.cicd.audit.SharingType;
import com.xliic.common.Workspace;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class AuditBuilder extends Builder implements SimpleBuildStep {

    private int minScore = 75;
    private String credentialsId;
    private String platformUrl = "https://platform.42crunch.com";
    private String logLevel;
    private String repositoryName = "${GIT_URL}";
    private String branchName = "${GIT_LOCAL_BRANCH}";
    private String shareEveryone;

    @DataBoundConstructor
    public AuditBuilder(String credentialsId, int minScore, String platformUrl) {
        this.credentialsId = credentialsId;
        this.minScore = minScore;
        this.platformUrl = platformUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public int getMinScore() {
        return minScore;
    }

    @DataBoundSetter
    public void setMinScore(int minScore) {
        this.minScore = minScore;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    @DataBoundSetter
    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public String getLogLevel() {
        if (logLevel == null) {
            return "INFO";
        }
        return logLevel;
    }

    @DataBoundSetter
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    @DataBoundSetter
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getBranchName() {
        return branchName;
    }

    @DataBoundSetter
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getShareEveryone() {
        if (shareEveryone == null) {
            return "OFF";
        }
        return shareEveryone;
    }

    @DataBoundSetter
    public void setShareEveryone(String shareEveryone) {
        this.shareEveryone = shareEveryone;
    }

    private String actualBranchName(Run<?, ?> build, TaskListener listener, Logger logger)
            throws IOException, InterruptedException {
        if (build instanceof AbstractBuild) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(((AbstractBuild) build).getBuildVariables());
            String expanded = env.expand(branchName);
            logger.debug(String.format("Expanded branchName parameter '%s' to '%s'", branchName, expanded));
            return expanded;
        } else {
            return branchName;
        }
    }

    private String actualRepositoryName(Run<?, ?> build, TaskListener listener, Logger logger)
            throws IOException, InterruptedException {
        if (build instanceof AbstractBuild) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(((AbstractBuild) build).getBuildVariables());
            String expanded = env.expand(repositoryName);
            logger.debug(String.format("Expanded repositoryName parameter '%s' to '%s'", repositoryName, expanded));
            return expanded;
        } else {
            return repositoryName;
        }
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        final LoggerImpl logger = new LoggerImpl(listener.getLogger(), getLogLevel());

        ApiKey credential = CredentialsProvider.findCredentialById(credentialsId, ApiKey.class, run,
                Collections.<DomainRequirement>emptyList());

        if (credential == null) {
            throw new AbortException("Unable to load API Token credential: " + credentialsId);
        }

        Secret apiKey = new SecretImpl(credential.getApiKey());

        if (!apiKey.getPlainText().matches(ApiKey.UUID_PATTERN)) {
            throw new AbortException("Invalid format of API Token");
        }

        String trimmedUrl = Util.fixEmptyAndTrim(platformUrl);
        if (trimmedUrl != null) {
            try {
                URI url = new URI(trimmedUrl);
                if (url.getScheme() == null || !url.getScheme().equals("https")) {
                    throw new AbortException(
                            String.format("Bad platform URL '%s': only https:// URLs are allowed", url));
                }
                this.platformUrl = String.format("%s://%s", url.getScheme(), url.getRawAuthority());
            } catch (URISyntaxException e) {
                throw new AbortException(String.format("Malformed platform URL '%s': %s", trimmedUrl, e.getMessage()));
            }
        }

        String actualRepositoryName = actualRepositoryName(run, listener, logger);
        if (actualRepositoryName == null || actualRepositoryName.length() == 0) {
            throw new AbortException(String.format("Parameter repositoryName must be set"));
        }

        String actualBranchName = actualBranchName(run, listener, logger);
        if (actualBranchName == null || actualBranchName.length() == 0) {
            throw new AbortException(String.format("Parameter branchName must be set"));
        }

        final WorkspaceImpl auditWorkspace = new WorkspaceImpl(workspace);
        final Finder finder = new Finder(workspace);

        Auditor auditor = new Auditor(finder, logger, apiKey, platformUrl, "Jenkins-CICD/2.0", "jenkins");

        auditor.setMinScore(minScore);

        if (getShareEveryone().equals("READ_ONLY")) {
            auditor.setShareEveryone(SharingType.READ_ONLY);
        } else if (getShareEveryone().equals("READ_WRITE")) {
            auditor.setShareEveryone(SharingType.READ_WRITE);
        }

        ProxyConfiguration proxyConfiguration = Jenkins.get().proxy;
        if (proxyConfiguration != null) {
            auditor.setProxy(proxyConfiguration.name, proxyConfiguration.port);
        }

        try {
            AuditResults results = auditor.audit(auditWorkspace, actualRepositoryName, actualBranchName);
            displayReport(results, logger, auditWorkspace);
            if (results.failures > 0) {
                throw new AbortException(String.format("Detected %d failure(s) in the %d OpenAPI file(s) checked",
                        results.failures, results.summary.size()));
            } else if (results.summary.size() == 0) {
                throw new AbortException("No OpenAPI files found.");
            }
        } catch (AuditException ex) {
            throw new AbortException(ex.getMessage());
        }
    }

    private void displayReport(AuditResults results, Logger logger, Workspace workspace) {
        results.summary.forEach((file, summary) -> {
            logger.error(String.format("Audited %s, the API score is %d", workspace.relativize(file).getPath(),
                    summary.score));
            if (summary.failures.length > 0) {
                for (String failure : summary.failures) {
                    logger.error("    " + failure);
                }
            } else {
                logger.error("    No blocking issues found.");
            }
            if (summary.reportUrl != null) {
                logger.error("    Details:");
                logger.error(String.format("    %s", summary.reportUrl));
            }
            logger.error("");
        });
    }

    @Symbol("audit")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public String getDisplayName() {
            return Messages.descriptor_displayName();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item ancestor,
                @QueryParameter String credentialsId) {

            StandardListBoxModel result = new StandardListBoxModel();

            if (ancestor == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!ancestor.hasPermission(Item.EXTENDED_READ)
                        && !ancestor.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            return result.includeMatchingAs(ACL.SYSTEM, ancestor, ApiKey.class,
                    Collections.<DomainRequirement>emptyList(), CredentialsMatchers.always())
                    .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckPlatformUrl(@QueryParameter String value) {
            String trimmedUrl = Util.fixEmptyAndTrim(value);
            if (trimmedUrl != null) {
                try {
                    new URI(trimmedUrl);
                } catch (URISyntaxException e) {
                    return FormValidation.error("Malformed URL");
                }
            }

            return FormValidation.ok();
        }
    }

    static class LoggerImpl implements Logger {
        private PrintStream logger;
        private int level;

        LoggerImpl(final PrintStream logger, String logLevel) {
            this.logger = logger;
            switch (logLevel.toUpperCase()) {
                case "FATAL":
                    this.level = Logger.Level.FATAL;
                    break;
                case "ERROR":
                    this.level = Logger.Level.ERROR;
                    break;
                case "WARN":
                    this.level = Logger.Level.WARN;
                    break;
                case "INFO":
                    this.level = Logger.Level.INFO;
                    break;
                case "DEBUG":
                    this.level = Logger.Level.DEBUG;
                    break;
                default:
                    logger.println("Unknown log level specified, setting log level to INFO");
                    this.level = Logger.Level.INFO;
            }
        }

        @Override
        public void setLevel(int level) {
            this.level = level;
        }

        @Override
        public void fatal(String message) {
            if (Logger.Level.FATAL >= level) {
                logger.println(message);
            }
        }

        @Override
        public void error(String message) {
            if (Logger.Level.ERROR >= level) {
                logger.println(message);
            }
        }

        @Override
        public void warn(String message) {
            if (Logger.Level.WARN >= level) {
                logger.println(message);
            }
        }

        @Override
        public void info(String message) {
            if (Logger.Level.INFO >= level) {
                logger.println(message);
            }
        }

        @Override
        public void debug(String message) {
            if (Logger.Level.DEBUG >= level) {
                logger.println(message);
            }
        }
    }

    static class SecretImpl implements Secret {
        private hudson.util.Secret secret;

        public SecretImpl(hudson.util.Secret secret) {
            this.secret = secret;
        }

        @Override
        public String getPlainText() {
            return secret.getPlainText();
        }
    }

    static class WorkspaceImpl implements Workspace {

        private FilePath workspace;

        WorkspaceImpl(FilePath workspace) {
            this.workspace = workspace;
        }

        @Override
        public String read(URI uri) throws IOException, InterruptedException {

            FilePath filepath = new FilePath(workspace, uri.getPath());

            InputStream is = filepath.read();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int read;
            byte[] data = new byte[16384];
            while ((read = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }
            buffer.flush();

            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);

        }

        @Override
        public boolean exists(URI file) throws IOException, InterruptedException {
            FilePath filepath = new FilePath(workspace, file.getPath());
            return filepath.exists();
        }

        @Override
        public URI resolve(String filename) {
            try {
                String safeFilename = new URI(null, filename, null).getRawSchemeSpecificPart();
                return workspace.toURI().resolve(safeFilename);
            } catch (IOException | InterruptedException | URISyntaxException e) {
                throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
            }
        }

        @Override
        public URI relativize(URI uri) {
            try {
                return workspace.toURI().relativize(uri);
            } catch (IOException | InterruptedException e) {
                throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
            }
        }

    }
}
