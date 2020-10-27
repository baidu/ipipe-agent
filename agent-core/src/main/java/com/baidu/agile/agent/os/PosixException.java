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

package com.baidu.agile.agent.os;

import org.jruby.ext.posix.POSIX.ERRORS;

/**
 * Indicates an error during POSIX API call.
 *
 * @see PosixAPI
 */
public class PosixException extends RuntimeException {
    private final ERRORS errors;

    public PosixException(String message, ERRORS errors) {
        super(message);
        this.errors = errors;
    }

    /**
     * @deprecated Leaks reference to deprecated jna-posix API.
     */
    @Deprecated
    public ERRORS getErrorCode() {
        return errors;
    }

    @Override
    public String toString() {
        return super.toString() + " " + errors;
    }
}

