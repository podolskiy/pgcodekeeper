CREATE TABLE [dbo].[table1](
    [c1] [int] NOT NULL,
    [c2] [varchar](100) NOT NULL)
GO

CREATE FUNCTION [dbo].[ReturnInt]() 
RETURNS integer
AS
BEGIN
  RETURN -100 + 500
END
GO