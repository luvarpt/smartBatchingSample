package sk.luvar.database;

import lombok.extern.slf4j.Slf4j;
import sk.luvar.service.UserDTO;
import sk.luvar.service.UserPersist;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation for basic architectural idea test.
 */
@Slf4j
public class UserPersistInPostgresql implements UserPersist {
    private static final String TABLE_NAME = "SUSERS";
    final String url;
    final String user;
    final String password;

    public UserPersistInPostgresql(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        // Connecting to DB from constructor is not a wise approach, but in real project, liquibase would do this job
        this.checkSchema();
    }

    private void checkSchema() {
        try (final var connection = this.connect()) {
            final var createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "\n" + """
                    (
                      -- id Integer Primary Key Generated Always as Identity, -- when SQL standard is needed
                      user_id SERIAL PRIMARY KEY, --non SQL standard way, which does postgresql support and it native for it
                      user_uuid uuid NOT NULL DEFAULT gen_random_uuid(), -- I how that GUID in assignment is only typo... GUID is not the same thing as UUID through! See https://stackoverflow.com/questions/246930/is-there-any-difference-between-a-guid-and-a-uuid for example
                      user_name character varying(70) NOT NULL
                    );
                    """;
            try (final Statement statement = connection.createStatement()) {
                statement.execute(createTable);
            }

            final ResultSet tables = connection.getMetaData().getTables(null, null, null, null);
            boolean tableExist = false;
            while (tables.next()) {
                if (TABLE_NAME.compareToIgnoreCase(tables.getString(3)) == 0) {
                    tableExist = true;
                }
            }
            if (!tableExist) {
                throw new RuntimeException("There was some error, because newly created table does not exist! tablename=" + TABLE_NAME);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unexpected error during schema validation/preparation! ex=" + ex.getMessage(), ex);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<UserDTO> getAllBlocking() {
        final List<UserDTO> res = new ArrayList<>(10);
        final String SELECT_STATEMENT = "SELECT user_id, user_uuid, user_name FROM " + TABLE_NAME + " ORDER BY user_id ASC;";
        try (
                final Connection conn = connect();
                final Statement statement = conn.createStatement()) {
            // ResultSet is closed, when statement is closed, so we do not need to track its closing
            final var rs = statement.executeQuery(SELECT_STATEMENT);
            while (rs.next()) {
                res.add(new UserDTO(rs.getLong(1), rs.getString(2), rs.getString(3)));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error during batch processing! Going to ignore it! ex=" + ex.getMessage(), ex);
        }
        return res;
    }

    @Override
    public void saveUsers(List<String> usernames) {
        final String INSERT_STATEMENT = "INSERT INTO SUSERS(USER_NAME) VALUES(?)";
        try (
                final Connection conn = connect();
                final PreparedStatement statement = conn.prepareStatement(INSERT_STATEMENT, Statement.NO_GENERATED_KEYS)) {
            for (String username : usernames) {
                statement.setString(1, username);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new RuntimeException("Error during batch processing! Going to ignore it! ex=" + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteAll() {
        final String TRUNCATE_STATEMENT = "TRUNCATE TABLE " + TABLE_NAME + " RESTART IDENTITY;";
        try (
                final Connection conn = connect();
                final Statement statement = conn.createStatement()) {
            // ResultSet is closed, when statement is closed, so we do not need to track its closing
            statement.execute(TRUNCATE_STATEMENT);
        } catch (SQLException ex) {
            throw new RuntimeException("Error during batch processing! Going to ignore it! ex=" + ex.getMessage(), ex);
        }
    }
}
