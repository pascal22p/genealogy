SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;

DROP EVENT IF EXISTS `delete_sessions`;
CREATE EVENT `delete_sessions` ON SCHEDULE EVERY 1 MINUTE STARTS '2024-07-15 14:50:12' ON COMPLETION NOT PRESERVE ENABLE DO DELETE FROM genea_sessions 
WHERE UNIX_TIMESTAMP(timestamp) < UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 HOUR));

DROP TABLE IF EXISTS `agregats_noms`;
CREATE TABLE `agregats_noms` (
  `id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) NOT NULL,
  `lettre` varbinary(8) NOT NULL,
  `nombre` smallint(3) unsigned NOT NULL DEFAULT 0,
  `longueur` tinyint(1) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `lettre` (`lettre`(5)),
  KEY `longueur` (`longueur`),
  KEY `base` (`base`),
  CONSTRAINT `agregats_noms_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tbale de prÃ©calul pour l''affichage de la liste des patronym';


DROP TABLE IF EXISTS `genea_address`;
CREATE TABLE `genea_address` (
  `addr_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) NOT NULL,
  `addr_addr` text NOT NULL,
  `addr_city` varchar(60) NOT NULL DEFAULT '',
  `addr_stae` varchar(60) NOT NULL DEFAULT '',
  `addr_post` varchar(10) NOT NULL DEFAULT '',
  `addr_ctry` varchar(60) NOT NULL DEFAULT '',
  `addr_phon1` varchar(25) NOT NULL DEFAULT '',
  `addr_phon2` varchar(25) NOT NULL DEFAULT '',
  `addr_phon3` varchar(25) NOT NULL DEFAULT '',
  `addr_email1` varchar(120) NOT NULL DEFAULT '',
  `addr_email2` varchar(120) NOT NULL DEFAULT '',
  `addr_email3` varchar(120) NOT NULL DEFAULT '',
  `addr_fax1` varchar(60) NOT NULL DEFAULT '',
  `addr_fax2` varchar(60) NOT NULL DEFAULT '',
  `addr_fax3` varchar(60) NOT NULL DEFAULT '',
  `addr_www1` varchar(120) NOT NULL DEFAULT '',
  `addr_www2` varchar(120) NOT NULL DEFAULT '',
  `addr_www3` varchar(120) NOT NULL DEFAULT '',
  PRIMARY KEY (`addr_id`),
  KEY `base` (`base`),
  CONSTRAINT `genea_address_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Table de stockage des adresses';


DROP TABLE IF EXISTS `genea_cache_deps`;
CREATE TABLE `genea_cache_deps` (
  `indi_id` mediumint(8) unsigned NOT NULL,
  `indi_dep` mediumint(8) unsigned NOT NULL,
  PRIMARY KEY (`indi_id`,`indi_dep`),
  KEY `indi_dep` (`indi_dep`),
  CONSTRAINT `genea_cache_deps_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `genea_cache_deps_ibfk_2` FOREIGN KEY (`indi_dep`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `genea_download`;
CREATE TABLE `genea_download` (
  `download_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) NOT NULL,
  `download_fichier` varchar(50) NOT NULL DEFAULT '',
  `download_titre` varchar(45) NOT NULL DEFAULT '',
  `download_description` tinytext DEFAULT NULL,
  `download_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`download_id`),
  KEY `base` (`base`),
  CONSTRAINT `genea_download_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Table contenant les fichiers Ã  tÃ©lÃ©charger';


DROP TABLE IF EXISTS `genea_events_details`;
CREATE TABLE `genea_events_details` (
  `events_details_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `place_id` mediumint(8) unsigned DEFAULT NULL,
  `addr_id` mediumint(8) unsigned DEFAULT NULL,
  `events_details_descriptor` varchar(255) NOT NULL DEFAULT '',
  `events_details_gedcom_date` varchar(100) NOT NULL DEFAULT '',
  `events_details_age` varchar(20) NOT NULL DEFAULT '',
  `events_details_cause` varchar(255) NOT NULL DEFAULT '',
  `jd_count` mediumint(8) unsigned DEFAULT NULL,
  `jd_precision` tinyint(3) unsigned DEFAULT NULL,
  `jd_calendar` varchar(45) DEFAULT NULL,
  `events_details_famc` mediumint(8) unsigned DEFAULT NULL,
  `events_details_adop` varchar(4) DEFAULT NULL,
  `base` mediumint(8) NOT NULL,
  `events_details_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`events_details_id`),
  KEY `base` (`base`),
  KEY `place_id` (`place_id`),
  KEY `addr_id` (`addr_id`),
  KEY `events_details_famc` (`events_details_famc`),
  CONSTRAINT `genea_events_details_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `genea_events_details_ibfk_2` FOREIGN KEY (`place_id`) REFERENCES `genea_place` (`place_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `genea_events_details_ibfk_3` FOREIGN KEY (`addr_id`) REFERENCES `genea_address` (`addr_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `genea_events_details_ibfk_4` FOREIGN KEY (`events_details_famc`) REFERENCES `genea_familles` (`familles_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tables contenant tous les évenements';


DROP TABLE IF EXISTS `genea_familles`;
CREATE TABLE `genea_familles` (
  `familles_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) DEFAULT NULL,
  `familles_wife` mediumint(8) unsigned DEFAULT NULL,
  `familles_husb` mediumint(8) unsigned DEFAULT NULL,
  `familles_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `familles_resn` enum('locked','privacy') DEFAULT NULL,
  `familles_refn` varchar(100) NOT NULL DEFAULT '',
  `familles_refn_type` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`familles_id`),
  KEY `husb` (`familles_husb`),
  KEY `wife` (`familles_wife`),
  KEY `base` (`base`),
  CONSTRAINT `genea_familles_ibfk_1` FOREIGN KEY (`familles_husb`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `genea_familles_ibfk_2` FOREIGN KEY (`familles_wife`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `genea_familles_ibfk_3` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Table contenant les familles';


DROP TABLE IF EXISTS `genea_individuals`;
CREATE TABLE `genea_individuals` (
  `indi_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) NOT NULL,
  `indi_nom` char(100) NOT NULL DEFAULT '',
  `indi_prenom` char(100) NOT NULL DEFAULT '',
  `indi_sexe` char(1) NOT NULL DEFAULT '',
  `indi_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `indi_npfx` char(30) NOT NULL DEFAULT '',
  `indi_givn` char(120) NOT NULL DEFAULT '',
  `indi_nick` char(30) NOT NULL DEFAULT '',
  `indi_spfx` char(30) NOT NULL DEFAULT '',
  `indi_nsfx` char(30) NOT NULL DEFAULT '',
  `indi_resn` enum('locked','privacy','confidential') DEFAULT NULL,
  PRIMARY KEY (`indi_id`),
  KEY `indi_nom` (`indi_nom`),
  KEY `indi_prenom` (`indi_prenom`),
  KEY `base` (`base`),
  CONSTRAINT `genea_individuals_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Table contenant les individus';


DROP TABLE IF EXISTS `genea_infos`;
CREATE TABLE `genea_infos` (
  `id` mediumint(8) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL DEFAULT '',
  `descriptif` tinytext NOT NULL,
  `entetes` text NOT NULL,
  `ged_corp` varchar(90) NOT NULL DEFAULT '',
  `subm` mediumint(8) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `subm` (`subm`),
  CONSTRAINT `genea_infos_ibfk_1` FOREIGN KEY (`subm`) REFERENCES `genea_submitters` (`sub_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ParamÃ¨tres gÃ©nÃ©raux des bases de donnÃ©es gÃ©nÃ©alogiques';


DROP TABLE IF EXISTS `genea_membres`;
CREATE TABLE `genea_membres` (
  `id` tinyint(3) unsigned NOT NULL AUTO_INCREMENT,
  `email` varchar(128) NOT NULL DEFAULT '',
  `saltpass` varchar(255) NOT NULL,
  `langue` varchar(20) NOT NULL DEFAULT '',
  `theme` varchar(20) NOT NULL DEFAULT '',
  `place` varchar(50) NOT NULL DEFAULT '',
  `see_privacy` tinyint(1) unsigned zerofill NOT NULL,
  `is_admin` tinyint(1) unsigned zerofill NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Liste des utilisateurs du site';


DROP TABLE IF EXISTS `genea_multimedia`;
CREATE TABLE `genea_multimedia` (
  `media_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) NOT NULL,
  `media_title` varchar(200) NOT NULL DEFAULT '',
  `media_format` varchar(50) NOT NULL DEFAULT '',
  `media_file` varchar(200) NOT NULL DEFAULT '',
  `media_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`media_id`),
  KEY `base` (`base`),
  CONSTRAINT `genea_multimedia_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Liste des documents multimÃ©dia';


DROP TABLE IF EXISTS `genea_notes`;
CREATE TABLE `genea_notes` (
  `notes_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) NOT NULL,
  `notes_text` text NOT NULL,
  `notes_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`notes_id`),
  KEY `base` (`base`),
  CONSTRAINT `genea_notes_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Liste des notes';


DROP TABLE IF EXISTS `genea_permissions`;
CREATE TABLE `genea_permissions` (
  `permission_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `membre_id` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `permission_type` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `permission_value` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `base` mediumint(8) NOT NULL DEFAULT 0,
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `unicite` (`membre_id`,`permission_type`,`permission_value`,`base`),
  KEY `type` (`permission_type`),
  KEY `base` (`base`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Gestion des permissions des diffÃ©rents membres';


DROP TABLE IF EXISTS `genea_place`;
CREATE TABLE `genea_place` (
  `place_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `place_lieudit` varchar(50) DEFAULT NULL,
  `place_ville` varchar(50) DEFAULT NULL,
  `place_cp` varchar(50) DEFAULT NULL,
  `place_insee` mediumint(8) unsigned DEFAULT NULL,
  `place_departement` varchar(50) DEFAULT NULL,
  `place_region` varchar(50) DEFAULT NULL,
  `place_pays` varchar(50) DEFAULT NULL,
  `place_longitude` float DEFAULT NULL,
  `place_latitude` float DEFAULT NULL,
  `base` mediumint(8) DEFAULT NULL,
  PRIMARY KEY (`place_id`),
  KEY `base` (`base`),
  CONSTRAINT `genea_place_ibfk_1` FOREIGN KEY (`base`) REFERENCES `genea_infos` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Liste des lieux gÃ©ographiques de la gÃ©nÃ©alogie';


DROP TABLE IF EXISTS `genea_refn`;
CREATE TABLE `genea_refn` (
  `refn_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` tinyint(4) NOT NULL,
  `refn_num` varchar(255) NOT NULL,
  `refn_type` varchar(255) NOT NULL,
  PRIMARY KEY (`refn_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `genea_repository`;
CREATE TABLE `genea_repository` (
  `repo_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `base` mediumint(8) NOT NULL,
  `repo_name` varchar(90) NOT NULL DEFAULT '',
  `repo_rin` varchar(255) NOT NULL DEFAULT '',
  `addr_id` mediumint(8) unsigned DEFAULT NULL,
  `repo_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`repo_id`),
  KEY `base` (`base`),
  KEY `addr_id` (`addr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='liste des dÃ©pots d''archives';


SET NAMES utf8mb4;

DROP TABLE IF EXISTS `genea_sessions`;
CREATE TABLE `genea_sessions` (
  `sessionId` varchar(36) NOT NULL,
  `sessionData` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`sessionData`)),
  `timeStamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sessionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `genea_sour_citations`;
CREATE TABLE `genea_sour_citations` (
  `sour_citations_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `sour_records_id` mediumint(8) unsigned DEFAULT NULL,
  `sour_citations_page` varchar(248) NOT NULL DEFAULT '',
  `sour_citations_even` varchar(15) NOT NULL DEFAULT '',
  `sour_citations_even_role` varchar(15) NOT NULL DEFAULT '',
  `sour_citations_data_dates` varchar(90) NOT NULL DEFAULT '',
  `sour_citations_data_text` mediumtext NOT NULL,
  `sour_citations_quay` tinyint(1) unsigned DEFAULT NULL,
  `sour_citations_subm` varchar(255) NOT NULL DEFAULT '',
  `sour_citations_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `base` mediumint(8) NOT NULL,
  PRIMARY KEY (`sour_citations_id`),
  KEY `sour_records_id` (`sour_records_id`),
  KEY `base` (`base`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `genea_sour_records`;
CREATE TABLE `genea_sour_records` (
  `sour_records_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `sour_records_auth` varchar(248) NOT NULL DEFAULT '',
  `sour_records_title` mediumtext NOT NULL,
  `sour_records_abbr` varchar(60) NOT NULL DEFAULT '',
  `sour_records_publ` mediumtext NOT NULL,
  `sour_records_agnc` varchar(120) NOT NULL DEFAULT '',
  `sour_records_rin` varchar(255) NOT NULL DEFAULT '',
  `repo_id` mediumint(8) unsigned DEFAULT NULL,
  `repo_caln` varchar(120) NOT NULL DEFAULT '',
  `repo_medi` varchar(15) NOT NULL DEFAULT '',
  `sour_records_timestamp` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `base` mediumint(8) NOT NULL,
  PRIMARY KEY (`sour_records_id`),
  KEY `base` (`base`),
  KEY `repo_id` (`repo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `genea_submitters`;
CREATE TABLE `genea_submitters` (
  `sub_id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `sub_name` varchar(100) NOT NULL DEFAULT '',
  `sub_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `sub_addr` mediumtext DEFAULT NULL,
  `sub_city` varchar(100) NOT NULL DEFAULT '',
  `sub_stae` varchar(100) NOT NULL DEFAULT '',
  `sub_post` varchar(100) NOT NULL DEFAULT '',
  `sub_ctry` varchar(100) NOT NULL DEFAULT '',
  `sub_phon1` varchar(100) NOT NULL DEFAULT '',
  `sub_phon2` varchar(100) NOT NULL DEFAULT '',
  `sub_phon3` varchar(100) NOT NULL DEFAULT '',
  `sub_lang` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`sub_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Liste des auteurs - NON UTILISE';


DROP TABLE IF EXISTS `gns_ADM1`;
CREATE TABLE `gns_ADM1` (
  `CC` varchar(2) NOT NULL DEFAULT '',
  `ADM1` varchar(2) NOT NULL DEFAULT '0',
  `name` varchar(200) NOT NULL DEFAULT '',
  PRIMARY KEY (`CC`,`ADM1`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Liste des rÃ©gions des pays chargÃ© dans la base de donnÃ©es';


DROP TABLE IF EXISTS `gns_CC`;
CREATE TABLE `gns_CC` (
  `RC` decimal(1,0) NOT NULL DEFAULT 0,
  `CC` char(2) NOT NULL DEFAULT '',
  `import` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`RC`,`CC`),
  KEY `CC` (`CC`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Liste des pays disponibles pour la base de donnÃ©es des lieu';


DROP TABLE IF EXISTS `gns_lieux`;
CREATE TABLE `gns_lieux` (
  `RC` decimal(1,0) NOT NULL DEFAULT 0,
  `UNI` decimal(10,0) NOT NULL DEFAULT 0,
  `LAT` decimal(10,7) NOT NULL DEFAULT 0.0000000,
  `LONGI` decimal(11,7) NOT NULL DEFAULT 0.0000000,
  `UTM` varchar(4) NOT NULL DEFAULT '',
  `JOG` varchar(7) NOT NULL DEFAULT '',
  `CC1` varchar(2) NOT NULL DEFAULT '',
  `ADM1` varchar(2) NOT NULL DEFAULT '',
  `SORT_NAME` varchar(200) NOT NULL DEFAULT '',
  `FULL_NAME` varchar(200) NOT NULL DEFAULT '',
  PRIMARY KEY (`UNI`),
  KEY `RC` (`RC`,`CC1`,`ADM1`),
  KEY `CC1` (`CC1`),
  KEY `FULL_NAME` (`FULL_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Base de donnÃ©es de lieux gÃ©ographiques';


DROP TABLE IF EXISTS `ign_rgc`;
CREATE TABLE `ign_rgc` (
  `dep` tinyint(4) unsigned NOT NULL,
  `com` smallint(5) unsigned NOT NULL,
  `arrd` tinyint(4) unsigned NOT NULL,
  `cant` tinyint(4) unsigned NOT NULL,
  `admi` enum('1','2','3','4','5','6') NOT NULL DEFAULT '6',
  `popu` mediumint(9) unsigned NOT NULL,
  `surface` mediumint(9) unsigned NOT NULL,
  `nom` varchar(50) NOT NULL,
  `xlamb2` mediumint(9) NOT NULL,
  `ylamb2` mediumint(9) NOT NULL,
  `xlambz` mediumint(9) NOT NULL,
  `ylambz` mediumint(9) NOT NULL,
  `xlamb93` mediumint(9) NOT NULL,
  `ylamb93` mediumint(9) NOT NULL,
  `longi_grd` mediumint(9) NOT NULL,
  `lati_grd` mediumint(9) NOT NULL,
  `longi_dms` mediumint(9) NOT NULL,
  `lati_dms` mediumint(9) NOT NULL,
  `zmin` smallint(6) NOT NULL,
  `zmax` smallint(6) NOT NULL,
  `zchl` smallint(6) NOT NULL,
  `carte` varchar(6) NOT NULL,
  KEY `dep` (`dep`,`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `insee_communes`;
CREATE TABLE `insee_communes` (
  `actual` set('1','2','3','4','5','6') NOT NULL,
  `cheflieu` set('0','1','2','3','4') NOT NULL,
  `cdc` set('0','1','2') NOT NULL,
  `rang` tinyint(3) unsigned NOT NULL,
  `reg` varchar(3) NOT NULL,
  `dep` varchar(3) NOT NULL,
  `com` mediumint(8) unsigned NOT NULL,
  `ar` tinyint(3) unsigned NOT NULL,
  `ct` tinyint(3) unsigned NOT NULL,
  `modif` tinyint(1) NOT NULL,
  `pole` varchar(5) NOT NULL,
  `tncc` set('0','1','2','3','4','5','6','7','8') NOT NULL,
  `artmaj` varchar(5) NOT NULL,
  `ncc` varchar(70) NOT NULL,
  `artmin` varchar(5) NOT NULL,
  `nccenr` varchar(70) NOT NULL,
  `articlct` varchar(5) NOT NULL,
  `nccct` varchar(70) NOT NULL,
  KEY `dep` (`dep`),
  KEY `com` (`com`),
  KEY `ncc` (`ncc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `rel_alias`;
CREATE TABLE `rel_alias` (
  `alias1` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `alias2` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`alias1`,`alias2`),
  KEY `alias2` (`alias2`),
  CONSTRAINT `rel_alias_ibfk_1` FOREIGN KEY (`alias1`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_alias_ibfk_2` FOREIGN KEY (`alias2`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stockage des alias des individus';


DROP TABLE IF EXISTS `rel_asso_events`;
CREATE TABLE `rel_asso_events` (
  `events_details_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`indi_id`,`events_details_id`),
  KEY `events_id` (`events_details_id`),
  CONSTRAINT `rel_asso_events_ibfk_1` FOREIGN KEY (`events_details_id`) REFERENCES `genea_events_details` (`events_details_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_asso_events_ibfk_2` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''un tÃ©moin Ã  un Ã©vÃ¨nement';


DROP TABLE IF EXISTS `rel_asso_familles`;
CREATE TABLE `rel_asso_familles` (
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `familles_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`indi_id`,`familles_id`),
  KEY `familles_id` (`familles_id`),
  CONSTRAINT `rel_asso_familles_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_asso_familles_ibfk_2` FOREIGN KEY (`familles_id`) REFERENCES `genea_familles` (`familles_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''une relation non familiale Ã  une famille';


DROP TABLE IF EXISTS `rel_asso_indi`;
CREATE TABLE `rel_asso_indi` (
  `indi_id1` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `indi_id2` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`indi_id1`,`indi_id2`),
  KEY `indi_id2` (`indi_id2`),
  CONSTRAINT `rel_asso_indi_ibfk_1` FOREIGN KEY (`indi_id1`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_asso_indi_ibfk_2` FOREIGN KEY (`indi_id2`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''une relation non familiale Ã  un individu';


DROP TABLE IF EXISTS `rel_events_multimedia`;
CREATE TABLE `rel_events_multimedia` (
  `media_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `events_details_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`events_details_id`,`media_id`),
  KEY `media_id` (`media_id`),
  CONSTRAINT `rel_events_multimedia_ibfk_1` FOREIGN KEY (`media_id`) REFERENCES `genea_multimedia` (`media_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_events_multimedia_ibfk_2` FOREIGN KEY (`events_details_id`) REFERENCES `genea_events_details` (`events_details_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''un objet multimedia a un évenement';


DROP TABLE IF EXISTS `rel_events_notes`;
CREATE TABLE `rel_events_notes` (
  `events_details_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `notes_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`events_details_id`,`notes_id`),
  KEY `notes_id` (`notes_id`),
  CONSTRAINT `rel_events_notes_ibfk_1` FOREIGN KEY (`events_details_id`) REFERENCES `genea_events_details` (`events_details_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_events_notes_ibfk_2` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''une note a un évenement';


DROP TABLE IF EXISTS `rel_events_sources`;
CREATE TABLE `rel_events_sources` (
  `events_details_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `sour_citations_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`events_details_id`,`sour_citations_id`),
  KEY `sources_id` (`sour_citations_id`),
  CONSTRAINT `rel_events_sources_ibfk_1` FOREIGN KEY (`events_details_id`) REFERENCES `genea_events_details` (`events_details_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_events_sources_ibfk_2` FOREIGN KEY (`sour_citations_id`) REFERENCES `genea_sour_citations` (`sour_citations_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''une source a un évenement';


DROP TABLE IF EXISTS `rel_familles_events`;
CREATE TABLE `rel_familles_events` (
  `events_details_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `familles_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `events_tag` varchar(4) NOT NULL DEFAULT '',
  `events_attestation` char(1) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`familles_id`,`events_details_id`),
  KEY `events_id` (`events_details_id`),
  CONSTRAINT `rel_familles_events_ibfk_1` FOREIGN KEY (`events_details_id`) REFERENCES `genea_events_details` (`events_details_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_familles_events_ibfk_2` FOREIGN KEY (`familles_id`) REFERENCES `genea_familles` (`familles_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''un évenement a une famille';


DROP TABLE IF EXISTS `rel_familles_indi`;
CREATE TABLE `rel_familles_indi` (
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `familles_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `rela_type` varchar(10) NOT NULL DEFAULT 'birth',
  `rela_stat` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`familles_id`,`indi_id`),
  KEY `indi_id` (`indi_id`),
  CONSTRAINT `rel_familles_indi_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_familles_indi_ibfk_2` FOREIGN KEY (`familles_id`) REFERENCES `genea_familles` (`familles_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''un enfant Ã  une famille';


DROP TABLE IF EXISTS `rel_familles_multimedia`;
CREATE TABLE `rel_familles_multimedia` (
  `media_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `familles_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`familles_id`,`media_id`),
  KEY `media_id` (`media_id`),
  CONSTRAINT `rel_familles_multimedia_ibfk_1` FOREIGN KEY (`media_id`) REFERENCES `genea_multimedia` (`media_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_familles_multimedia_ibfk_2` FOREIGN KEY (`familles_id`) REFERENCES `genea_familles` (`familles_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''un objet multimedia Ã  une famille';


DROP TABLE IF EXISTS `rel_familles_notes`;
CREATE TABLE `rel_familles_notes` (
  `notes_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `familles_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`familles_id`,`notes_id`),
  KEY `notes_id` (`notes_id`),
  CONSTRAINT `rel_familles_notes_ibfk_1` FOREIGN KEY (`familles_id`) REFERENCES `genea_familles` (`familles_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_familles_notes_ibfk_2` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''une note Ã  une famille';


DROP TABLE IF EXISTS `rel_familles_sources`;
CREATE TABLE `rel_familles_sources` (
  `sour_citations_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `familles_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`familles_id`,`sour_citations_id`),
  KEY `sources_id` (`sour_citations_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''une source Ã  une famille';


DROP TABLE IF EXISTS `rel_indi_attributes`;
CREATE TABLE `rel_indi_attributes` (
  `events_details_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `events_tag` varchar(4) NOT NULL DEFAULT '',
  `events_descr` varchar(255) NOT NULL DEFAULT '',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`indi_id`,`events_details_id`),
  KEY `events_id` (`events_details_id`),
  CONSTRAINT `rel_indi_attributes_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_indi_attributes_ibfk_2` FOREIGN KEY (`events_details_id`) REFERENCES `genea_events_details` (`events_details_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''un énement a une famille';


DROP TABLE IF EXISTS `rel_indi_events`;
CREATE TABLE `rel_indi_events` (
  `events_details_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `events_tag` varchar(4) NOT NULL DEFAULT '',
  `events_attestation` set('Y') DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`indi_id`,`events_details_id`),
  KEY `events_id` (`events_details_id`),
  CONSTRAINT `rel_indi_events_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_indi_events_ibfk_2` FOREIGN KEY (`events_details_id`) REFERENCES `genea_events_details` (`events_details_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association d''un Ã©vÃ¨nement Ã§ une famille';


DROP TABLE IF EXISTS `rel_indi_multimedia`;
CREATE TABLE `rel_indi_multimedia` (
  `media_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`indi_id`,`media_id`),
  KEY `media_id` (`media_id`),
  CONSTRAINT `rel_indi_multimedia_ibfk_1` FOREIGN KEY (`media_id`) REFERENCES `genea_multimedia` (`media_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_indi_multimedia_ibfk_2` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `rel_indi_notes`;
CREATE TABLE `rel_indi_notes` (
  `notes_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`indi_id`,`notes_id`),
  KEY `notes_id` (`notes_id`),
  CONSTRAINT `rel_indi_notes_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_indi_notes_ibfk_2` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;



DROP TABLE IF EXISTS `rel_indi_refn`;
CREATE TABLE `rel_indi_refn` (
  `indi_id` mediumint(8) unsigned NOT NULL,
  `refn_id` mediumint(8) unsigned NOT NULL,
  PRIMARY KEY (`indi_id`,`refn_id`),
  KEY `refn_id` (`refn_id`),
  CONSTRAINT `rel_indi_refn_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_indi_refn_ibfk_2` FOREIGN KEY (`refn_id`) REFERENCES `genea_refn` (`refn_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;


DROP TABLE IF EXISTS `rel_indi_sources`;
CREATE TABLE `rel_indi_sources` (
  `sour_citations_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `indi_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`indi_id`,`sour_citations_id`),
  KEY `sources_id` (`sour_citations_id`),
  CONSTRAINT `rel_indi_sources_ibfk_1` FOREIGN KEY (`indi_id`) REFERENCES `genea_individuals` (`indi_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_indi_sources_ibfk_2` FOREIGN KEY (`sour_citations_id`) REFERENCES `genea_sour_citations` (`sour_citations_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;


DROP TABLE IF EXISTS `rel_multimedia_notes`;
CREATE TABLE `rel_multimedia_notes` (
  `notes_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `media_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`notes_id`,`media_id`),
  KEY `media_id` (`media_id`),
  CONSTRAINT `rel_multimedia_notes_ibfk_1` FOREIGN KEY (`media_id`) REFERENCES `genea_multimedia` (`media_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_multimedia_notes_ibfk_2` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci COMMENT='Association d''un objet multimedia Ã  une source';


DROP TABLE IF EXISTS `rel_place_notes`;
CREATE TABLE `rel_place_notes` (
  `place_id` mediumint(8) unsigned NOT NULL,
  `notes_id` mediumint(8) unsigned NOT NULL,
  PRIMARY KEY (`place_id`,`notes_id`),
  KEY `note_id` (`notes_id`),
  CONSTRAINT `rel_place_notes_ibfk_1` FOREIGN KEY (`place_id`) REFERENCES `genea_place` (`place_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_place_notes_ibfk_2` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;


DROP TABLE IF EXISTS `rel_repo_notes`;
CREATE TABLE `rel_repo_notes` (
  `notes_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `repo_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`notes_id`,`repo_id`),
  KEY `media_id` (`repo_id`),
  CONSTRAINT `rel_repo_notes_ibfk_1` FOREIGN KEY (`repo_id`) REFERENCES `genea_repository` (`repo_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_repo_notes_ibfk_2` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci COMMENT='Association d''un objet multimedia Ã  une source';


DROP TABLE IF EXISTS `rel_sour_citations_multimedia`;
CREATE TABLE `rel_sour_citations_multimedia` (
  `sour_citations_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `media_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`sour_citations_id`,`media_id`),
  KEY `media_id` (`media_id`),
  CONSTRAINT `rel_sour_citations_multimedia_ibfk_1` FOREIGN KEY (`sour_citations_id`) REFERENCES `genea_sour_citations` (`sour_citations_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_sour_citations_multimedia_ibfk_2` FOREIGN KEY (`media_id`) REFERENCES `genea_multimedia` (`media_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci COMMENT='Association d''un objet multimedia Ã  une source';



DROP TABLE IF EXISTS `rel_sour_citations_notes`;
CREATE TABLE `rel_sour_citations_notes` (
  `notes_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `sour_citations_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`notes_id`,`sour_citations_id`),
  KEY `media_id` (`sour_citations_id`),
  CONSTRAINT `rel_sour_citations_notes_ibfk_1` FOREIGN KEY (`sour_citations_id`) REFERENCES `genea_sour_citations` (`sour_citations_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_sour_citations_notes_ibfk_2` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci COMMENT='Association d''un objet multimedia Ã  une source';


DROP TABLE IF EXISTS `rel_sour_records_notes`;
CREATE TABLE `rel_sour_records_notes` (
  `notes_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  `sour_records_id` mediumint(8) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`notes_id`,`sour_records_id`),
  KEY `media_id` (`sour_records_id`),
  CONSTRAINT `rel_sour_records_notes_ibfk_1` FOREIGN KEY (`notes_id`) REFERENCES `genea_notes` (`notes_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `rel_sour_records_notes_ibfk_2` FOREIGN KEY (`sour_records_id`) REFERENCES `genea_sour_records` (`sour_records_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci COMMENT='Association d''un objet multimedia Ã  une source';


