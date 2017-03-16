package com.containersol.minimesos.main;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Objects;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.docker.MesosClusterDockerFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a minimesos cluster")
public class CommandUp implements Command {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommandUp.class);

    private static final String CLINAME = "up";

    @Parameter(names = "--mapPortsToHost", description = "Map the Mesos and Marathon UI ports to the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private Boolean mapPortsToHost = null;

    @Parameter(names = "--clusterConfig", description = "Path to file with cluster configuration. Defaults to minimesosFile")
    private String clusterConfigPath = ClusterConfig.DEFAULT_CONFIG_FILE;

    private MesosCluster startedCluster = null;

    private PrintStream output = System.out; //NOSONAR

    MesosClusterFactory factory = new MesosClusterDockerFactory();

    CommandUp() {

    }

    CommandUp(PrintStream ps) {
        this();
        this.output = ps;
    }

    @Override
    public void execute() {
        LOGGER.debug("Executing up command");

        MesosCluster cluster = getCluster();
        if (cluster != null) {
            output.println("Cluster " + cluster.getClusterId() + " is already running");
            return;
        }
        ClusterConfig clusterConfig = readClusterConfigFromMinimesosFile();
        updateWithParameters(clusterConfig);

        startedCluster = factory.createMesosCluster(clusterConfig);
        // save cluster ID first, so it becomes available for 'destroy' even if its part failed to start
        factory.saveStateFile(startedCluster);

        startedCluster.start();
        startedCluster.waitForState(Objects::nonNull);

        new CommandInfo(output).execute();
    }

    /**
     * Reads ClusterConfig from minimesosFile.
     *
     * @return configuration of the cluster from the minimesosFile
     * @throws MinimesosException if minimesosFile is not found or malformed
     */
    private ClusterConfig readClusterConfigFromMinimesosFile() {
        InputStream clusterConfigFile = MesosCluster.getInputStream(getClusterConfigPath());
        if (clusterConfigFile != null) {
            ConfigParser configParser = new ConfigParser();
            try {
                return configParser.parse(IOUtils.toString(clusterConfigFile, "UTF-8"));
            } catch (Exception e) {
                String msg = String.format("Failed to load cluster configuration from %s: %s", getClusterConfigPath(), e.getMessage());
                throw new MinimesosException(msg, e);
            }
        }
        throw new MinimesosException("No minimesosFile found in current directory. Please generate one with 'minimesos init'");
    }

    /**
     * Adjust cluster configuration according to CLI parameters
     *
     * @param clusterConfig cluster configuration to update
     */
    public void updateWithParameters(ClusterConfig clusterConfig) {
        if (isMapPortsToHost() != null) {
            clusterConfig.setMapPortsToHost(isMapPortsToHost());
        }

        if (clusterConfig.getZookeeper() == null) {
            clusterConfig.setZookeeper(new ZooKeeperConfig());
        }

        if (clusterConfig.getMaster() == null) {
            clusterConfig.setMaster(new MesosMasterConfig(ClusterConfig.DEFAULT_MESOS_VERSION));
        }
    }

    public MesosCluster getCluster() {
        if (startedCluster != null) {
            return startedCluster;
        }
        else {
            return factory.retrieveMesosCluster();
        }
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLINAME;
    }

    public Boolean isMapPortsToHost() {
        return mapPortsToHost;
    }

    public void setMapPortsToHost(Boolean mapPortsToHost) {
        this.mapPortsToHost = mapPortsToHost;
    }

    public String getClusterConfigPath() {
        return clusterConfigPath;
    }

    public void setClusterConfigPath(String clusterConfigPath) {
        this.clusterConfigPath = clusterConfigPath;
    }

}
