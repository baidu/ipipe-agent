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

import com.baidu.agile.agent.java.JavaUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UnixUtil {

    private static final Field PID_FIELD;

    private static final Method DESTROY_PROCESS;

    static {
        try {
            // 非jdk中的public类
            Class<?> clazz = Class.forName("java.lang.UNIXProcess");
            PID_FIELD = clazz.getDeclaredField("pid");
            PID_FIELD.setAccessible(true);

            if (JavaUtil.isPreJava8()) {
                DESTROY_PROCESS = clazz.getDeclaredMethod("destroyProcess", int.class);
            } else {
                DESTROY_PROCESS = clazz.getDeclaredMethod("destroyProcess", int.class, boolean.class);
            }
            DESTROY_PROCESS.setAccessible(true);
        } catch (ClassNotFoundException e) {
            LinkageError x = new LinkageError();
            x.initCause(e);
            throw x;
        } catch (NoSuchFieldException e) {
            LinkageError x = new LinkageError();
            x.initCause(e);
            throw x;
        } catch (NoSuchMethodException e) {
            LinkageError x = new LinkageError();
            x.initCause(e);
            throw x;
        }
    }

    public static void destroy(int pid) throws IllegalAccessException, InvocationTargetException {
        if (JavaUtil.isPreJava8()) {
            DESTROY_PROCESS.invoke(null, pid);
        } else {
            DESTROY_PROCESS.invoke(null, pid, false);
        }
    }

    public static Integer getProcessId(Process proc) throws IllegalAccessError {
        try {
            return (Integer) PID_FIELD.get(proc);
        } catch (IllegalAccessException e) {
            IllegalAccessError x = new IllegalAccessError();
            x.initCause(e);
            throw x;
        }
    }
}
