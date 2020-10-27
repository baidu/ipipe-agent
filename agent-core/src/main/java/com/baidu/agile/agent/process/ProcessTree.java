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

package com.baidu.agile.agent.process;

import com.baidu.agile.agent.os.OS;
import com.baidu.agile.agent.os.UnixUtil;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jvnet.winp.WinProcess;
import org.jvnet.winp.WinpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.baidu.agile.agent.jna.GNUCLibrary.LIBC;
import static com.sun.jna.Pointer.NULL;

/**
 *
 * 生成操作系统当前进程树
 * linux和solaris: 通过进程文件系统/proc读取进程信息
 * darwin: 实现比较复杂，通过jna访问系统本地库获取进程信息
 */
public abstract class ProcessTree implements Iterable<Process> {

    public static final Logger LOGGER = LoggerFactory.getLogger(Process.class);

    protected final Map<Integer, Process> processes = new HashMap<Integer, Process>();

    public static final ProcessTree DEFAULT = new ProcessTree() {
        @Override
        public void killAll(ProcessMatcher matcher) throws InterruptedException {
            // do nothing
        }
    };

    private static final long softKillWaitSeconds = Integer.getInteger("SoftKillWaitSeconds", 5);

    @Override
    public Iterator<Process> iterator() {
        return processes.values().iterator();
    }

    public static ProcessTree getTree() {
        switch (OS.os()) {
            case LINUX:
                return new LinuxTree();
            case DARWIN:
                return new DarwinTree();
            case SOLARIS:
                return new SolarisTree();
            case WINDOWS:
                return new WindowsTree();
            default:
        }
        return DEFAULT;
    }

    public Process getProcess(Integer pid) {
        return processes.get(pid);
    }

    public abstract void killAll(ProcessMatcher matcher) throws InterruptedException;

    private static final class WindowsTree extends ProcessTree {
        static {
            WinProcess.enableDebugPrivilege();
        }

        WindowsTree() {
            for (final WinProcess p : WinProcess.all()) {
                int pid = p.getPid();
                if (pid == 0 || pid == 4) {
                    // skip the System Idle and System processes
                    continue;
                }
                LOGGER.info("new WindowsTree pid=" + pid);
                this.processes.put(pid, new WindowsProcess(p));
            }
        }

        @Override
        public void killAll(ProcessMatcher matcher) throws InterruptedException {
            for (Process p : this) {
                if (p.getPid() < 10) {
                    continue;
                }
                boolean matched = matcher.match(p);
                LOGGER.info("windows to kill pid=" + p.getPid() + ",matched=" + matched);
                if (matched) {
                    p.killRecursively();
                } else {
                    LOGGER.info("windows environment variable didn't match for process with pid=" + p.getPid());
                }
            }
        }

        private class WindowsProcess extends Process {

            private final WinProcess p;
            protected Map<String, String> env;
            private List<String> args;

            WindowsProcess(WinProcess p) {
                super(p.getPid());
                this.p = p;
            }

            @Override
            public Process getParent() {
                // Windows process doesn't have parent/child relationship
                return null;
            }

            @Override
            public List<Process> getChildren() {
                // Windows process doesn't have parent/child relationship
                return null;
            }

            @Override
            public Map<String, String> getEnvVars() {
                try {
                    return getEnvironmentVariables2();
                } catch (WindowsProcessException e) {
                    LOGGER.info("Failed to get the environment variables of process with pid=" +
                            p.getPid() + "，message=" + e);
                }
                return env;
            }

            private synchronized Map<String, String> getEnvironmentVariables2() throws WindowsProcessException {
                if (env != null) {
                    return env;
                }
                env = new HashMap<String, String>();
                try {
                    env.putAll(p.getEnvironmentVariables());
                } catch (WinpException e) {
                    throw new WindowsProcessException("Failed to get the environment variables", e);
                }
                return env;
            }

            @Override
            public void killRecursively() throws InterruptedException {
                LOGGER.info("Killing recursively,pid=" + getPid());
                // Firstly try to kill the root process gracefully, then do a forcekill if it does not help (algorithm is described in JENKINS-17116)
                // 由于win10 CtrlC 存在问题所以注释掉
                // killSoftly();
                p.killRecursively();
            }

