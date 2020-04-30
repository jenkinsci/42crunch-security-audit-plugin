/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit;

import java.io.IOException;

public interface OpenApiFinder {
    public void setPatterns(String[] patterns);

    public String[] find() throws AuditException, IOException, InterruptedException;
}
