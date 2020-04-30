/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.model;

public class OpenApiFile {
    public String swagger;
    public String openapi;
    private static String openApiRegex = "^3\\.0\\.\\d(-.+)?$";

    public boolean isOpenApi() {
        if (swagger != null && swagger.equals("2.0")) {
            return true;
        } else if (openapi != null && openapi.matches(openApiRegex)) {
            return true;
        }
        return false;
    }
}