            @Override
            public void kill() throws InterruptedException {
                LOGGER.info("Killing pid=" + getPid());
                killSoftly();
                p.kill();
            }

            private void killSoftly() throws InterruptedException {
                try {
                    if (!p.sendCtrlC()) {
                        return;
                    }
                } catch (WinpException e) {
                    LOGGER.info("Failed to send CTRL+C to pid=" + getPid() + ",message=" + e);
                    return;
                }
                // after that wait for it to cease to exist
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(softKillWaitSeconds);
                int sleepTime = 10; // initially we sleep briefly, then sleep up to 1sec
                do {
                    if (!p.isRunning()) {
                        break;
                    }

                    Thread.sleep(sleepTime);
                    sleepTime = Math.min(sleepTime * 2, 1000);
                } while (System.nanoTime() < deadline);
            }
        }
    }

    private static class WindowsProcessException extends Exception {
        WindowsProcessException(WinpException ex) {
            super(ex);
        }

        WindowsProcessException(String message, WinpException ex) {
            super(message, ex);
        }
    }

    abstract class InnerProcess extends Process {
        public InnerProcess(int pid) {
            super(pid);
        }

        @Override
        public List<Process> getChildren() {
            List<Process> r = new ArrayList<Process>();
            for (Process p : ProcessTree.this) {
                if (p.getParent() == this) {
                    r.add(p);
                }
            }
            return r;
        }
    }

    abstract class UnixProcess extends InnerProcess {
        // envVars must be null for sub class implement
        protected Map<String, String> envVars;

        public UnixProcess(int pid) {
            super(pid);
        }

        public void addEnvVar(String line) {
            if (envVars == null) {
                envVars = new HashMap<String, String>();
            }
            int sep = line.indexOf('=');
            if (sep > 0) {
                envVars.put(line.substring(0, sep), line.substring(sep + 1));
            }
        }

        @Override
        public void kill() throws InterruptedException {
            try {
                LOGGER.info("Killing pid=" + this.pid);
                UnixUtil.destroy(pid);
            } catch (IllegalAccessException e) {
                // this is impossible
                IllegalAccessError x = new IllegalAccessError();
                x.initCause(e);
                throw x;
            } catch (InvocationTargetException e) {
                // tunnel serious errors
                if (e.getTargetException() instanceof Error) {
                    throw (Error) e.getTargetException();
                }
                LOGGER.info("Failed to terminate pid=" + pid, e);
            }
        }

        protected final File getFile(String relativePath) {
            return new File(new File("/proc/" + this.pid), relativePath);
        }
    }

    static abstract class ProcfsUnixTree extends UnixTree {
        ProcfsUnixTree() {
            File[] processes = new File("/proc").listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });
            if (processes == null) {
                LOGGER.info("No /proc");
                return;
            }

