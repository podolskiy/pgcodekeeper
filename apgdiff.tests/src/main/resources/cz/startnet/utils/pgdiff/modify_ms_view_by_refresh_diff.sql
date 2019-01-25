-- DEPCY: This VIEW depends on the TABLE: dbo.t1

DROP VIEW [dbo].[v2]
GO

DROP TABLE [dbo].[t1]
GO

-- DEPCY: This TABLE is a dependency of VIEW: dbo.v3

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[t1](
	[c2] [int] NOT NULL,
	[c1] [int] NOT NULL
)
GO

-- DEPCY: This VIEW is a dependency of VIEW: dbo.v3

EXEC sp_refreshview '[dbo].[v1]' 
GO

-- DEPCY: This VIEW is a dependency of VIEW: dbo.v3

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE VIEW [dbo].[v2] AS
    SELECT * FROM [dbo].[v1]
GO

EXEC sp_refreshview '[dbo].[v3]' 
GO
