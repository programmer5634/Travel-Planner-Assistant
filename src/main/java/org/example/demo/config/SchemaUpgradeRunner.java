package org.example.demo.config;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.service.support.TravelPlanAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
public class SchemaUpgradeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgradeRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final TravelPlanAssembler travelPlanAssembler;

    public SchemaUpgradeRunner(JdbcTemplate jdbcTemplate,
                               TravelPlanAssembler travelPlanAssembler) {
        this.jdbcTemplate = jdbcTemplate;
        this.travelPlanAssembler = travelPlanAssembler;
    }

    @Override
    public void run(ApplicationArguments args) {
        upgradeTextColumn("itinerary", "request_json");
        upgradeTextColumn("itinerary", "current_plan_json");
        upgradeTextColumn("itinerary_revision", "structured_plan_json");
        ensureItineraryColumn("departure_city", "ALTER TABLE `itinerary` ADD COLUMN `departure_city` VARCHAR(128) DEFAULT NULL AFTER `overview`");
        ensureItineraryColumn("destination", "ALTER TABLE `itinerary` ADD COLUMN `destination` VARCHAR(128) DEFAULT NULL AFTER `departure_city`");
        ensureItineraryColumn("start_date", "ALTER TABLE `itinerary` ADD COLUMN `start_date` DATE DEFAULT NULL AFTER `destination`");
        ensureItineraryColumn("end_date", "ALTER TABLE `itinerary` ADD COLUMN `end_date` DATE DEFAULT NULL AFTER `start_date`");
        ensureIndex("itinerary", "idx_itinerary_user_deleted_updated", "CREATE INDEX `idx_itinerary_user_deleted_updated` ON `itinerary` (`user_id`, `deleted_at`, `updated_at`)");
        ensureIndex("itinerary", "idx_itinerary_user_destination", "CREATE INDEX `idx_itinerary_user_destination` ON `itinerary` (`user_id`, `destination`)");
        ensureIndex("itinerary", "idx_itinerary_user_start_end", "CREATE INDEX `idx_itinerary_user_start_end` ON `itinerary` (`user_id`, `start_date`, `end_date`)");
        ensureIndex("itinerary", "idx_itinerary_user_favorite_updated", "CREATE INDEX `idx_itinerary_user_favorite_updated` ON `itinerary` (`user_id`, `favorite`, `updated_at`)");
        backfillItineraryMetadata();
    }

    private void upgradeTextColumn(String tableName, String columnName) {
        String dataType = jdbcTemplate.query(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                tableName,
                columnName
        );
        if (dataType == null || !"text".equalsIgnoreCase(dataType)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + columnName + "` MEDIUMTEXT NOT NULL");
        log.info("Upgraded {}.{} from TEXT to MEDIUMTEXT", tableName, columnName);
    }

    private void ensureItineraryColumn(String columnName, String alterSql) {
        if (columnExists("itinerary", columnName)) {
            return;
        }
        jdbcTemplate.execute(alterSql);
        log.info("Added itinerary column {}", columnName);
    }

    private void ensureIndex(String tableName, String indexName, String createSql) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(createSql);
        log.info("Added {}.{} index", tableName, indexName);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }

    private void backfillItineraryMetadata() {
        List<ItineraryMetadataRow> rows = jdbcTemplate.query(
                "SELECT id, request_json FROM itinerary WHERE departure_city IS NULL OR destination IS NULL OR start_date IS NULL OR end_date IS NULL",
                (rs, rowNum) -> new ItineraryMetadataRow(rs.getLong("id"), rs.getString("request_json"))
        );
        for (ItineraryMetadataRow row : rows) {
            try {
                TravelPlanRequest request = travelPlanAssembler.readRequest(row.requestJson());
                LocalDate startDate = parseDateOrNull(request.startDate());
                LocalDate endDate = parseDateOrNull(request.endDate());
                jdbcTemplate.update(
                        "UPDATE itinerary SET departure_city = ?, destination = ?, start_date = ?, end_date = ? WHERE id = ?",
                        request.departureCity(),
                        request.destination(),
                        startDate == null ? null : Date.valueOf(startDate),
                        endDate == null ? null : Date.valueOf(endDate),
                        row.id()
                );
            } catch (RuntimeException exception) {
                log.warn("Failed to backfill itinerary metadata for id={}", row.id(), exception);
            }
        }
        if (!rows.isEmpty()) {
            log.info("Backfilled itinerary metadata for {} rows", rows.size());
        }
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private record ItineraryMetadataRow(Long id, String requestJson) {
    }
}
