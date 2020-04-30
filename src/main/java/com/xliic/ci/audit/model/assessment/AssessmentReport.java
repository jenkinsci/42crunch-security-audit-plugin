/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.model.assessment;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class AssessmentReport {
    public boolean valid;
    public String openapiState;
    public Section security;
    public Section data;

    public class Section {
        public float score;
        public Issues issues;
    }

    @SuppressWarnings("serial")
    public static class Issues extends HashMap<String, Issue> {
        @JsonAnySetter
        public void set(String id, Issue issue) {
            this.put(id, issue);
        }
    }

    public static class Issue {
        public int criticality;
        public ArrayList<SubIssue> issues;
    }

    public static class SubIssue {
        public float score;
    }

}