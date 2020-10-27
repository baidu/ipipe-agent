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

package com.baidu.agile.agent.run.artifact;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;

public class DirFileScanner implements FileScanner {

    public static final String ANT_PATTERN_ALL = "**/*";

    private String dir;

    private static final AntFileScanner ANT_FILE_SCANNER = new AntFileScanner(ANT_PATTERN_ALL);

    public DirFileScanner(String dir) {
        this.dir = dir;
    }

    public List<String> scan(String baseDir) {
        String scanDir;
        String dir = StringUtils.trimToEmpty(this.dir);
        if (new File(dir).isAbsolute()) {
            scanDir = dir;
        } else {
            scanDir = baseDir + File.separator + dir;
        }
        return ANT_FILE_SCANNER.scan(scanDir);
    }

}
