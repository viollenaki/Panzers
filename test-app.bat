@echo off
echo Starting Panzers Application...
echo.
echo Make sure MySQL is running on localhost:3306 with:
echo - Database: panzers (will be created automatically)
echo - Username: root
echo - Password: 1234
echo.
echo Starting application...
java -jar target\Panzers-0.0.1-SNAPSHOT.jar
pause
