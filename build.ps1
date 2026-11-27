# build.ps1 - Builds PharmaGest.exe (folder dist\PharmaGest) and the zip to publish on GitHub.
# Usage, from the project root:   powershell -ExecutionPolicy Bypass -File build.ps1
# Needs a JDK 17+ (found automatically, or via JAVA_HOME). The .exe bundles its own trimmed Java runtime
# (only the modules the app uses, found with jdeps), so users don't need to install Java.

$ErrorActionPreference = 'Stop'
$version = '1.0.0'
Set-Location $PSScriptRoot

# Find the JDK's bin folder. Oracle's installer only puts java/javac on the PATH
# (in "Common Files\Oracle\Java\javapath"), not jar or jpackage, so we look for the real JDK.
function Find-JdkBin {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin') }
    if (Get-Command java -ErrorAction SilentlyContinue) {
        $line = cmd /c "java -XshowSettings:properties -version 2>&1" |
            Where-Object { $_ -match '^\s*java\.home\s*=' } | Select-Object -First 1
        if ($line) { $candidates += (Join-Path (($line -replace '^\s*java\.home\s*=\s*', '').Trim()) 'bin') }
    }
    foreach ($root in 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft') {
        if (Test-Path $root) {
            $candidates += Get-ChildItem $root -Directory -Filter 'jdk*' | Sort-Object Name -Descending |
                ForEach-Object { Join-Path $_.FullName 'bin' }
        }
    }
    foreach ($c in $candidates) {
        if (Test-Path (Join-Path $c 'jpackage.exe')) { return $c }
    }
    return $null
}

$jdkBin = Find-JdkBin
if (-not $jdkBin) { throw 'JDK not found (need javac, jar and jpackage): install a JDK 17 or newer, or set JAVA_HOME.' }
$javac = Join-Path $jdkBin 'javac.exe'
$jarTool = Join-Path $jdkBin 'jar.exe'
$jpackage = Join-Path $jdkBin 'jpackage.exe'
Write-Host "Using JDK: $jdkBin"

Write-Host '1/5 Cleaning'
foreach ($d in 'build', 'dist') {
    if (Test-Path $d) { Remove-Item -Recurse -Force $d }
}
New-Item -ItemType Directory -Force build\classes, build\jar | Out-Null

Write-Host '2/5 Compiling'
$driver = Get-ChildItem lib -Filter 'mysql-connector-j-*.jar' | Select-Object -First 1
if (-not $driver) { throw 'MySQL driver not found in lib\' }
$sources = Get-ChildItem -Recurse src -Filter *.java |
    Where-Object { $_.FullName -notmatch '\\test\\' } |
    ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d build\classes -cp $driver.FullName $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }

Write-Host '3/5 Building the JAR'
"Main-Class: pharmacie.Main`r`nClass-Path: $($driver.Name)`r`n" | Set-Content build\manifest.txt -Encoding Ascii
& $jarTool --create --file build\jar\pharmagest.jar --manifest build\manifest.txt -C build\classes .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }
Copy-Item $driver.FullName build\jar\

Write-Host '4/5 Building PharmaGest.exe with jpackage (about a minute)'
& $jpackage --type app-image --name PharmaGest --app-version $version --vendor 'Sharaf N.' `
    --description 'PharmaGest - gestion de pharmacie' --icon assets\pharmagest.ico `
    --input build\jar --main-jar pharmagest.jar --main-class pharmacie.Main --dest dist `
    --add-modules java.base,java.desktop,java.sql,java.naming,java.management,java.security.sasl,java.logging `
    --jlink-options '--strip-debug --no-man-pages --no-header-files'
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed' }
Copy-Item db.properties.example dist\PharmaGest\
Copy-Item sql\pharmacie.sql dist\PharmaGest\

Write-Host '5/5 Zipping for GitHub'
$zip = "dist\PharmaGest-$version-windows.zip"
Compress-Archive -Path dist\PharmaGest -DestinationPath $zip
# The zip does NOT contain your db.properties. It is copied afterwards only to test on this PC.
if (Test-Path db.properties) { Copy-Item db.properties dist\PharmaGest\ }

Write-Host ''
Write-Host 'Done.'
Write-Host '  App to run on this PC : dist\PharmaGest\PharmaGest.exe'
Write-Host "  File to publish       : $zip"
Write-Host '  If Windows Smart App Control blocks the .exe, run the app with the signed Java runtime:'
Write-Host '    .\dist\PharmaGest\runtime\bin\javaw.exe -jar .\dist\PharmaGest\app\pharmagest.jar'
