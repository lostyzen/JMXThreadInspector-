package org.pba;

import com.sun.tools.attach.VirtualMachine;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

public class JMXThreadInspector {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java JMXThreadInspector <pid>");
            System.exit(1);
        }

        String pid = args[0];

        try {
            // Attach to the target JVM
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                vm.loadAgentLibrary("management-agent");
            } catch (Exception e) {
                vm.loadAgentLibrary("jmx.agent");
            }

            // Get the connector address
            String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");

            if (connectorAddress == null) {
                System.err.println("JMX connector address not found.");
                System.exit(1);
            }

            // Connect to the MBean server
            JMXServiceURL url = new JMXServiceURL(connectorAddress);
            Map<String, Object> env = new HashMap<>();
            JMXConnector connector = JMXConnectorFactory.connect(url, env);
            MBeanServerConnection mbsc = connector.getMBeanServerConnection();

            // Get the ThreadMXBean
            ThreadMXBean threadMXBean = ManagementFactory.newPlatformMXBeanProxy(
                    mbsc, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);

            // Retrieve thread information
            long[] threadIds = threadMXBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);

            // Print thread information
            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo != null) {
                    System.out.println("Thread ID: " + threadInfo.getThreadId());
                    System.out.println("Thread Name: " + threadInfo.getThreadName());
                    System.out.println("Thread State: " + threadInfo.getThreadState());
                    System.out.println("Blocked Time: " + threadInfo.getBlockedTime());
                    System.out.println("Waited Time: " + threadInfo.getWaitedTime());
                    System.out.println("----------------------------------------");
                }
            }

            // Detach from the JVM
            vm.detach();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
