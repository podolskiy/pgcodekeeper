SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[table2](
	[c1] [int] NOT NULL,
	[c2] [numeric] (18, 2) NOT NULL,
	[c3] [nvarchar] (max) COLLATE Cyrillic_General_CI_AS NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
WITH (DATA_COMPRESSION = PAGE)
GO

ALTER TABLE [dbo].[table2]
	ADD CONSTRAINT [PK_table2] PRIMARY KEY CLUSTERED  ([c1]) ON [PRIMARY]
GO
