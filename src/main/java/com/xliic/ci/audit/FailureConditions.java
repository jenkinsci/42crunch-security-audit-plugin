/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit;

import com.xliic.ci.audit.config.FailOn;

public class FailureConditions {
    public final int minScore;
    public final FailOn failOn;

    public FailureConditions(int minScore, FailOn failOn) {
        this.minScore = minScore;
        this.failOn = failOn;
    }

}