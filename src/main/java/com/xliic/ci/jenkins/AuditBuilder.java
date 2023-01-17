/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.xliic.cicd.common.Logger;
import com.xliic.cicd.audit.Secret;

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
import hudson.remoting.VirtualChannel;
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
    private String branchName = "";
    private String tagName = "";
    private String prId = "";
    private String prTargetBranch = "";
    private String defaultCollectionName = "";
    private String rootDirectory = "";
    private String jsonReport;
    private String api_tags;
    private boolean skipLocalChecks = false;
    private boolean ignoreNetworkErrors = false;
    private boolean ignoreFailures = false;

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
    public void setSkipLocalChecks(boolean skipLocalChecks) {
        this.skipLocalChecks = skipLocalChecks;
    }

    public boolean getSkipLocalChecks() {
        return this.skipLocalChecks;
    }

    @DataBoundSetter
    public void setIgnoreNetworkErrors(boolean ignoreNetworkErrors) {
        this.ignoreNetworkErrors = ignoreNetworkErrors;
    }

    public boolean getIgnoreNetworkErrors() {
        return this.ignoreNetworkErrors;
    }

    @DataBoundSetter
    public void setIgnoreFailures(boolean ignoreFailures) {
        this.ignoreFailures = ignoreFailures;
    }

    public boolean getIgnoreFailures() {
        return this.ignoreFailures;
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

    public String getTagName() {
        return tagName;
    }

    @DataBoundSetter
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getPrId() {
        return prId;
    }

    @DataBoundSetter
    public void setPrId(String prId) {
        this.prId = prId;
    }

    public String getPrTargetBranch() {
        return prTargetBranch;
    }

    @DataBoundSetter
    public void setPrTargetBranch(String prTargetBranch) {
        this.prTargetBranch = prTargetBranch;
    }

    public String getDefaultCollectionName() {
        return defaultCollectionName;
    }

    @DataBoundSetter
    public void setDefaultCollectionName(String defaultCollectionName) {
        this.defaultCollectionName = defaultCollectionName;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    @DataBoundSetter
    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getJsonReport() {
        return jsonReport;
    }

    @DataBoundSetter
    public void setJsonReport(String jsonReport) {
        this.jsonReport = jsonReport;
    }

    public String getApiTags() {
        return api_tags;
    }

    @DataBoundSetter
    public void setApiTags(String api_tags) {
        this.api_tags = api_tags;
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

    private String expandVariable(String name, String value, Run<?, ?> build, TaskListener listener, Logger logger)
            throws IOException, InterruptedException {
        if (build instanceof AbstractBuild && value != null && !value.equals("")) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(((AbstractBuild) build).getBuildVariables());
            String expanded = env.expand(value);
            logger.debug(String.format("Expanded %s parameter '%s' to '%s'", name, value, expanded));
            return expanded;
        } else {
            return value;
        }
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        LoggerImpl logger = new LoggerImpl(listener.getLogger(), logLevel);

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
                throw new AbortException(
                        String.format("Malformed platform URL '%s': %s", trimmedUrl, e.getMessage()));
            }
        }

        String actualRepositoryName = expandVariable("repositoryName", repositoryName, run, listener, logger);
        if (actualRepositoryName == null || actualRepositoryName.length() == 0) {
            throw new AbortException(String.format("Parameter repositoryName must be set"));
        }

        String actualBranchName = expandVariable("branchName", branchName, run, listener, logger);
        String actualTagName = expandVariable("tagName", tagName, run, listener, logger);
        String actualPrId = expandVariable("prId", prId, run, listener, logger);
        String actualPrTargetBranch = expandVariable("prTargetBranch", prTargetBranch, run, listener, logger);

        ProxyConfiguration proxyConfiguration = Jenkins.get().proxy;

        VirtualChannel channel = launcher.getChannel();
        if (channel != null) {
            channel.call(new RemoteAuditTask(workspace, listener, apiKey, getPlatformUrl(), getLogLevel(),
                    getDefaultCollectionName(), getRootDirectory(), getJsonReport(), getApiTags(),
                    getSkipLocalChecks(),
                    getIgnoreNetworkErrors(), getIgnoreFailures(), getShareEveryone(),
                    minScore, proxyConfiguration, actualRepositoryName, actualBranchName, actualTagName,
                    actualPrId,
                    actualPrTargetBranch));
        } else {
            throw new AbortException("Unable to get channel to launch AuditTask");
        }
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

    static class SecretImpl implements Secret, Serializable {
        private hudson.util.Secret secret;

        public SecretImpl(hudson.util.Secret secret) {
            this.secret = secret;
        }

        @Override
        public String getPlainText() {
            return secret.getPlainText();
        }
    }
}
