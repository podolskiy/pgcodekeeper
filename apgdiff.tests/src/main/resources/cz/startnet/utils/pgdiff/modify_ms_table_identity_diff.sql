ALTER TABLE [dbo].[testtable]
	DROP COLUMN c1
GO

ALTER TABLE [dbo].[testtable]
	ADD [c1] [int] NOT NULL IDENTITY (2,1) NOT FOR REPLICATION
GO
