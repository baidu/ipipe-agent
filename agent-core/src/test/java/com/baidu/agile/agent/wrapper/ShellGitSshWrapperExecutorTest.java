package com.baidu.agile.agent.wrapper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.baidu.agile.agent.execute.ShellProcessBuilder;
import com.baidu.agile.server.job.bean.concrete.ScriptType;
import com.baidu.agile.server.job.bean.wrapper.GitSshWraper;
import com.baidu.agile.server.job.bean.wrapper.ShellWraper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class ShellGitSshWrapperExecutorTest {

    private ShellGitSshWrapperExecutor shellGitSshWrapperExecutor = new ShellGitSshWrapperExecutor();

    @Test
    public void beforeParamNull() {
        shellGitSshWrapperExecutor.before(null, null);
    }

    @Test
    public void afterParamNull() {
        shellGitSshWrapperExecutor.after(null, null);
    }

    @Test
    public void testAfter() throws NoSuchFieldException, IllegalAccessException {
        Field field = ShellGitSshWrapperExecutor.class.getDeclaredField("filesMap");
        field.setAccessible(true);
        Map<String, List<String>> filesMap = (Map<String, List<String>>) field.get(shellGitSshWrapperExecutor);
        filesMap.put("uuid222", Arrays.asList("./a", "./b"));
        shellGitSshWrapperExecutor
                .after(new ShellProcessBuilder("uuid222", ScriptType.SH, "", null,
                        null, null), new GitSshWraper());
    }

    @Test
    public void testSupport() {
        Assert.assertFalse(shellGitSshWrapperExecutor.support(null));
        Assert.assertTrue(shellGitSshWrapperExecutor.support(new GitSshWraper()));
    }
}