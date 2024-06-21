package com.pba;

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
            System.out.println("Attaching to JVM with PID: " + pid);
            VirtualMachine vm = VirtualMachine.attach(pid);
            System.out.println("Attached to JVM successfully.");

            // Check if the JMX agent is already loaded
            System.out.println("Checking if JMX agent is already loaded...");
            String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (connectorAddress == null) {
                System.out.println("JMX agent not loaded. Enabling JMX...");

                // Set the required properties to enable JMX
                vm.startLocalManagementAgent();

                // Retrieve the connector address again
                connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
                if (connectorAddress == null) {
                    throw new IllegalStateException("Failed to enable JMX and obtain connector address.");
                }
            }

            System.out.println("JMX connector address: " + connectorAddress);

            // Connect to the MBean server
            System.out.println("Connecting to the MBean server...");
            JMXServiceURL url = new JMXServiceURL(connectorAddress);
            Map<String, Object> env = new HashMap<>();
            JMXConnector connector = JMXConnectorFactory.connect(url, env);
            MBeanServerConnection mbsc = connector.getMBeanServerConnection();
            System.out.println("Connected to the MBean server successfully.");

            // Get the ThreadMXBean
            System.out.println("Retrieving ThreadMXBean...");
            ThreadMXBean threadMXBean = ManagementFactory.newPlatformMXBeanProxy(
                    mbsc, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
            System.out.println("ThreadMXBean retrieved successfully.");

            // Retrieve thread information
            System.out.println("Retrieving thread information...");
            long[] threadIds = threadMXBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);

            // Print thread information
            System.out.println("Printing thread information:");
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
            System.out.println("Detaching from JVM...");
            vm.detach();
            System.out.println("Detached from JVM successfully.");
        } catch (Exception e) {
            System.err.println("Error occurred:");
            e.printStackTrace();
        }
    }
}
