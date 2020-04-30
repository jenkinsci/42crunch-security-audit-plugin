/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.model.api;

import java.time.ZonedDateTime;

public class Api {
    public Description desc;
    public Assessment assessment;

    public static class Assessment {
        public boolean isProcessed;
        public ZonedDateTime last;
        public float grade;
        public boolean isValid;
    }

    public static class Description {
        public String id;
    }
}
