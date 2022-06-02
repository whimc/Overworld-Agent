package edu.whimc.overworld_agent.utils.sql;

import edu.whimc.overworld_agent.utils.sql.migration.SchemaManager;
import edu.whimc.overworld_agent.OverworldAgent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Class for SQL connection
 */
public class MySQLConnection {

    public static final String URL_TEMPLATE = "jdbc:mysql://%s:%s/%s";

    private Connection connection;
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final String url;
    private final int port;

    private final OverworldAgent plugin;

    /**
     * Constructor to set SQL info from config
     * @param plugin StudentFeedback plugin instance
     */
    public MySQLConnection(OverworldAgent plugin) {

        this.host = plugin.getConfig().getString("mysql.host");
        this.port = plugin.getConfig().getInt("mysql.port");
        this.database = plugin.getConfig().getString("mysql.database");
        this.username = plugin.getConfig().getString("mysql.username");
        this.password = plugin.getConfig().getString("mysql.password");


        this.url = String.format(URL_TEMPLATE, this.host, this.port, this.database);
        this.plugin = plugin;
    }

    /**
     * Initializes SQLConnection with schemamanager
     * @return boolean for sqlconnection status
     */
    public boolean initialize() {
        if (getConnection() == null) {
            return false;
        }

        SchemaManager manager = new SchemaManager(this.plugin, this.connection);
        return manager.initialize();
    }

    /**
     * Get SQL connection
     * @return SQL connection
     */
    public Connection getConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                return this.connection;
            }
            this.connection = DriverManager.getConnection(this.url, this.username, this.password);
        } catch (SQLException ignored) {
            ignored.printStackTrace();
            return null;
        }

        return this.connection;
    }

}
