@echo off
setlocal
where py >nul 2>nul
if not errorlevel 1 goto python_launcher
where python >nul 2>nul
if not errorlevel 1 goto python_direct
echo Python 3.10+ is required.
exit /b 1
:python_launcher
py -3 -B "%~dp0scripts\auto-deliver.py" --entry-root "%~dp0." %*
exit /b %errorlevel%
:python_direct
python -B "%~dp0scripts\auto-deliver.py" --entry-root "%~dp0." %*
exit /b %errorlevel%
