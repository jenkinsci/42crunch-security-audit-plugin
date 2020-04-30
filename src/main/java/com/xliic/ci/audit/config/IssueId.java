/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonAnySetter;

@SuppressWarnings("serial")
public class IssueId extends ArrayList<String> {
    @JsonAnySetter
    public void set(String id) {
        this.add(id);
    }
}
