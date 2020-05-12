/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.model.assessment;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({ "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD",
        "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", })
public class AssessmentAttributeData {
    public String numCriticals;
    public String numErrors;
    public String numHighs;
    public String numLows;
    public String numMediums;
    public String oasVersion;
    public float grade;
}
