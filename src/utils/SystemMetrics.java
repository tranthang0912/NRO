package utils;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class SystemMetrics {

    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    private static String formatMemory(double used, double total) {
        return String.format("(%.0f/%.0f) MB", used, total);
    }

    private static String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    private static double bytesToMB(long bytes) {
        return (double) bytes / (1024 * 1024);
    }

    private static double getUsedMemoryMB() {
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        return bytesToMB(totalMemory - freeMemory);
    }

    private static double getTotalMemoryMB() {
        return bytesToMB(osBean.getTotalMemorySize());
    }

    private static double getCPUUsage() {
        return osBean.getCpuLoad();
    }

    private static double getHeapUsedMemoryMB() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return bytesToMB(heapUsage.getUsed());
    }

    private static double getHeapMaxMemoryMB() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return bytesToMB(heapUsage.getMax());
    }

    public static String ToString() {
        double usedMemory = getUsedMemoryMB();
        double totalMemory = getTotalMemoryMB();
        double cpuUsage = getCPUUsage();
        double heapUsedMemory = getHeapUsedMemoryMB();
        double heapMaxMemory = getHeapMaxMemoryMB();

        return "Used Memory: " + formatMemory(usedMemory, totalMemory) + "\n"
                + "CPU Usage: " + formatDouble(cpuUsage) + "\n"
                + "Heap Used Memory: " + formatMemory(heapUsedMemory, heapMaxMemory);
    }
}