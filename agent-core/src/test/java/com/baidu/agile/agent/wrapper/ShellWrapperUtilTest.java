package com.baidu.agile.agent.wrapper;

import java.util.Arrays;

import com.baidu.agile.agent.execute.ShellProcessBuilder;
import com.baidu.agile.server.job.bean.wrapper.GitSshWraper;
import com.baidu.agile.server.job.bean.wrapper.ShellWraper;
import org.junit.Test;
import org.mockito.Mockito;


public class ShellWrapperUtilTest {

    private ShellProcessBuilder shellProcessBuilder = Mockito.mock(ShellProcessBuilder.class);

    @Test
    public void before() {
        ShellWraper shellWraper = new GitSshWraper();
        ShellWrapperUtil.before(shellProcessBuilder, Arrays.asList(shellWraper));
    }

    @Test
    public void beforeNull() {
        ShellWrapperUtil.before(null, null);
    }

    @Test
    public void after() {
        ShellWraper shellWraper = new GitSshWraper();
        ShellWrapperUtil.after(shellProcessBuilder, Arrays.asList(shellWraper));
    }

    @Test
    public void afterNull() {
        ShellWrapperUtil.after(null, null);
    }
}