/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.model.api;

public class ApiCollections {
    public ApiCollection[] list;

    public static class ApiCollection {
        public CollectionDesc desc;
    }

    public static class CollectionDesc {
        public String id;
        public String name;
    }
}
