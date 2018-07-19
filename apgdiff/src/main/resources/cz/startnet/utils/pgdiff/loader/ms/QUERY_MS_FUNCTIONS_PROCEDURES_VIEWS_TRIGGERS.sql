SELECT 
    s.object_id,
    ss.schema_id,
    s.type,
    p.name AS owner,
    sm.definition,
    sm.uses_ansi_nulls AS ansi_nulls,
    sm.uses_quoted_identifier AS quoted_identifier
    --t.is_disabled AS IsDisabled
FROM sys.objects s WITH (NOLOCK)
LEFT JOIN sys.database_principals p WITH (NOLOCK) ON p.principal_id=s.principal_id
LEFT JOIN sys.schemas ss WITH (NOLOCK) ON ss.schema_id=s.schema_id
LEFT JOIN sys.sql_modules sm WITH (NOLOCK) ON sm.object_id=s.object_id
--LEFT JOIN sys.triggers t WITH(NOLOCK) ON t.object_id=s.object_id

WHERE s.type IN (N'TR', N'TA', N'V', N'IF', N'FN', N'TF', N'AF', N'P')