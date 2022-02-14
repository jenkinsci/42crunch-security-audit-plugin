package com.xliic.ci.jenkins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import com.xliic.cicd.common.TaskException;
import com.xliic.cicd.audit.AuditResults;
import com.xliic.cicd.audit.Auditor;
import com.xliic.cicd.common.Logger;
import com.xliic.cicd.common.Reference;
import com.xliic.cicd.audit.Secret;
import com.xliic.cicd.audit.SharingType;
import com.xliic.common.Workspace;

import hudson.model.TaskListener;
import hudson.AbortException;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import jenkins.security.MasterToSlaveCallable;

public class RemoteAuditTask extends MasterToSlaveCallable<Void, AbortException> {
    private TaskListener listener;
    private FilePath workspace;
    private String logLevel;
    private Secret apiKey;
    private String platformUrl;
    private String shareEveryone;
    private int minScore;
    private ProxyConfiguration proxyConfiguration;
    private String actualRepositoryName;
    private String actualBranchName;
    private String actualTagName;
    private String actualPrId;
    private String actualPrTargetBranch;
    private String defaultCollectionName;
    private String rootDirectory;

    RemoteAuditTask(FilePath workspace, TaskListener listener, Secret apiKey, String platformUrl, String logLevel,
            String defaultCollectionName, String rootDirectory,
            String shareEveryone, int minScore, ProxyConfiguration proxyConfiguration, String actualRepositoryName,
            String actualBranchName, String actualTagName, String actualPrId, String actualPrTargetBranch) {
        this.listener = listener;
        this.workspace = workspace;
        this.logLevel = logLevel;
        this.apiKey = apiKey;
        this.platformUrl = platformUrl;
        this.shareEveryone = shareEveryone;
        this.minScore = minScore;
        this.proxyConfiguration = proxyConfiguration;
        this.actualRepositoryName = actualRepositoryName;
        this.actualBranchName = actualBranchName;
        this.actualTagName = actualTagName;
        this.actualPrId = actualPrId;
        this.actualPrTargetBranch = actualPrTargetBranch;
        this.defaultCollectionName = defaultCollectionName;
        this.rootDirectory = rootDirectory;
    }

    public Void call() throws AbortException {
        if (rootDirectory != null && !rootDirectory.equals("")) {
            workspace = new FilePath(workspace, rootDirectory);
        }
        final WorkspaceImpl auditWorkspace = new WorkspaceImpl(workspace);
        final Finder finder = new Finder(workspace);
        final LoggerImpl logger = new LoggerImpl(listener.getLogger(), logLevel);

        Auditor auditor = new Auditor(finder, logger, apiKey, platformUrl, "Jenkins-CICD/2.0", "jenkins");

        auditor.setMinScore(minScore);
        auditor.setDefaultCollectionName(defaultCollectionName);

        if (shareEveryone.equals("READ_ONLY")) {
            auditor.setShareEveryone(SharingType.READ_ONLY);
        } else if (shareEveryone.equals("READ_WRITE")) {
            auditor.setShareEveryone(SharingType.READ_WRITE);
        }

        if (proxyConfiguration != null) {
            auditor.setProxy(proxyConfiguration.name, proxyConfiguration.port);
        }

        final Reference reference = getReference(actualBranchName, actualTagName, actualPrId, actualPrTargetBranch);

        if (reference == null) {
            throw new AbortException("Unable to retrieve branch/tag name or PR id");
        }

        try {
            AuditResults results = auditor.audit(auditWorkspace, actualRepositoryName, reference);
            displayReport(results, logger, auditWorkspace);
            if (results.failures > 0) {
                throw new AbortException(String.format("Detected %d failure(s) in the %d OpenAPI file(s) checked",
                        results.failures, results.summary.size()));
            } else if (results.summary.size() == 0) {
                throw new AbortException("No OpenAPI files found.");
            }
        } catch (TaskException ex) {
            throw new AbortException(ex.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        }

        return null;
    }

    private Reference getReference(String branch, String tag, String prId, String prTargetBranch) {
        if (branch != null) {
            return Reference.branch(branch);
        }

        if (tag != null) {
            return Reference.tag(tag);
        }

        if (prId != null && prTargetBranch != null) {
            return Reference.pr(prId, prTargetBranch);
        }

        return null;
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
