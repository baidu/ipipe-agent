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

package com.baidu.agile.agent.common.response;

public class ResultResponse<R> extends Response {

    public ResultResponse(boolean success) {
        super(success);
    }

    public static ResultResponse success() {
        return new ResultResponse(true);
    }

    public static <R> ResultResponse success(R result) {
        ResultResponse response = new ResultResponse(true);
        response.setResult(result);
        return response;
    }

    public static ResultResponse fail(String error) {
        ResultResponse response = new ResultResponse(false);
        response.setError(error);
        return response;
    }

    private R result;

    public R getResult() {
        return result;
    }

    public void setResult(R result) {
        this.result = result;
    }
}
