/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.util.Secret;

@NameWith(ApiKey.NameProvider.class)
public interface ApiKey extends StandardCredentials {
    static final String UUID_PATTERN = "(api_)?[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

    @Nonnull
    Secret getApiKey();

    class NameProvider extends CredentialsNameProvider<ApiKey> {
        @Override
        @NonNull
        public String getName(@NonNull ApiKey credentials) {
            String description = Util.fixEmptyAndTrim(credentials.getDescription());
            return description != null ? description : Messages.ApiKeyImpl_api_key();
        }
    }

}
