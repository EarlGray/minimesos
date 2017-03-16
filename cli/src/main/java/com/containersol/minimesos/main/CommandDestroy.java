package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.docker.MesosClusterDockerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameters for the 'destroy' command.
 */
@Parameters(separators = "=", commandDescription = "Destroy a minimesos cluster")
public class CommandDestroy implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDestroy.class);

    private static final String CLINAME = "destroy";

    MesosClusterFactory factory = new MesosClusterDockerFactory();

    @Override
    public void execute() {
        MesosCluster cluster = factory.retrieveMesosCluster();
        if (cluster != null) {
            cluster.destroy(factory);
            LOGGER.info("Destroyed minimesos cluster with ID " + cluster.getClusterId());
        } else {
            LOGGER.info("Minimesos cluster is not running");
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

}
