-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `mydb` DEFAULT CHARACTER SET utf8 ;
USE `mydb` ;

-- -----------------------------------------------------
-- Table `mydb`.`User`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`User1` (
  `UID` INT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(45) NULL,
  `username` VARCHAR(45) NULL,
  `password` VARCHAR(45) NULL,
  `Default_pickup_lat` DOUBLE NULL,
  `Default_pickup_long` DOUBLE NULL,
  PRIMARY KEY (`UID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Routes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Routes` (
  `Rid` INT NOT NULL AUTO_INCREMENT,
  `Destination_lat` DOUBLE NULL,
  `Destination_long` DOUBLE NULL,
  `Order1` VARCHAR(45) NULL,
  PRIMARY KEY (`Rid`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Group1`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Group1` (
  `Group_ID` INT NOT NULL AUTO_INCREMENT,
  `isTemp` TINYINT NULL,
  `isVoting` TINYINT NULL,
  `group_leader` INT NULL,
  `Route_id` INT NULL,
  PRIMARY KEY (`Group_ID`),
  INDEX `group_leader_idx` (`group_leader` ASC),
  INDEX `Route_id_idx` (`Route_id` ASC),
  CONSTRAINT `group_leader`
    FOREIGN KEY (`group_leader`)
    REFERENCES `mydb`.`User1` (`UID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `Route_id`
    FOREIGN KEY (`Route_id`)
    REFERENCES `mydb`.`Routes` (`Rid`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Group_Membership`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Group_Membership` (
  `Gid` INT NOT NULL,
  `Uid` INT NOT NULL,
  `user_lat` DOUBLE NULL,
  `user_long` DOUBLE NULL,
  PRIMARY KEY (`Gid`, `Uid`),
  INDEX `Uid_idx` (`Uid` ASC),
  CONSTRAINT `Uid`
    FOREIGN KEY (`Uid`)
    REFERENCES `mydb`.`User1` (`UID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `Gid`
    FOREIGN KEY (`Gid`)
    REFERENCES `mydb`.`Group1` (`Group_ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Group_Destinations`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Group_Destinations` (
  `Group_id` INT NOT NULL,
  `Destination_lat` DOUBLE NULL,
  `Destination_long` DOUBLE NULL,
  `isPriority` BOOLEAN,
  `label` VARCHAR(90) NULL,
  CONSTRAINT `Group_id`
    FOREIGN KEY (`Group_id`)
    REFERENCES `mydb`.`Group1` (`Group_ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `mydb`.`GroupInvites` (
    `id` INT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`),
    `Gid` INT NOT NULL,
    `InviteId` VARCHAR(15) NOT NULL,
    CONSTRAINT `Invite_Gid`
        FOREIGN KEY (`Gid`)
        REFERENCES `mydb`.Group1 (`Group_ID`)
)
ENGINE = InnoDB;

USE `mydb`;

DELIMITER $$
USE `mydb`$$
CREATE DEFINER = CURRENT_USER TRIGGER `mydb`.`User_BEFORE_DELETE` BEFORE DELETE ON `User1` FOR EACH ROW
BEGIN
DELETE FROM Group_Membership WHERE Uid = old.UID;
END$$

-- Commented out due to error. Do we even need the default pickup locations?

-- DELIMITER $$
-- USE `mydb`$$
-- CREATE DEFINER = CURRENT_USER TRIGGER `mydb`.`Group_AFTER_INSERT` AFTER INSERT ON `Group1` FOR EACH ROW
-- BEGIN
--
-- declare lat double;
-- declare long1 double;
--
-- Select lat = Default_pickup_lat from User1 where UID =  new.group_leader;
-- Select long1 = Default_pickup_long from User1 where UID =  new.group_leader;
-- Insert into Group_Membership(Gid, Uid, user_lat, user_long) value
--     (new.Group_ID, new.group_leader, lat, long1);
-- END$$
--
USE `mydb`$$
CREATE DEFINER = CURRENT_USER TRIGGER `mydb`.`Group_BEFORE_DELETE` BEFORE DELETE ON `Group1` FOR EACH ROW
BEGIN
    DELETE from Group_Membership where Gid = old.Group_ID;
    delete from Group_Destinations where Group_id = old.Group_ID;
END$$

#drop database mydb;
DELIMITER ;

insert into User1(Uid, email, Username, password, Default_pickup_lat, Default_pickup_long) values
(001, 'qwe.gmail.com', 'qqq', 'www', 100.00, 120.00),
(002, 'asd.gmail.com', 'aaa', 'eee',  100.00, 120.00),
(003, 'zxc.gmail.com', 'zzz', 'rrr',  100.00, 120.00),
(004, 'try.gmail.com', 'ttt', 'ttt', 100.00, 120.00),
(005, 'fgh.gmail.com', 'fff', 'yyy', 100.00, 120.00);

insert into Routes(Rid, Destination_lat, Destination_long, Order1) values
(1, 50.00, 50.00, 1);

insert into Group1(Group_ID, isTemp, isVoting, group_leader, Route_id) values
(123, false, false, 001, 1),
(456, false, false, 002, 1),
(789, false, false, 003, 1);

insert into Group_Membership(Gid, Uid, User_lat, User_long) values
(123, 001, 100.00, 120.00),
(123, 005, 100.00, 120.00),
(456, 002, 100.00, 120.00),
(456, 004, 100.00, 120.00),
(789, 003, 100.00, 120.00);

insert into Group_Destinations(Group_id, Destination_lat, Destination_long, label) values
(123, 50.00, 50.00, 'delete me');

select * from User1;
select * from Group1;
select * from Routes;
select * from Group_Membership;
select * from Group_Destinations;

DELETE FROM Group1 WHERE Group_ID = 123;
DELETE FROM User1 WHERE UID = 5;
DELETE FROM User1 WHERE UID = 1;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

