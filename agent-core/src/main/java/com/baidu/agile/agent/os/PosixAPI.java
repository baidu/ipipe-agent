/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.baidu.agile.agent.os;

import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.util.DefaultPOSIXHandler;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Logger;

/**
 * POSIX API wrapper.
 * Formerly used the jna-posix library, but this has been superseded by jnr-posix.
 * copy from jenkins
 * 根据不同操作系统加载不同的库
 */
public class PosixAPI {

    private static POSIX posix;

    /**
     * Load the JNR implementation of the POSIX APIs for the current platform.
     * Runtime exceptions will be of type {@link PosixException}.
     *
     * @return some implementation (even on Windows or unsupported Unix)
     * @since 1.518
     */
    public static synchronized POSIX jnr() {
        if (posix == null) {
            posix = POSIXFactory.getPOSIX(new DefaultPOSIXHandler() {
                @Override
                public void error(Errno error, String extraData) {
                    throw new PosixException("native error " + error.description() + " " + extraData, convert(error));
                }

                @Override
                public void error(Errno error, String methodName, String extraData) {
                    throw new PosixException("native error calling " + methodName + ": " + error.description() + " " + extraData, convert(error));
                }

                private org.jruby.ext.posix.POSIX.ERRORS convert(Errno error) {
                    try {
                        return org.jruby.ext.posix.POSIX.ERRORS.valueOf(error.name());
                    } catch (IllegalArgumentException x) {
                        return org.jruby.ext.posix.POSIX.ERRORS.EIO; // PosixException.message has real error anyway
                    }
                }
            }, true);
        }
        return posix;
    }

    /**
     * @deprecated use {@link #jnr} and {@link POSIX#isNative}
     */
    @Deprecated
    public boolean isNative() {
        return supportsNative();
    }

    /**
     * @deprecated use {@link #jnr} and {@link POSIX#isNative}
     */
    @Deprecated
    public static boolean supportsNative() {
        return !(jnaPosix instanceof org.jruby.ext.posix.JavaPOSIX);
    }

    private static org.jruby.ext.posix.POSIX jnaPosix;

    /**
     * @deprecated Use {@link #jnr} instead.
     */
    @Deprecated
    public static synchronized org.jruby.ext.posix.POSIX get() {
        if (jnaPosix == null) {
            jnaPosix = org.jruby.ext.posix.POSIXFactory.getPOSIX(new org.jruby.ext.posix.POSIXHandler() {
                public void error(org.jruby.ext.posix.POSIX.ERRORS errors, String s) {
                    throw new PosixException(s, errors);
                }

                public void unimplementedError(String s) {
                    throw new UnsupportedOperationException(s);
                }

                public void warn(WARNING_ID warning_id, String s, Object... objects) {
                    LOGGER.fine(s);
                }

                public boolean isVerbose() {
                    return true;
                }

                public File getCurrentWorkingDirectory() {
                    return new File(".").getAbsoluteFile();
                }

                public String[] getEnv() {
                    Map<String, String> envs = System.getenv();
                    String[] envp = new String[envs.size()];

                    int i = 0;
                    for (Map.Entry<String, String> e : envs.entrySet()) {
                        envp[i++] = e.getKey() + '+' + e.getValue();
                    }
                    return envp;
                }

                public InputStream getInputStream() {
                    return System.in;
                }

                public PrintStream getOutputStream() {
                    return System.out;
                }

                public int getPID() {
                    return 0;
                }

                public PrintStream getErrorStream() {
                    return System.err;
                }
            }, true);
        }
        return jnaPosix;
    }

    private static final Logger LOGGER = Logger.getLogger(PosixAPI.class.getName());
}
