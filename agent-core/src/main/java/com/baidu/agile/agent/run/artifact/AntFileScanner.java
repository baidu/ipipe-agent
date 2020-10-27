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

import com.baidu.agile.agent.common.util.AntFileUtil;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AntFileScanner implements FileScanner {

    private String includes;

    /**
     * AntFileScanner
     *
     * @param includes relative path pattern, (always empty when scan for absolute path pattern)
     */
    public AntFileScanner(String includes) {
        this.includes = includes;
    }

    public List<String> scan(String baseDir) {
        return Arrays.asList(AntFileUtil.createDirectoryScanner(
                new File(baseDir), StringUtils.trimToEmpty(includes)).getIncludedFiles());
    }

}
