/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

import java.util.Arrays;

public class Discovery {
    private static String[] DEFAULT_SEARCH = new String[] { "**/*.json", "**/*.yaml", "**/*.yml", "!node_modules/**",
            "!tsconfig.json" };

    private boolean enabled;
    private String[] search;

    public static Discovery defaultDiscovery() {
        Discovery discovery = new Discovery();
        discovery.search = DEFAULT_SEARCH;
        return discovery;
    }

    public Discovery(boolean enabled) {
        // called when discovery: false is specified
        this.enabled = enabled;
    }

    public Discovery() {
        // discovery is enabled by default, can be disabled with discovery: false
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String[] getSearch() {
        return Arrays.copyOf(search, search.length);
    }

    public void setSearch(String[] search) {
        this.search = Arrays.copyOf(search, search.length);
    }
}
