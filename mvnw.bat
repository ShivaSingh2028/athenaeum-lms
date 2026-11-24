@echo off
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-25" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    )
)
if exist "%~dp0.tools\maven\bin\mvn.cmd" (
    "%~dp0.tools\maven\bin\mvn.cmd" -Dmaven.repo.local="%~dp0.tools\m2-repo" %*
) else (
    mvn %*
)
