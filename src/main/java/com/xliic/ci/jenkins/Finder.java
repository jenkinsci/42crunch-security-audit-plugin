/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;
import jenkins.MasterToSlaveFileCallable;

import com.xliic.ci.audit.OpenApiFinder;

class Finder extends MasterToSlaveFileCallable<String[]> implements OpenApiFinder {
    private static final long serialVersionUID = 1L;
    private FilePath workspace;
    private String includes = "";
    private String excludes = "";

    public Finder(FilePath workspace) {
        this.workspace = workspace;
    }

    public void setPatterns(String[] patterns) {
        ArrayList<String> includes = new ArrayList<String>();
        ArrayList<String> excludes = new ArrayList<String>();

        for (String pattern : patterns) {
            if (pattern.startsWith("!")) {
                excludes.add(pattern.substring(1));
            } else {
                includes.add(pattern);
            }
        }
        this.includes = String.join(",", includes);
        this.excludes = String.join(",", excludes);
    }

    public String[] find() throws IOException, InterruptedException {
        String[] openApiFiles = workspace.act(this);
        return openApiFiles;
    }

    @Override
    public String[] invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        ArrayList<String> found = new ArrayList<String>();
        DirScanner.Glob scanner = new DirScanner.Glob(includes, excludes);
        scanner.scan(workspace, new FileVisitor() {
            @Override
            public void visit(File f, String relativePath) throws IOException {
                found.add(relativePath);
            }
        });

        return found.toArray(new String[found.size()]);
    }

}
