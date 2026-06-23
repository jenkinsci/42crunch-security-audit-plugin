/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import com.xliic.cicd.audit.Secret;
import java.io.Serializable;

class SecretImpl implements Secret, Serializable {
    private static final long serialVersionUID = 1L;
    private hudson.util.Secret secret;

    public SecretImpl(hudson.util.Secret secret) {
        this.secret = secret;
    }

    @Override
    public String getPlainText() {
        return secret.getPlainText();
    }
}
