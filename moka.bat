@echo off
set SOURCE=%~dp0
set DIR=%SOURCE%

java --module-path "%DIR%modules\explicit";"%DIR%modules\automatic" --class-path "%DIR%modules\unnamed\*" --module io.hotmoka.tools/io.hotmoka.tools.Moka %*
