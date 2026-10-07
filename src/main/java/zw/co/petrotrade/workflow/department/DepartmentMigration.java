package zw.co.petrotrade.workflow.department;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// Departments used to be typed text on users and requests. On startup, turns any such text into real
// departments ("Finance" and "finance " become one) and links the rows to them. Safe to run every time:
// a user's old text is cleared once linked, and requests are only touched while they have no link.
@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final DepartmentRepository departments;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int users = 0;
        if (hasColumn("users", "department")) {
            for (Map<String, Object> row : jdbc.queryForList(
                    "select id, department from users where department_id is null and trim(coalesce(department, '')) <> ''")) {
                Department d = findOrCreate((String) row.get("department"));
                jdbc.update("update users set department_id = ?, department = null where id = ?", d.getId(), row.get("id"));
                users++;
            }
        }
        int requests = 0;
        for (Map<String, Object> row : jdbc.queryForList(
                "select id, department from transport_requests where department_id is null and trim(coalesce(department, '')) <> ''")) {
            Department d = findOrCreate((String) row.get("department"));
            jdbc.update("update transport_requests set department_id = ?, department = ? where id = ?", d.getId(), d.getName(), row.get("id"));
            requests++;
        }
        if (users + requests > 0) {
            log.info("Linked {} users and {} requests to departments", users, requests);
        }
    }

    private Department findOrCreate(String text) {
        String name = text.trim();
        return departments.findByNameIgnoreCase(name).orElseGet(() -> {
            Department d = new Department();
            d.setName(name);
            return departments.save(d);
        });
    }

    private boolean hasColumn(String table, String column) {
        Integer n = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema = current_schema() and table_name = ? and column_name = ?",
                Integer.class, table, column);
        return n != null && n > 0;
    }
}
