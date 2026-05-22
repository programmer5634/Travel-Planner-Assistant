-- Travel Planner Assistant - Initial Schema
-- Based on entity classes: UserEntity, TripSessionEntity, ItineraryEntity, ItineraryRevisionEntity

CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(64)  NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname`      VARCHAR(64)  DEFAULT NULL,
    `enabled`       TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `trip_session` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `session_code`  VARCHAR(64)  NOT NULL,
    `destination`   VARCHAR(128) DEFAULT NULL,
    `status`        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trip_session_code` (`session_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `itinerary` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
    `session_id`         BIGINT        NOT NULL,
    `user_id`            BIGINT        NOT NULL,
    `title`              VARCHAR(255)  NOT NULL,
    `overview`           TEXT          DEFAULT NULL,
    `departure_city`     VARCHAR(128)  DEFAULT NULL,
    `destination`        VARCHAR(128)  DEFAULT NULL,
    `start_date`         DATE          DEFAULT NULL,
    `end_date`           DATE          DEFAULT NULL,
    `request_json`       MEDIUMTEXT    NOT NULL,
    `current_plan_json`  MEDIUMTEXT    NOT NULL,
    `current_revision`   INT           NOT NULL DEFAULT 1,
    `favorite`           TINYINT(1)    NOT NULL DEFAULT 0,
    `deleted_at`         DATETIME      DEFAULT NULL,
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_itinerary_user_id` (`user_id`),
    KEY `idx_itinerary_session_id` (`session_id`),
    KEY `idx_itinerary_user_deleted_updated` (`user_id`, `deleted_at`, `updated_at`),
    KEY `idx_itinerary_user_destination` (`user_id`, `destination`),
    KEY `idx_itinerary_user_start_end` (`user_id`, `start_date`, `end_date`),
    KEY `idx_itinerary_user_favorite_updated` (`user_id`, `favorite`, `updated_at`),
    CONSTRAINT `fk_itinerary_session` FOREIGN KEY (`session_id`) REFERENCES `trip_session` (`id`),
    CONSTRAINT `fk_itinerary_user`    FOREIGN KEY (`user_id`)    REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `itinerary_revision` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
    `itinerary_id`         BIGINT       NOT NULL,
    `revision_no`          INT          NOT NULL,
    `user_message`         TEXT         DEFAULT NULL,
    `structured_plan_json` MEDIUMTEXT   NOT NULL,
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_revision_itinerary_id` (`itinerary_id`),
    CONSTRAINT `fk_revision_itinerary` FOREIGN KEY (`itinerary_id`) REFERENCES `itinerary` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
