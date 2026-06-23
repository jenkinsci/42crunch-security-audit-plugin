/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import com.xliic.cicd.audit.scan.CliConfig;
import com.xliic.cicd.audit.scan.CliInstaller;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.TaskListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import jenkins.security.MasterToSlaveCallable;

public class RemoteInstallCliTask extends MasterToSlaveCallable<Void, AbortException> {
    private FilePath workspace;
    private TaskListener listener;
    private String logLevel;
    private String repository;
    private String version;

    RemoteInstallCliTask(FilePath workspace, TaskListener listener, String logLevel, String repository,
            String version) {
        this.workspace = workspace;
        this.listener = listener;
        this.logLevel = logLevel;
        this.repository = repository;
        this.version = version;
    }

    public Void call() throws AbortException {
        final LoggerImpl logger = new LoggerImpl(listener.getLogger(), logLevel);

        Path base = Paths.get(workspace.getRemote()).resolve(".42c-ast");
        CliConfig cliConfig = new CliConfig(base, base.resolve("tmp"));

        try {
            CliInstaller installer = new CliInstaller(logger);
            installer.ensureCliInstalled(repository, version, cliConfig);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        }

        listener.getLogger().flush();
        return null;
    }
}
