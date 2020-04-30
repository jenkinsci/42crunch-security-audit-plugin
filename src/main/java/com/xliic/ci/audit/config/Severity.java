/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

public class Severity {
    private String data;
    private String security;
    private String overall;

    public Severity() {
        this.overall = null;
    }

    public Severity(String overall) {
        this.overall = overall;
    }

    public String getOverall() {
        return overall;
    }

    public void setOverall(String overall) {
        this.overall = overall;
    }

    public String getData() {
        return data;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public void setData(String data) {
        this.data = data;
    }
}
