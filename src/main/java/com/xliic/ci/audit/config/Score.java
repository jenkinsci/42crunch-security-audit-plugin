/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

public class Score {
    private Integer data;
    private Integer security;

    public Integer getData() {
        return data;
    }

    public Integer getSecurity() {
        return security;
    }

    public void setSecurity(Integer security) {
        this.security = security;
    }

    public void setData(Integer data) {
        this.data = data;
    }

}
