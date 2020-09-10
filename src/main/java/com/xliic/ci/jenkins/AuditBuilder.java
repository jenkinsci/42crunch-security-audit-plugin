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
import java.util.List;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import com.xliic.cicd.audit.AuditException;
import com.xliic.cicd.audit.Auditor;
import com.xliic.cicd.audit.Logger;
import com.xliic.cicd.audit.Secret;
import com.xliic.common.Workspace;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class AuditBuilder extends Builder implements SimpleBuildStep {

    private int minScore;
    private String credentialsId;
    private String collectionName;

    @DataBoundConstructor
    public AuditBuilder(String credentialsId, int minScore, String collectionName) {
        this.credentialsId = credentialsId;
        this.minScore = minScore;
        this.collectionName = collectionName;
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

    public String getCollectionName() {
        return collectionName;
    }

    @DataBoundSetter
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        ApiKey credential = CredentialsProvider.findCredentialById(credentialsId, ApiKey.class, run,
                Collections.<DomainRequirement>emptyList());

        if (credential == null) {
            throw new AbortException("Unable to load API Token credential: " + credentialsId);
        }

        Secret apiKey = new SecretImpl(credential.getApiKey());

        if (!apiKey.getPlainText().matches(ApiKey.UUID_PATTERN)) {
            throw new AbortException("Invalid format of API Token");
        }

        final LoggerImpl logger = new LoggerImpl(listener.getLogger());
        final WorkspaceImpl auditWorkspace = new WorkspaceImpl(workspace);
        final Finder finder = new Finder(workspace);

        Auditor auditor = new Auditor(finder, logger, apiKey);

        ProxyConfiguration proxyConfiguration = Jenkins.get().proxy;
        if (proxyConfiguration != null) {
            auditor.setProxy(proxyConfiguration.name, proxyConfiguration.port);
        }

        try {
            String failure = auditor.audit(auditWorkspace, collectionName, minScore);
            if (failure != null) {
                throw new AbortException(failure);

            }
        } catch (AuditException ex) {
            throw new AbortException(ex.getMessage());
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
    }

    static class LoggerImpl implements Logger {
        private PrintStream logger;

        LoggerImpl(final PrintStream logger) {
            this.logger = logger;
        }

        @Override
        public void log(final String message) {
            logger.println(message);
        }

        @Override
        public void progress(String message) {
            logger.println(message);
        }

        @Override
        public void report(String message) {
            logger.println(message);
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
