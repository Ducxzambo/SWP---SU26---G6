package com.petclinic.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // ---- Cấu hình kết nối ----
    private static final String HOST     = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String PORT     = System.getenv().getOrDefault("DB_PORT", "1433");
    private static final String DB_NAME  = System.getenv().getOrDefault("DB_NAME", "PetClinicDB");
    private static final String USER     = System.getenv().getOrDefault("DB_USER", "sa");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASS", "123");

    private static final String URL =
        "jdbc:sqlserver://" + HOST + ":" + PORT
        + ";databaseName=" + DB_NAME
        + ";encrypt=true;trustServerCertificate=true";

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("SQL Server JDBC driver not found: " + e.getMessage());
        }
    }

    /** Trả về connection mới. Caller chịu trách nhiệm đóng. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private DBConnection() {}
}
