/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import com.xliic.cicd.common.Util;
import com.xliic.cicd.common.WritableWorkspace;
import com.xliic.common.ContentType;
import com.xliic.common.Workspace;
import com.xliic.common.WorkspaceContent;
import com.xliic.common.WorkspaceException;
import hudson.FilePath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

class WorkspaceImpl implements WritableWorkspace {

    private FilePath workspace;

    WorkspaceImpl(FilePath workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceContent read(URI uri) throws IOException, InterruptedException {

        FilePath filepath = new FilePath(workspace, uri.getPath());

        InputStream is = filepath.read();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read;
        byte[] data = new byte[16384];
        while ((read = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, read);
        }
        buffer.flush();
        String contentData = new String(buffer.toByteArray(), StandardCharsets.UTF_8);

        boolean isYaml = Util.isYaml(uri.getPath());
        return new WorkspaceContent(contentData, isYaml ? ContentType.YAML : ContentType.JSON);
    }

    @Override
    public void write(URI uri, WorkspaceContent content)
            throws IOException, InterruptedException, WorkspaceException {

        FilePath filepath = new FilePath(workspace, uri.getPath());
        filepath.write(content.data, StandardCharsets.UTF_8.name());
    }

    @Override
    public boolean exists(URI file) throws IOException, InterruptedException {
        FilePath filepath = new FilePath(workspace, file.getPath());
        return filepath.exists();
    }

    @Override
    public URI resolve(String filename) {
        try {
            String safeFilename = new URI(null, filename, null).getRawSchemeSpecificPart();
            return workspace.toURI().resolve(safeFilename);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    @Override
    public URI relativize(URI uri) {
        try {
            return workspace.toURI().relativize(uri);
        } catch (IOException | InterruptedException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    public Workspace child(String dir) throws IOException {
        return new WorkspaceImpl(workspace.child(dir));
    }
}
