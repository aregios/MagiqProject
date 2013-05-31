-- phpMyAdmin SQL Dump
-- version 3.5.1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Feb 11, 2013 at 07:15 AM
-- Server version: 5.5.24-log
-- PHP Version: 5.4.3

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `magiq`
--

DELIMITER $$
--
-- Functions
--
DROP FUNCTION IF EXISTS `geoDistance`$$
CREATE DEFINER=`root`@`127.0.0.1` FUNCTION `geoDistance`(`lat1` DOUBLE, `lon1` DOUBLE, `lat2` DOUBLE, `lon2` DOUBLE, `Rkm` DOUBLE) RETURNS double
    NO SQL
    DETERMINISTIC
BEGIN

DECLARE radlat1 double;
DECLARE radlon1 double;

DECLARE radlat2 double;
DECLARE radlon2 double;

DECLARE Rm double;
DECLARE x double;
DECLARE y double;

SET radlat1 = RADIANS(lat1);
SET radlon1 = RADIANS(lon1);

SET radlat2 = RADIANS(lat2);
SET radlon2 = RADIANS(lon2);

SET Rm = (Rkm * 1000);

-- Equirectangular approximation
SET x = (radlon2 - radlon1) * COS((radlat2 + radlat1)/2);
SET y = (radlat2 - radlat1);
RETURN SQRT(x*x + y*y) * Rm;

END$$

DROP FUNCTION IF EXISTS `geoDistance2`$$
CREATE DEFINER=`root`@`127.0.0.1` FUNCTION `geoDistance2`(`lat1` DOUBLE, `lon1` DOUBLE, `lat2` DOUBLE, `lon2` DOUBLE, `Rkm` DOUBLE) RETURNS double
    NO SQL
    DETERMINISTIC
BEGIN

DECLARE radlat1 double;
DECLARE radlon1 double;

DECLARE radlat2 double;
DECLARE radlon2 double;

DECLARE Rm double;
DECLARE d double;

SET radlat1 = RADIANS(lat1);
SET radlon1 = RADIANS(lon1);

SET radlat2 = RADIANS(lat2);
SET radlon2 = RADIANS(lon2);

SET Rm = (Rkm * 1000);

-- Spherical Law of Cosines
SET d = ACOS(SIN(radlat1)*SIN(radlat2) + COS(radlat1)*COS(radlat2)*COS(radlon2-radlon1));
RETURN d * Rm;

END$$

DROP FUNCTION IF EXISTS `geoDistance3`$$
CREATE DEFINER=`root`@`127.0.0.1` FUNCTION `geoDistance3`(`lat1` DOUBLE, `lon1` DOUBLE, `lat2` DOUBLE, `lon2` DOUBLE, `Rkm` DOUBLE) RETURNS double
    NO SQL
    DETERMINISTIC
BEGIN

DECLARE radlat1 double;
DECLARE radlon1 double;

DECLARE radlat2 double;
DECLARE radlon2 double;

DECLARE dlat double;
DECLARE dlon double;

DECLARE Rm double;
DECLARE d double;

SET radlat1 = RADIANS(lat1);
SET radlon1 = RADIANS(lon1);

SET radlat2 = RADIANS(lat2);
SET radlon2 = RADIANS(lon2);

SET Rm = (Rkm * 1000);

-- Haversine formula
SET dlat = (radlat2 - radlat1);
SET dlon = (radlon2 - radlon1);

SET d = SIN(dlat/2)*SIN(dlat/2) + SIN(dlon/2)*SIN(dlon/2)*COS(radlat1)*COS(radlat2);
RETURN 2 * ATAN2(SQRT(d), SQRT(1 - d)) * Rm;

END$$

DROP FUNCTION IF EXISTS `geoDistance4`$$
CREATE DEFINER=`root`@`127.0.0.1` FUNCTION `geoDistance4`(`lat1` DOUBLE, `lon1` DOUBLE, `lat2` DOUBLE, `lon2` DOUBLE, `Rkm` DOUBLE) RETURNS double
    NO SQL
    DETERMINISTIC
BEGIN

DECLARE radlat1 double;
DECLARE radlon1 double;

DECLARE radlat2 double;
DECLARE radlon2 double;

DECLARE Rm double;
DECLARE d double;
DECLARE latSin double;
DECLARE lonSin double;

SET radlat1 = RADIANS(lat1);
SET radlon1 = RADIANS(lon1);

SET radlat2 = RADIANS(lat2);
SET radlon2 = RADIANS(lon2);

SET Rm = (Rkm * 1000);

--
SET latSin = SIN(0.5 * (radlat1 - radlat2));
SET lonSin = SIN(0.5 * (radlon1 - radlon2));

SET d = 2 * ASIN(SQRT((latSin*latSin) + COS(radlat1) * COS(radlat2) * (lonSin * lonSin)));
RETURN d * Rm;

END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `imagedata`
--

DROP TABLE IF EXISTS `imagedata`;
CREATE TABLE IF NOT EXISTS `imagedata` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `imagefile` varchar(255) NOT NULL DEFAULT '',
  `latitude` double NOT NULL DEFAULT '0',
  `longitude` double NOT NULL DEFAULT '0',
  `description` varchar(255) DEFAULT '',
  `clientid` char(36) NOT NULL DEFAULT '',
  `inserted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `latitude` (`latitude`),
  KEY `longitude` (`longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=greek AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `imagequeries`
--

DROP TABLE IF EXISTS `imagequeries`;
CREATE TABLE IF NOT EXISTS `imagequeries` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `imagefile` varchar(255) NOT NULL DEFAULT '',
  `latitude` double NOT NULL DEFAULT '0',
  `longitude` double NOT NULL DEFAULT '0',
  `distance` int(11) NOT NULL DEFAULT '0',
  `maxtests` int(11) NOT NULL DEFAULT '0',
  `comparemethod` tinyint(4) NOT NULL DEFAULT '0',
  `reducefactor` tinyint(4) NOT NULL DEFAULT '0',
  `threshold` double NOT NULL DEFAULT '0',
  `clientid` char(36) NOT NULL DEFAULT '',
  `inserted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `clientid` (`clientid`)
) ENGINE=InnoDB DEFAULT CHARSET=greek AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `imageresults`
--

DROP TABLE IF EXISTS `imageresults`;
CREATE TABLE IF NOT EXISTS `imageresults` (
  `mid` int(11) NOT NULL,
  `did` int(11) NOT NULL,
  `result` double NOT NULL DEFAULT '0',
  `resulttype` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`mid`,`did`)
) ENGINE=InnoDB DEFAULT CHARSET=greek;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