            for (File p : processes) {
                int pid;
                try {
                    pid = Integer.parseInt(p.getName());
                } catch (NumberFormatException e) {
                    // other sub-directories
                    continue;
                }
                try {
                    this.processes.put(pid, createProcess(pid));
                } catch (IOException e) {
                    // perhaps the process status has changed since we obtained a directory listing
                }
            }
        }

        protected abstract Process createProcess(int pid) throws IOException;
    }

    static class LinuxTree extends ProcfsUnixTree {
        protected LinuxProcess createProcess(int pid) throws IOException {
            return new LinuxProcess(pid);
        }

        private class LinuxProcess extends UnixProcess {
            private int ppid = -1;
            private String name = "";
            private List<String> arguments;

            LinuxProcess(int pid) throws IOException {
                super(pid);

                BufferedReader r = new BufferedReader(new FileReader(getFile("status")));
                try {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.toLowerCase(Locale.ENGLISH);
                        if (line.startsWith("name:")) {
                            name = line.substring(5).trim();
                        }
                        if (line.startsWith("ppid:")) {
                            ppid = Integer.parseInt(line.substring(5).trim());
                            break;
                        }
                    }
                } finally {
                    r.close();
                }
                if (ppid == -1) {
                    throw new IOException("Failed to parse PPID from /proc/" + pid + "/status");
                }
            }

            public Process getParent() {
                return processes.get(ppid);
            }

            @Override
            public synchronized Map<String, String> getEnvVars() {
                if (envVars != null) {
                    return envVars;
                }
                envVars = new HashMap<String, String>();
                try {
                    byte[] environ = readFileToByteArray(getFile("environ"));
                    int pos = 0;
                    for (int i = 0; i < environ.length; i++) {
                        byte b = environ[i];
                        if (b == 0) {
                            String line = new String(environ, pos, i - pos);
                            addEnvVar(line);
                            pos = i + 1;
                        }
                    }
                } catch (IOException e) {
                    // failed to read. this can happen under normal circumstances (most notably permission denied)
                    // so don't report this as an error.
                }
                return envVars;
            }

            @Override
            public String getName() {
                return name;
            }

            public synchronized List<String> getArguments() {
                if (arguments != null) {
                    return arguments;
                }
                arguments = new ArrayList<String>();
                try {
                    byte[] cmdline = readFileToByteArray(getFile("cmdline"));
                    int pos = 0;
                    for (int i = 0; i < cmdline.length; i++) {
                        byte b = cmdline[i];
                        if (b == 0) {
                            arguments.add(new String(cmdline, pos, i - pos));
                            pos = i + 1;
                        }
                    }
                } catch (IOException e) {
                    // failed to read. this can happen under normal circumstances (most notably permission denied)
                    // so don't report this as an error.
                }
                arguments = Collections.unmodifiableList(arguments);
                return arguments;
            }

            public byte[] readFileToByteArray(File file) throws IOException {
                InputStream in = FileUtils.openInputStream(file);
                try {
                    return IOUtils.toByteArray(in);
                } finally {
                    in.close();
                }
            }
        }
    }

    static class SolarisTree extends ProcfsUnixTree {
        protected Process createProcess(final int pid) throws IOException {
            return new SolarisProcess(pid);
        }

        private class SolarisProcess extends UnixProcess {

            private final int ppid;
            /**
             * Address of the environment vector. Even on 64bit Solaris this is still 32bit pointer.
             */
            private final int envp;
            /**
             * Similarly, address of the arguments vector.
             */
            private final int argp;
            private final int argc;
            private List<String> arguments;

            private SolarisProcess(int pid) throws IOException {
                super(pid);

                RandomAccessFile psinfo = new RandomAccessFile(getFile("psinfo"), "r");
                try {
                    // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/uts/common/sys/procfs.h
                    // typedef struct psinfo {
                    //    int    pr_flag;    /* process flags */
                    //    int    pr_nlwp;    /* number of lwps in the process */
                    //    pid_t    pr_pid;    /* process id */
                    //    pid_t    pr_ppid;    /* process id of parent */
                    //    pid_t    pr_pgid;    /* process id of process group leader */
                    //    pid_t    pr_sid;    /* session id */
                    //    uid_t    pr_uid;    /* real user id */
                    //    uid_t    pr_euid;    /* effective user id */
                    //    gid_t    pr_gid;    /* real group id */
                    //    gid_t    pr_egid;    /* effective group id */
                    //    uintptr_t    pr_addr;    /* address of process */
                    //    size_t    pr_size;    /* size of process image in Kbytes */
                    //    size_t    pr_rssize;    /* resident set size in Kbytes */
                    //    dev_t    pr_ttydev;    /* controlling tty device (or PRNODEV) */
                    //    ushort_t    pr_pctcpu;    /* % of recent cpu time used by all lwps */
                    //    ushort_t    pr_pctmem;    /* % of system memory used by process */
                    //    timestruc_t    pr_start;    /* process start time, from the epoch */
                    //    timestruc_t    pr_time;    /* cpu time for this process */
                    //    timestruc_t    pr_ctime;    /* cpu time for reaped children */
                    //    char    pr_fname[PRFNSZ];    /* name of exec'ed file */
                    //    char    pr_psargs[PRARGSZ];    /* initial characters of arg list */
                    //    int    pr_wstat;    /* if zombie, the wait() status */
                    //    int    pr_argc;    /* initial argument count */
                    //    uintptr_t    pr_argv;    /* address of initial argument vector */
                    //    uintptr_t    pr_envp;    /* address of initial environment vector */
                    //    char    pr_dmodel;    /* data model of the process */
                    //    lwpsinfo_t    pr_lwp;    /* information for representative lwp */
                    // } psinfo_t;

                    // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/uts/common/sys/types.h
                    // for the size of the various datatype.

                    // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/cmd/ptools/pargs/pargs.c
                    // for how to read this information

                    psinfo.seek(8);
                    if (adjust(psinfo.readInt()) != pid) {
                        // sanity check
                        throw new IOException("psinfo PID mismatch");
                    }
                    ppid = adjust(psinfo.readInt());

                    psinfo.seek(188);  // now jump to pr_argc
                    argc = adjust(psinfo.readInt());
                    argp = adjust(psinfo.readInt());
                    envp = adjust(psinfo.readInt());
                } finally {
                    psinfo.close();
                }
                if (ppid == -1) {
                    throw new IOException("Failed to parse PPID from /proc/" + pid + "/status");
                }

            }

            public Process getParent() {
                return processes.get(ppid);
            }

            public synchronized List<String> getArguments() {
                if (arguments != null) {
                    return arguments;
                }

                arguments = new ArrayList<String>(argc);

                try {
                    RandomAccessFile as = new RandomAccessFile(getFile("as"), "r");
                    LOGGER.info("Reading " + getFile("as"));
                    try {
                        for (int n = 0; n < argc; n++) {
                            // read a pointer to one entry
                            as.seek(to64(argp + n * 4));
                            int p = adjust(as.readInt());

                            arguments.add(readLine(as, p, "argv[" + n + "]"));
                        }
                    } finally {
                        as.close();
                    }
                } catch (IOException e) {
                    // failed to read. this can happen under normal circumstances (most notably permission denied)
                    // so don't report this as an error.
                }

                arguments = Collections.unmodifiableList(arguments);
                return arguments;
            }

            @Override
            public synchronized Map<String, String> getEnvVars() {
                if (envVars != null) {
                    return envVars;
                }
                envVars = new HashMap<String, String>();

                try {
                    RandomAccessFile as = new RandomAccessFile(getFile("as"), "r");
                    LOGGER.info("Reading " + getFile("as"));
                    try {
                        for (int n = 0; ; n++) {
                            // read a pointer to one entry
                            as.seek(to64(envp + n * 4));
                            int p = adjust(as.readInt());
                            if (p == 0) {
                                break;  // completed the walk
                            }

                            // now read the null-terminated string
                            addEnvVar(readLine(as, p, "env[" + n + "]"));
                        }
                    } finally {
                        as.close();
                    }
                } catch (IOException e) {
                    // failed to read. this can happen under normal circumstances (most notably permission denied)
                    // so don't report this as an error.
                }

                return envVars;
            }

            private String readLine(RandomAccessFile as, int p, String prefix) throws IOException {
                LOGGER.info("Reading " + prefix + " at " + p);

                as.seek(to64(p));
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int ch;
                int i = 0;
                while ((ch = as.read()) > 0) {
                    if ((++i) % 100 == 0) {
                        LOGGER.info(prefix + " is so far " + buf.toString());
                    }

                    buf.write(ch);
                }
                String line = buf.toString();
                LOGGER.info(prefix + " was " + line);
                return line;
            }
        }
    }

    /**
     * int to long conversion with zero-padding.
     */
    private static long to64(int i) {
        return i & 0xFFFFFFFFL;
    }


    private static final boolean IS_LITTLE_ENDIAN = "little" .equals(System.getProperty("sun.cpu.endian"));

    /**
     * {@link java.io.DataInputStream} reads a value in big-endian, so
     * convert it to the correct value on little-endian systems.
     */
    private static int adjust(int i) {
        if (IS_LITTLE_ENDIAN) {
            return (i << 24) | ((i << 8) & 0x00FF0000) | ((i >> 8) & 0x0000FF00) | (i >>> 24);
        } else {
            return i;
        }
    }

    static abstract class UnixTree extends ProcessTree {
        @Override
        public void killAll(ProcessMatcher matcher) throws InterruptedException {
            for (Process p : this) {
                if (matcher.match(p)) {
                    p.killRecursively();
                }
            }
        }
    }

    /**
     * Implementation for Mac OS X based on sysctl(3).
     */
    private static class DarwinTree extends UnixTree {
        DarwinTree() {
            String arch = System.getProperty("sun.arch.data.model");
            if ("64".equals(arch)) {
                sizeOf_kinfo_proc = sizeOf_kinfo_proc_64;
                kinfo_proc_pid_offset = kinfo_proc_pid_offset_64;
                kinfo_proc_ppid_offset = kinfo_proc_ppid_offset_64;
            } else {
                sizeOf_kinfo_proc = sizeOf_kinfo_proc_32;
                kinfo_proc_pid_offset = kinfo_proc_pid_offset_32;
                kinfo_proc_ppid_offset = kinfo_proc_ppid_offset_32;
            }
            try {
                IntByReference intByReference = new IntByReference(sizeOfInt);
                IntByReference size = new IntByReference(sizeOfInt);
                Memory m;
                int nRetry = 0;
                while (true) {
                    // find out how much memory we need to do this
                    if (LIBC.sysctl(MIB_PROC_ALL, 3, NULL, size, NULL, intByReference) != 0) {
                        throw new IOException("Failed to obtain memory requirement: "
                                + LIBC.strerror(Native.getLastError()));
                    }

                    // now try the real call
                    m = new Memory(size.getValue());
                    if (LIBC.sysctl(MIB_PROC_ALL, 3, m, size, NULL, intByReference) != 0) {
                        if (Native.getLastError() == ENOMEM && nRetry++ < 16) {
                            continue;
                            // retry
                        }
                        throw new IOException("Failed to call kern.proc.all: " + LIBC.strerror(Native.getLastError()));
                    }
                    break;
                }

                int count = size.getValue() / sizeOf_kinfo_proc;
                LOGGER.info("Found " + count + " processes");

                for (int base = 0; base < size.getValue(); base += sizeOf_kinfo_proc) {
                    int pid = m.getInt(base + kinfo_proc_pid_offset);
                    int ppid = m.getInt(base + kinfo_proc_ppid_offset);
                    // int effective_uid = m.getInt(base+304);
                    // byte[] comm = new byte[16];
                    // m.read(base+163,comm,0,16);
                    super.processes.put(pid, new DarwinProcess(pid, ppid));
                }
            } catch (IOException e) {
                LOGGER.info("Failed to obtain process list", e);
            }
        }

        // local constants
        public final int sizeOf_kinfo_proc;
        public static final int sizeOf_kinfo_proc_32 = 492; // on 32bit Mac OS X.
        public static final int sizeOf_kinfo_proc_64 = 648; // on 64bit Mac OS X.
        public final int kinfo_proc_pid_offset;
        public static final int kinfo_proc_pid_offset_32 = 24;
        public static final int kinfo_proc_pid_offset_64 = 40;
        public final int kinfo_proc_ppid_offset;
        public static final int kinfo_proc_ppid_offset_32 = 416;
        public static final int kinfo_proc_ppid_offset_64 = 560;
        public static final int sizeOfInt = Native.getNativeSize(int.class);
        public static final int CTL_KERN = 1;
        public static final int KERN_PROC = 14;
        public static final int KERN_PROC_ALL = 0;
        public static final int ENOMEM = 12;
        private static int[] MIB_PROC_ALL = {CTL_KERN, KERN_PROC, KERN_PROC_ALL};
        public static final int KERN_ARGMAX = 8;
        public static final int KERN_PROCARGS2 = 49;

        private class DarwinProcess extends UnixProcess {
            public final int ppid;
            private List<String> arguments;

            DarwinProcess(int pid, int ppid) {
                super(pid);
                this.ppid = ppid;
            }

            public Process getParent() {
                return processes.get(ppid);
            }

            @Override
            public synchronized Map<String, String> getEnvVars() {
                if (envVars != null) {
                    return envVars;
                }
                parse();
                return envVars;
            }

            private void parse() {
                try {
                    // allocate them first, so that the parse error wil result in empty data
                    // and avoid retry.
                    arguments = new ArrayList<String>();
                    envVars = new HashMap<String, String>();

                    IntByReference intByReference = new IntByReference();

                    IntByReference argmaxRef = new IntByReference(0);
                    IntByReference size = new IntByReference(DarwinTree.sizeOfInt);

                    // for some reason, I was never able to get sysctlbyname work.
                    if (LIBC.sysctl(new int[] {DarwinTree.CTL_KERN, DarwinTree.KERN_ARGMAX}, 2, argmaxRef.getPointer(),
                            size, NULL, intByReference) != 0) {
                        throw new IOException("Failed to get kernl.argmax: " + LIBC.strerror(Native.getLastError()));
                    }

                    int argmax = argmaxRef.getValue();

                    class StringArrayMemory extends Memory {
                        private long offset = 0;

                        StringArrayMemory(long l) {
                            super(l);
                        }

                        int readInt() {
                            int r = getInt(offset);
                            offset += DarwinTree.sizeOfInt;
                            return r;
                        }

                        byte peek() {
                            return getByte(offset);
                        }

                        String readString() {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte ch;
                            while ((ch = getByte(offset++)) != '\0') {
                                baos.write(ch);
                            }
                            return baos.toString();
                        }

                        void skip0() {
                            // skip padding '\0's
                            while (getByte(offset) == '\0') {
                                offset++;
                            }
                        }
                    }
                    StringArrayMemory m = new StringArrayMemory(argmax);
                    size.setValue(argmax);
                    if (LIBC.sysctl(new int[] {DarwinTree.CTL_KERN, DarwinTree.KERN_PROCARGS2, pid}, 3, m, size, NULL,
                            intByReference) != 0) {
                        throw new IOException("Failed to obtain ken.procargs2:" + LIBC.strerror(Native.getLastError()));
                    }


                    /**
                     * Make a sysctl() call to get the raw argument space of the
                     * process.  The layout is documented in start.s, which is part
                     * of the Csu project.  In summary, it looks like:
                     *
                     * /---------------\ 0x00000000
                     * :               :
                     * :               :
                     * |---------------|
                     * | argc          |
                     * |---------------|
                     * | arg[0]        |
                     * |---------------|
                     * :               :
                     * :               :
                     * |---------------|
                     * | arg[argc - 1] |
                     * |---------------|
                     * | 0             |
                     * |---------------|
                     * | env[0]        |
                     * |---------------|
                     * :               :
                     * :               :
                     * |---------------|
                     * | env[n]        |
                     * |---------------|
                     * | 0             |
                     * |---------------| <-- Beginning of data returned by sysctl()
                     * | exec_path     |     is here.
                     * |:::::::::::::::|
                     * |               |
                     * | String area.  |
                     * |               |
                     * |---------------| <-- Top of stack.
                     * :               :
                     * :               :
                     * \---------------/ 0xffffffff
                     */
                    // I find the Darwin source code of the 'ps' command helpful in understanding how it does this:
                    // see http://www.opensource.apple.com/source/adv_cmds/adv_cmds-147/ps/print.c
                    int argc = m.readInt();
                    String args0 = m.readString(); // exec path
                    m.skip0();
                    try {
                        for (int i = 0; i < argc; i++) {
                            arguments.add(m.readString());
                        }
                    } catch (IndexOutOfBoundsException e) {
                        throw new IllegalStateException("Failed to parse arguments: pid=" + pid + ", arg0=" + args0
                                + ",arguments=" + arguments + ", nargs=" + argc + ". Please run 'ps e " + pid
                                + "' and report this to https://issues.jenkins-ci.org/browse/JENKINS-9634", e);
                    }

                    // read env vars that follow
                    while (m.peek() != 0) {
                        addEnvVar(m.readString());
                    }
                } catch (IOException e) {
                    // this happens with insufficient permissions, so just ignore the problem.
                }
            }
        }
    }


}
