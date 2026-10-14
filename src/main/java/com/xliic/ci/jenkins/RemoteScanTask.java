/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import com.xliic.cicd.audit.Secret;
import com.xliic.cicd.audit.scan.CliConfig;
import com.xliic.cicd.audit.scan.CliInstaller;
import com.xliic.cicd.audit.scan.CliPlatform;
import com.xliic.cicd.audit.scan.InstalledCli;
import com.xliic.cicd.audit.scan.ScanResults;
import com.xliic.cicd.audit.scan.Scanner;
import com.xliic.cicd.common.Reference;
import com.xliic.cicd.common.TaskException;
import com.xliic.common.WorkspaceException;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.TaskListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import jenkins.security.MasterToSlaveCallable;

public class RemoteScanTask extends MasterToSlaveCallable<Void, AbortException> {
    private FilePath workspace;
    private String cliHome;
    private TaskListener listener;
    private Secret apiKey;
    private String platformUrl;
    private String logLevel;
    private boolean disableLogRedaction;
    private String defaultCollectionName;
    private String rootDirectory;
    private boolean enableGraphql;
    private boolean ignoreNetworkErrors;
    private boolean ignoreFailures;
    private Map<String, String> env;
    private String actualRepositoryName;
    private String actualBranchName;
    private String actualTagName;
    private String actualPrId;
    private String actualPrTargetBranch;

    RemoteScanTask(
            FilePath workspace,
            String cliHome,
            TaskListener listener,
            Secret apiKey,
            String platformUrl,
            String logLevel,
            boolean disableLogRedaction,
            String defaultCollectionName,
            String rootDirectory,
            boolean enableGraphql,
            boolean ignoreNetworkErrors,
            boolean ignoreFailures,
            Map<String, String> env,
            String actualRepositoryName,
            String actualBranchName,
            String actualTagName,
            String actualPrId,
            String actualPrTargetBranch) {
        this.workspace = workspace;
        this.cliHome = cliHome;
        this.listener = listener;
        this.apiKey = apiKey;
        this.platformUrl = platformUrl;
        this.logLevel = logLevel;
        this.disableLogRedaction = disableLogRedaction;
        this.defaultCollectionName = defaultCollectionName;
        this.rootDirectory = rootDirectory;
        this.enableGraphql = enableGraphql;
        this.ignoreNetworkErrors = ignoreNetworkErrors;
        this.ignoreFailures = ignoreFailures;
        this.env = env;
        this.actualRepositoryName = actualRepositoryName;
        this.actualBranchName = actualBranchName;
        this.actualTagName = actualTagName;
        this.actualPrId = actualPrId;
        this.actualPrTargetBranch = actualPrTargetBranch;
    }

    public Void call() throws AbortException {
        Path base = Paths.get(workspace.getRemote()).resolve(".42c-ast");
        CliConfig cliConfig = new CliConfig(base, base.resolve("tmp"));

        if (rootDirectory != null && !rootDirectory.equals("")) {
            workspace = new FilePath(workspace, rootDirectory);
        }
        final WorkspaceImpl scanWorkspace = new WorkspaceImpl(workspace);
        final Finder finder = new Finder(workspace);
        final LoggerImpl logger = new LoggerImpl(listener.getLogger(), logLevel);

        final Reference reference = getReference(actualBranchName, actualTagName, actualPrId, actualPrTargetBranch);
        if (reference == null) {
            throw new AbortException("Unable to retrieve branch/tag name or PR id");
        }
        if (ignoreFailures) {
            logger.info("Ignoring scan failures");
        }
        if (ignoreNetworkErrors) {
            logger.info("Ignoring network errors");
        }

        try {
            InstalledCli cli = findCli(logger, cliConfig.temp);

            Scanner scanner = new Scanner(finder, logger, apiKey, platformUrl, "Jenkins-CICD/2.0", "jenkins");
            scanner.setCli(cli);
            scanner.setTmpDir(cliConfig.temp);
            scanner.setEnv(env);
            scanner.setEnableGraphql(enableGraphql);
            scanner.setDisableLogRedaction(disableLogRedaction);
            if (defaultCollectionName != null && !defaultCollectionName.equals("")) {
                scanner.setDefaultCollectionName(defaultCollectionName);
            }

            ScanResults results = scanner.scan(scanWorkspace, actualRepositoryName, reference, apiKey.getPlainText());
            listener.getLogger().flush();
            if (!ignoreFailures) {
                if (results.failures > 0) {
                    throw new AbortException(String.format(
                            "Detected %d failure(s) in the %d API(s) scanned",
                            results.failures, results.files.size()));
                } else if (results.files.size() == 0) {
                    throw new AbortException("No OpenAPI files found.");
                }
            }
        } catch (TaskException ex) {
            if (ignoreNetworkErrors && ex.isNetworkError()) {
                logger.error(ex.getMessage());
            } else {
                ex.printStackTrace();
                throw new AbortException(ex.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        } catch (WorkspaceException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        }

        return null;
    }

    private InstalledCli findCli(LoggerImpl logger, Path tmpDir) throws AbortException {
        if (cliHome == null) {
            CliPlatform platform = CliPlatform.detect();
            String filename = platform != null ? platform.filename() : "42c-ast";
            logger.info("No 42Crunch CLI tool configured, using 42c-ast from PATH");
            return new InstalledCli(Paths.get(filename));
        }

        InstalledCli cli = new CliInstaller(logger).cliFind(new CliConfig(Paths.get(cliHome), tmpDir));
        if (cli == null) {
            throw new AbortException(String.format("42Crunch CLI not found in %s", cliHome));
        }
        return cli;
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
}
