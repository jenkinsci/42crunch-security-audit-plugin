/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonAnySetter;

@SuppressWarnings("serial")
public class Mapping extends HashMap<String, String> {
    @JsonAnySetter
    public void set(String filename, String apiId) {
        this.put(filename, apiId);
    }
}
