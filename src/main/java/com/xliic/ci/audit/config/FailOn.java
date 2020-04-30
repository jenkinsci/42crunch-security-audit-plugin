/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.config;

import com.fasterxml.jackson.annotation.JsonSetter;

public class FailOn {
    private Boolean invalidContract;
    private Score score;
    private Severity severity;
    private IssueId issueId;

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Boolean getInvalidContract() {
        return invalidContract;
    }

    @JsonSetter("invalid_contract")
    public void setInvalidContract(Boolean invalidContract) {
        this.invalidContract = invalidContract;
    }

    public Score getScore() {
        return this.score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    public IssueId getIssueId() {
        return issueId;
    }

    @JsonSetter("issue_id")
    public void setIssueId(IssueId issueId) {
        this.issueId = issueId;
    }
}
