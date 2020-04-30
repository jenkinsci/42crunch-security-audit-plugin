/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.client;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ApiStatus {
    public boolean isProcessed;
    public ZonedDateTime lastAssessment;

    public static ApiStatus freshApiStatus() {
        return new ApiStatus(false, ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    public ApiStatus(boolean isProcessed, ZonedDateTime lastAssessment) {
        this.isProcessed = isProcessed;
        this.lastAssessment = lastAssessment;
    }
}
