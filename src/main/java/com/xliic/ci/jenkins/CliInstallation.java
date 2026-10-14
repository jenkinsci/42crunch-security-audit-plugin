/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The 42c-ast CLI tool. The home is the directory containing the 42c-ast
 * binary, either set manually or populated by {@link CliDownloadInstaller}.
 */
public class CliInstallation extends ToolInstallation
        implements NodeSpecific<CliInstallation>, EnvironmentSpecific<CliInstallation> {

    public static final String CLI_HOME = "CRUNCH_CLI_HOME";

    @DataBoundConstructor
    public CliInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public CliInstallation forEnvironment(EnvVars env) {
        return new CliInstallation(getName(), env.expand(getHome()), getProperties().toList());
    }

    @Override
    public CliInstallation forNode(@NonNull Node node, TaskListener log) throws IOException, InterruptedException {
        return new CliInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        if (getHome() != null) {
            env.put(CLI_HOME, getHome());
        }
    }

    public static CliInstallation get(String name) {
        DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(DescriptorImpl.class);
        for (CliInstallation installation : descriptor.getInstallations()) {
            if (installation.getName().equals(name)) {
                return installation;
            }
        }
        return null;
    }

    @Symbol("crunchCli")
    @Extension
    public static final class DescriptorImpl extends ToolDescriptor<CliInstallation> {

        public DescriptorImpl() {
            super(CliInstallation.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.descriptor_cliInstallationDisplayName();
        }

        @Override
        public void setInstallations(CliInstallation... installations) {
            super.setInstallations(installations);
            save();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new CliDownloadInstaller(null));
        }
    }
}
