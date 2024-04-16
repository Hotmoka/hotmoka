@echo off
set SOURCE=%~dp0
set DIR=%SOURCE%

java --module-path "%DIR%modules\explicit";"%DIR%modules\automatic" --class-path "%DIR%modules\unnamed\*" --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module io.hotmoka.moka/io.hotmoka.moka.Moka %*
