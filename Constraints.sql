CREATE DATABASE WeChatBeansDB;
GO

USING WeChatBeansDB;
GO

CREATE TABLE Users(
UserId INT IDENTITY(1,1) NOT NULL,
UserName VARCHAR(100) NOT NULL,
UserFirstName VARCHAR(1000) NULL,
UserLastName VARCHAR(1000) NULL,
EmailAddress NVARCHAR(500) NULL,
MobileNo NVARCHAR(20) NULL,
ProfilePicture IMAGE NULL,
CreatedAt DATETIME NULL
)

CREATE TABLE Messages(
MessageId INT IDENTITY(1,1) NOT NULL,
Sender INT,
Receiver INT,
Content VARCHAR(MAX) NOT NULL,
CreatedAt DATETIME NULL
)

CREATE TABLE Conversations(
ConversationsId INT IDENTITY(1,1) NOT NULL,
MessageId INT NOT NULL,
CreatedAt DATETIME NULL
)
