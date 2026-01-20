package io.github.chi2l3s.nextlib.api.database;

import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.HikariDataSource;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Optional;

/**
 * Provides metrics and monitoring information for database connection pools.
 * <p>
 * Exposes HikariCP pool statistics for performance monitoring and troubleshooting.
 * </p>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * DatabaseMetrics metrics = new DatabaseMetrics(dataSource);
 *
 * System.out.println("Active connections: " + metrics.getActiveConnections());
 * System.out.println("Idle connections: " + metrics.getIdleConnections());
 * System.out.println("Total connections: " + metrics.getTotalConnections());
 * System.out.println("Threads waiting: " + metrics.getThreadsAwaitingConnection());
 * }</pre>
 *
 * @since 1.0.6
 */
public class DatabaseMetrics {

    private final HikariDataSource dataSource;
    private final HikariPoolMXBean poolBean;

    /**
     * Creates a DatabaseMetrics instance for the given HikariDataSource.
     *
     * @param dataSource the data source to monitor
     */
    public DatabaseMetrics(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.poolBean = getPoolMXBean(dataSource).orElse(null);
    }

    /**
     * Returns the number of currently active connections.
     *
     * @return active connections count, or -1 if unavailable
     */
    public int getActiveConnections() {
        return poolBean != null ? poolBean.getActiveConnections() : -1;
    }

    /**
     * Returns the number of currently idle connections.
     *
     * @return idle connections count, or -1 if unavailable
     */
    public int getIdleConnections() {
        return poolBean != null ? poolBean.getIdleConnections() : -1;
    }

    /**
     * Returns the total number of connections in the pool.
     *
     * @return total connections count, or -1 if unavailable
     */
    public int getTotalConnections() {
        return poolBean != null ? poolBean.getTotalConnections() : -1;
    }

    /**
     * Returns the number of threads waiting for a connection.
     *
     * @return threads awaiting connection count, or -1 if unavailable
     */
    public int getThreadsAwaitingConnection() {
        return poolBean != null ? poolBean.getThreadsAwaitingConnection() : -1;
    }

    /**
     * Returns the maximum pool size configured for this data source.
     *
     * @return maximum pool size
     */
    public int getMaximumPoolSize() {
        return dataSource.getMaximumPoolSize();
    }

    /**
     * Returns the minimum idle connections configured for this data source.
     *
     * @return minimum idle count
     */
    public int getMinimumIdle() {
        return dataSource.getMinimumIdle();
    }

    /**
     * Checks if the pool is running and accepting connections.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return dataSource.isRunning();
    }

    /**
     * Returns true if the pool has reached its maximum size and cannot create more connections.
     *
     * @return true if pool is at maximum capacity
     */
    public boolean isPoolAtMaxCapacity() {
        if (poolBean == null) {
            return false;
        }
        return poolBean.getTotalConnections() >= dataSource.getMaximumPoolSize();
    }

    /**
     * Returns pool utilization as a percentage (0.0 to 1.0).
     * <p>
     * Calculated as: (active connections / maximum pool size)
     * </p>
     *
     * @return pool utilization ratio, or -1.0 if unavailable
     */
    public double getPoolUtilization() {
        if (poolBean == null) {
            return -1.0;
        }
        int active = poolBean.getActiveConnections();
        int max = dataSource.getMaximumPoolSize();
        return max > 0 ? (double) active / max : 0.0;
    }

    /**
     * Returns a formatted string with key metrics.
     *
     * @return metrics summary string
     */
    public String getMetricsSummary() {
        if (poolBean == null) {
            return "Metrics unavailable (pool not initialized or closed)";
        }

        return String.format(
                "Pool[active=%d, idle=%d, total=%d, waiting=%d, max=%d, util=%.1f%%]",
                getActiveConnections(),
                getIdleConnections(),
                getTotalConnections(),
                getThreadsAwaitingConnection(),
                getMaximumPoolSize(),
                getPoolUtilization() * 100
        );
    }

    /**
     * Attempts to retrieve the HikariPoolMXBean for monitoring via JMX.
     *
     * @param dataSource the data source
     * @return Optional containing the MXBean if available
     */
    private Optional<HikariPoolMXBean> getPoolMXBean(HikariDataSource dataSource) {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + dataSource.getPoolName() + ")");
            HikariPoolMXBean bean = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);
            return Optional.of(bean);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
