call mvn -U clean install
echo Exit Code = %ERRORLEVEL%
if not "%ERRORLEVEL%" == "0" exit /b
sleep 2
cd target
unzip jwat-tools-*.zip
cd ..
