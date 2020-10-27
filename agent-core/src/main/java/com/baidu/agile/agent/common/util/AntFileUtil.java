/**
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.agile.agent.common.util;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import javax.validation.constraints.NotNull;

import java.io.File;
import java.util.StringTokenizer;

public class AntFileUtil {

    @NotNull
    public static DirectoryScanner createDirectoryScanner(@NotNull File baseDir, @NotNull String includes) {
        return createFileSet(baseDir, includes).getDirectoryScanner();
    }

    @NotNull
    public static FileSet createFileSet(@NotNull File baseDir, @NotNull String includes) {
        return createFileSet(baseDir, includes, null);
    }

    @NotNull
    public static FileSet createFileSet(@NotNull File baseDir, @NotNull String includes, String excludes) {
        FileSet fs = new FileSet();
        fs.setDir(baseDir);
        fs.setProject(new Project());

        StringTokenizer tokens;

        tokens = new StringTokenizer(includes, ",");
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            fs.createInclude().setName(token);
        }
        if (excludes != null) {
            tokens = new StringTokenizer(excludes, ",");
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken().trim();
                fs.createExclude().setName(token);
            }
        }
        return fs;
    }
}
