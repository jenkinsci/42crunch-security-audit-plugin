/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

import com.fasterxml.jackson.annotation.JsonSetter;

public class Audit {
    private Mapping mapping;
    private Discovery discovery;
    private FailOn failOn;

    public Mapping getMapping() {
        return this.mapping;
    }

    public void setMapping(Mapping mapping) {
        this.mapping = mapping;
    }

    public Discovery getDiscovery() {
        return this.discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    public FailOn getFailOn() {
        return this.failOn;
    }

    @JsonSetter("fail_on")
    public void setFailOn(FailOn failOn) {
        this.failOn = failOn;
    }
}
