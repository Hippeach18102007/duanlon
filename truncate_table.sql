DECLARE @sql NVARCHAR(MAX) = ''

SELECT @sql += 'DELETE FROM [' + s.name + '].[' + t.name + '];'
FROM sys.tables t
JOIN sys.schemas s ON t.schema_id = s.schema_id

EXEC sp_executesql @sql