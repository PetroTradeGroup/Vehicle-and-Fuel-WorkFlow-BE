package zw.co.petrotrade.workflow.station;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Hibernate wrote check constraints listing each enum's values when it created these tables, and ddl-auto
// never updates them, so new roles and top-up statuses would be refused. The enums are the check now.
@Component
@RequiredArgsConstructor
public class EnumCheckMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("alter table if exists users drop constraint if exists users_role_check");
        jdbc.execute("alter table if exists station_top_ups drop constraint if exists station_top_ups_status_check");
        jdbc.execute("alter table if exists approvals drop constraint if exists approvals_level_check");
    }
}
