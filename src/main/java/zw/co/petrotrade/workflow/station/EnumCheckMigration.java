package zw.co.petrotrade.workflow.station;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Hibernate wrote check constraints listing each enum's values when it created these tables, and ddl-auto
// never updates them, so new roles and top-up statuses would be refused. The enums are the check now.
// (Runs on every start; each step does nothing once applied.)
@Component
@RequiredArgsConstructor
public class EnumCheckMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("alter table if exists users drop constraint if exists users_role_check");
        jdbc.execute("alter table if exists station_top_ups drop constraint if exists station_top_ups_status_check");
        jdbc.execute("alter table if exists approvals drop constraint if exists approvals_level_check");
        // top-ups HR & Admin approved but IT hasn't loaded yet were briefly marked completed
        if (Boolean.TRUE.equals(jdbc.queryForObject("select to_regclass('fuel_transactions') is not null and to_regclass('station_top_ups') is not null", Boolean.class))) {
            jdbc.update("update station_top_ups set status = 'APPROVED' where status = 'COMPLETED' and id in "
                    + "(select station_top_up_id from fuel_transactions where loaded = false)");
        }
    }
}
