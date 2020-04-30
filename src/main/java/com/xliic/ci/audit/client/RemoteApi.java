/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.client;

public class RemoteApi {
    public String apiId;
    public ApiStatus previousStatus;

    RemoteApi(String apiId, ApiStatus previousStatus) {
        this.apiId = apiId;
        this.previousStatus = previousStatus;
    }
}
