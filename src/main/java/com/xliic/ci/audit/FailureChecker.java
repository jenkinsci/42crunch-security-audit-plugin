/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.xliic.ci.audit.config.Severity;
import com.xliic.ci.audit.model.assessment.AssessmentReport;
import com.xliic.ci.audit.model.assessment.AssessmentResponse;
import com.xliic.ci.audit.model.assessment.AssessmentReport.Issue;
import com.xliic.ci.audit.model.assessment.AssessmentReport.Issues;
import com.xliic.ci.audit.model.assessment.AssessmentReport.Section;

public class FailureChecker {

    private final HashMap<String, Integer> names = new HashMap<String, Integer>();

    public FailureChecker() {
        names.put("critical", 5);
        names.put("high", 4);
        names.put("medium", 3);
        names.put("low", 2);
        names.put("info", 1);
    }

    public ArrayList<String> checkAssessment(AssessmentResponse assessment, AssessmentReport report,
            FailureConditions conditions) {
        ArrayList<String> failures = new ArrayList<String>();

        failures.addAll(checkMinScore(assessment, conditions));

        if (conditions.failOn != null) {
            failures.addAll(checkCategoryScore(report, conditions));
            failures.addAll(checkInvalidContract(report, conditions));
            failures.addAll(checkSeverity(report, conditions));
            failures.addAll(checkIssueId(report, conditions));
        }

        return failures;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private ArrayList<String> checkMinScore(AssessmentResponse assessment, FailureConditions conditions) {
        ArrayList<String> failures = new ArrayList<String>();
        int score = Math.round(assessment.attr.data.grade);
        if (score < conditions.minScore) {
            failures.add(String.format("The API score %d is lower than the set minimum score of %d", score,
                    conditions.minScore));
        }
        return failures;
    }

    private ArrayList<String> checkCategoryScore(AssessmentReport report, FailureConditions conditions) {
        ArrayList<String> failures = new ArrayList<String>();
        if (conditions.failOn.getScore() != null) {
            Integer dataScore = conditions.failOn.getScore().getData();
            Integer securityScore = conditions.failOn.getScore().getSecurity();

            if (dataScore != null && getScore(report.data) < dataScore.intValue()) {
                failures.add(String.format("The API data score %d is lower than the set minimum score of %d",
                        getScore(report.data), dataScore));
            }

            if (securityScore != null && getScore(report.security) < securityScore.intValue()) {
                failures.add(String.format("The API security score %d is lower than the set minimum score of %d",
                        getScore(report.security), securityScore));
            }

        }

        return failures;
    }

    private ArrayList<String> checkInvalidContract(AssessmentReport report, FailureConditions conditions) {
        ArrayList<String> failures = new ArrayList<String>();

        boolean denyInvalidContract = conditions.failOn.getInvalidContract() == null
                || conditions.failOn.getInvalidContract().booleanValue();

        if (denyInvalidContract && !report.openapiState.equals("valid")) {
            failures.add("The OpenAPI definition is not valid");
        }

        return failures;
    }

    private ArrayList<String> checkIssueId(AssessmentReport report, FailureConditions conditions) {
        ArrayList<String> failures = new ArrayList<String>();
        if (conditions.failOn.getIssueId() != null) {

            HashSet<String> reportIssueIds = new HashSet<String>();
            if (report.data != null && report.data.issues != null) {
                reportIssueIds.addAll(report.data.issues.keySet());
            }
            if (report.security != null && report.security.issues != null) {
                reportIssueIds.addAll(report.security.issues.keySet());
            }

            for (String id : conditions.failOn.getIssueId()) {
                for (String reportId : reportIssueIds) {
                    String issueWithDashes = reportId.replace('.', '-');
                    if (issueWithDashes.matches(id)) {
                        failures.add(String.format("Found issue \"%s\"", issueWithDashes));
                    }
                }

            }
        }

        return failures;
    }

    private ArrayList<String> checkSeverity(AssessmentReport report, FailureConditions conditions) {
        ArrayList<String> failures = new ArrayList<String>();
        Severity severity = conditions.failOn.getSeverity();

        if (severity != null) {
            String dataSeverity = severity.getData();
            if (dataSeverity != null && report.data != null && report.data.issues != null) {
                int found = findBySeverity(report.data.issues, dataSeverity);
                if (found > 0) {
                    failures.add(String.format("Found %d issues in category \"data\" with severity \"%s\" or higher",
                            found, dataSeverity));
                }
            }

            String securitySeverity = severity.getSecurity();
            if (securitySeverity != null && report.security != null && report.security.issues != null) {
                int found = findBySeverity(report.security.issues, securitySeverity);
                if (found > 0) {
                    failures.add(
                            String.format("Found %d issues in category \"security\" with severity \"%s\" or higher",
                                    found, securitySeverity));
                }
            }

            String overallSeverity = severity.getOverall();
            if (overallSeverity != null) {
                int foundData = (report.data != null && report.data.issues != null)
                        ? findBySeverity(report.data.issues, overallSeverity)
                        : 0;
                int foundSecurity = (report.security != null && report.security.issues != null)
                        ? findBySeverity(report.security.issues, overallSeverity)
                        : 0;
                int found = foundData + foundSecurity;
                if (found > 0) {
                    failures.add(
                            String.format("Found %d issues with severity \"%s\" or higher", found, overallSeverity));
                }
            }

        }

        return failures;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private int findBySeverity(Issues issues, String severity) {
        if (issues == null) {
            return 0;
        }

        int found = 0;
        int criticality = names.get(severity);

        for (Issue issue : issues.values()) {
            if (issue.criticality >= criticality) {
                found = found + issue.issues.size();
            }
        }

        return found;
    }

    private int getScore(Section section) {
        if (section == null) {
            return 0;
        }
        return Math.round(section.score);
    }

}