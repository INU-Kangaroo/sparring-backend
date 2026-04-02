CREATE TABLE `insulin_log` (
  `dose` decimal(6,2) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `temp_basal_active` bit(1) NOT NULL,
  `temp_basal_value` decimal(6,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `used_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `event_type` enum('BASAL','BOLUS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `insulin_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_insulin_log_user_used_at` (`user_id`,`used_at`),
  CONSTRAINT `fk_insulin_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
