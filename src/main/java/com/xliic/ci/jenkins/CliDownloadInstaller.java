/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import com.xliic.cicd.audit.scan.CliConfig;
import com.xliic.cicd.audit.scan.CliInstaller;
import com.xliic.cicd.audit.scan.InstalledCli;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Downloads the latest 42c-ast CLI listed in the repository manifest into the
 * tool directory on the agent.
 */
public class CliDownloadInstaller extends ToolInstaller {

    public static final String DEFAULT_REPOSITORY = "https://repo.42crunch.com/downloads";

    private static final long LOCK_TIMEOUT_MINUTES = 5;
    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private String repository = DEFAULT_REPOSITORY;

    @DataBoundConstructor
    public CliDownloadInstaller(String label) {
        super(label);
    }

    public String getRepository() {
        return repository;
    }

    @DataBoundSetter
    public void setRepository(String repository) {
        String trimmed = Util.fixEmptyAndTrim(repository);
        this.repository = trimmed != null ? trimmed : DEFAULT_REPOSITORY;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log)
            throws IOException, InterruptedException {
        FilePath dir = preferredLocation(tool, node);
        dir.mkdirs();

        // Serialize installs into the same directory (parallel stages, executors)
        ReentrantLock lock = LOCKS.computeIfAbsent(dir.getRemote(), k -> new ReentrantLock());
        if (!lock.tryLock(LOCK_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            throw new IOException("Timed out waiting for 42Crunch CLI installation in " + dir);
        }
        try {
            dir.act(new Downloader(getRepository(), log));
        } finally {
            lock.unlock();
        }
        return dir;
    }

    static final class Downloader extends MasterToSlaveFileCallable<Void> {
        private static final long serialVersionUID = 1L;

        private final String repository;
        private final TaskListener listener;

        Downloader(String repository, TaskListener listener) {
            this.repository = repository;
            this.listener = listener;
        }

        @Override
        public Void invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
            LoggerImpl logger = new LoggerImpl(listener.getLogger(), "INFO");
            CliConfig config = new CliConfig(dir.toPath(), dir.toPath().resolve(".tmp"));
            CliInstaller installer = new CliInstaller(logger);
            try {
                installer.ensureCliInstalled(repository, config);
            } catch (IOException e) {
                InstalledCli existing = installer.cliFind(config);
                if (existing == null) {
                    throw e;
                }
                logger.warn(String.format("Failed to update 42Crunch CLI, using existing %s: %s",
                        existing.location, e.getMessage()));
            }
            return null;
        }
    }

    @Symbol("crunchCliDownload")
    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<CliDownloadInstaller> {

        @Override
        public String getDisplayName() {
            return Messages.descriptor_cliDownloadInstallerDisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == CliInstallation.class;
        }

        @POST
        public FormValidation doCheckRepository(@QueryParameter String value) {
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
}
