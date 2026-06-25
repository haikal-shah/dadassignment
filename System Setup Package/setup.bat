@echo off
echo =======================================================
echo MasakGramPrompt Setup Script
echo =======================================================
echo.
echo Default MySQL password is: 1234
echo Make sure MySQL Server 8.0 is running before continuing!
echo.
pause

SET MYSQL="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

echo.
echo [1/4] Creating Database...
%MYSQL% -u root -p1234 -e "DROP DATABASE IF EXISTS masakgramprompt; CREATE DATABASE masakgramprompt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo [2/4] Importing Database Schema and Data...
%MYSQL% -u root -p1234 masakgramprompt < database_dump.sql

echo [4/4] Pulling Local LLM Model (Llama 3.2 3B)...
echo (Make sure Ollama is installed and running!)
ollama pull llama3.2:3b

echo.
echo =======================================================
echo Setup Complete!
echo Open Eclipse, import the 4 Maven projects, clean/build them,
echo and then run ServerMain followed by ClientMain.
echo =======================================================
pause
