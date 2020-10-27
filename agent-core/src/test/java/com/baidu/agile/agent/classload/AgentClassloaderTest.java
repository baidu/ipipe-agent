package com.baidu.agile.agent.classload;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import sun.net.www.protocol.jar.JarURLConnection;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AgentClassloader.class, ClassLoader.class})
public class AgentClassloaderTest {

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLoadAgileClass() throws IOException, ClassNotFoundException {
        final URL url = PowerMockito.mock(URL.class);
        Enumeration urls = new Enumeration<URL>() {
            int i = 0;

            @Override
            public boolean hasMoreElements() {
                i++;
                return i == 1;
            }

            @Override
            public URL nextElement() {
                return url;
            }
        };
        ClassLoader classLoader = PowerMockito.mock(ClassLoader.class);
        PowerMockito.when(classLoader.getResources("com/baidu/agile")).thenReturn(urls);
        PowerMockito.when(classLoader.loadClass(Mockito.anyString())).thenReturn(null);
        JarURLConnection jarURLConnection = PowerMockito.mock(JarURLConnection.class);
        PowerMockito.when(url.openConnection()).thenReturn(jarURLConnection);
        JarFile jarFile = PowerMockito.mock(JarFile.class);
        PowerMockito.when(jarURLConnection.getJarFile()).thenReturn(jarFile);
        final JarEntry jarEntry = new JarEntry("com/baidu/agile/test.class");
        Enumeration jarEntries = new Enumeration() {
            int i = 0;

            @Override
            public boolean hasMoreElements() {
                i++;
                return i == 1;
            }

            @Override
            public Object nextElement() {
                return jarEntry;
            }
        };
        PowerMockito.when(jarFile.entries()).thenReturn(jarEntries);
        PowerMockito.when(url.getProtocol()).thenReturn("jar");
        AgentClassloader.classLoader = classLoader;
        AgentClassloader.loadAgileClass();
    }
}
