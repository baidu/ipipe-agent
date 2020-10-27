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

package com.baidu.agile.agent;

import java.io.Serializable;

public class AgentHttpException extends RuntimeException implements Serializable {
    public AgentHttpException(String message) {
        super(message);
    }

    public AgentHttpException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public AgentHttpException(Throwable throwable) {
        super(throwable);
    }

    public AgentHttpException() {
    }
}
