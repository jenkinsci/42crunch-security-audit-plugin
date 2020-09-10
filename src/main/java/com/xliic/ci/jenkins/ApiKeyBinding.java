/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.Binding;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class ApiKeyBinding extends Binding<ApiKeyImpl> {

    @DataBoundConstructor
    public ApiKeyBinding(String variable, String credentialsId) {
        super(variable, credentialsId);
    }

    @Override
    protected Class<ApiKeyImpl> type() {
        return ApiKeyImpl.class;
    }

    @Override
    public SingleEnvironment bindSingle(@Nonnull Run<?, ?> build, @Nullable FilePath workspace,
            @Nullable Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        return new SingleEnvironment(getCredentials(build).getApiKey().getPlainText());
    }

    @Symbol("token")
    @Extension
    public static class DescriptorImpl extends BindingDescriptor<ApiKeyImpl> {

        @Override
        public boolean requiresWorkspace() {
            return false;
        }

        @Override
        protected Class<ApiKeyImpl> type() {
            return ApiKeyImpl.class;
        }

        @Override
        public String getDisplayName() {
            return Messages.ApiKeyImpl_api_key();
        }

    }

}