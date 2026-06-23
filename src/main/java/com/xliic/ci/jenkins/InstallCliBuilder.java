/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class InstallCliBuilder extends Builder implements SimpleBuildStep {

    private String logLevel;
    private String cliRepository = "https://repo.42crunch.com/downloads";
    private String cliVersion = "0.0.0";

    @DataBoundConstructor
    public InstallCliBuilder() {
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

    public String getCliRepository() {
        return cliRepository;
    }

    @DataBoundSetter
    public void setCliRepository(String cliRepository) {
        this.cliRepository = cliRepository;
    }

    public String getCliVersion() {
        return cliVersion;
    }

    @DataBoundSetter
    public void setCliVersion(String cliVersion) {
        this.cliVersion = cliVersion;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        String repository = Util.fixEmptyAndTrim(cliRepository);
        if (repository == null) {
            throw new AbortException("Parameter cliRepository must be set");
        }

        String version = Util.fixEmptyAndTrim(cliVersion);
        if (version == null) {
            version = "0.0.0";
        }

        VirtualChannel channel = launcher.getChannel();
        if (channel != null) {
            channel.call(new RemoteInstallCliTask(workspace, listener, getLogLevel(), repository, version));
        } else {
            throw new AbortException("Unable to get channel to launch InstallCliTask");
        }
    }

    @Symbol("installCli")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public String getDisplayName() {
            return Messages.descriptor_installCliDisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public FormValidation doCheckCliRepository(@QueryParameter String value) {
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
