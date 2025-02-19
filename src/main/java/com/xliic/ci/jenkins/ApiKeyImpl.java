/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class ApiKeyImpl extends BaseStandardCredentials implements ApiKey {
    static final long serialVersionUID = 42L;

    @Nonnull
    private final Secret apiKey;

    @DataBoundConstructor
    public ApiKeyImpl(
            @CheckForNull CredentialsScope scope,
            @CheckForNull String id,
            @Nonnull Secret apiKey,
            @CheckForNull String description)
            throws IOException {
        super(scope, id, description);
        this.apiKey = apiKey;
    }

    @Nonnull
    @Override
    public Secret getApiKey() {
        return apiKey;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.ApiKeyImpl_api_key();
        }

        public FormValidation doCheckApiKey(@QueryParameter("apiKey") final String apiKey) {
            String decryptedKey = Secret.fromString(apiKey).getPlainText();
            if (Util.fixEmptyAndTrim(decryptedKey) == null) {
                return FormValidation.error("API Token cannot be empty");
            }
            if (!decryptedKey.matches(ApiKey.UUID_PATTERN)) {
                return FormValidation.error("Invalid API Token format");
            }
            return FormValidation.ok("Valid API Token format");
        }
    }
}
