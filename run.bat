:start
git pull
call gradle shadowJar
move /Y build\libs\Sola-1.0-SNAPSHOT-all.jar Sola.jar
java -jar Sola.jar
if errorlevel 133 GOTO :start