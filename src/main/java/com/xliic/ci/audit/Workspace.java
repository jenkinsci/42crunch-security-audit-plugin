/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit;

import java.io.IOException;

public interface Workspace {
    public String read(String filename) throws IOException, InterruptedException;

    public boolean exists(String filename) throws IOException, InterruptedException;

    public String absolutize(String filename) throws IOException, InterruptedException;
}
