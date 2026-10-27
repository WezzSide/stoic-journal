Add-Type -AssemblyName System.Drawing

$sourcePath = "D:\dev-android\stoic-journal\Stoic Journal App Icon.png"
if (-not (Test-Path $sourcePath)) {
    Write-Error "Source image not found at $sourcePath"
    exit 1
}

$srcBmp = [System.Drawing.Bitmap]::FromFile($sourcePath)

# 1. Create 1024x1024 Master Icon without white border
$masterWidth = 1024
$masterHeight = 1024
$masterBmp = New-Object System.Drawing.Bitmap $masterWidth, $masterHeight
$masterGraphics = [System.Drawing.Graphics]::FromImage($masterBmp)
$masterGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$masterGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$masterGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

# Fill dark background #23262C
$bgColor = [System.Drawing.ColorTranslator]::FromHtml("#23262C")
$masterGraphics.Clear($bgColor)

# Crop inner dark icon from source (from x=36, y=34, w=1177, h=1186)
$srcRect = New-Object System.Drawing.Rectangle 36, 34, 1177, 1186
$destRect = New-Object System.Drawing.Rectangle 0, 0, 1024, 1024
$masterGraphics.DrawImage($srcBmp, $destRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)

$masterPath = "D:\dev-android\stoic-journal\branding\stoic-journal-icon-1024.png"
$masterBmp.Save($masterPath, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Saved Master Icon: $masterPath"

# Function to resize bitmap
function Resize-Image($src, $w, $h) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($src, 0, 0, $w, $h)
    $g.Dispose()
    return $bmp
}

# Function to create adaptive foreground image (108x108 base, inner 72x72 active content)
function Create-AdaptiveForeground($srcMaster, $canvasSize) {
    $bmp = New-Object System.Drawing.Bitmap $canvasSize, $canvasSize
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    # Scale icon to inner 66% (safe zone)
    $innerSize = [int]($canvasSize * 0.72)
    $offset = [int](($canvasSize - $innerSize) / 2)
    $g.DrawImage($srcMaster, $offset, $offset, $innerSize, $innerSize)
    $g.Dispose()
    return $bmp
}

# Densities for launcher icon
$densities = @(
    @{ name = "mipmap-mdpi";    legacySize = 48;  fgSize = 108 },
    @{ name = "mipmap-hdpi";    legacySize = 72;  fgSize = 162 },
    @{ name = "mipmap-xhdpi";   legacySize = 96;  fgSize = 216 },
    @{ name = "mipmap-xxhdpi";  legacySize = 144; fgSize = 324 },
    @{ name = "mipmap-xxxhdpi"; legacySize = 192; fgSize = 432 }
)

$resDir = "D:\dev-android\stoic-journal\app\src\main\res"

foreach ($d in $densities) {
    $folder = Join-Path $resDir $d.name
    if (-not (Test-Path $folder)) { New-Item -ItemType Directory -Force -Path $folder | Out-Null }

    # Legacy launcher & round launcher
    $legacyBmp = Resize-Image $masterBmp $d.legacySize $d.legacySize
    $legacyBmp.Save((Join-Path $folder "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $legacyBmp.Save((Join-Path $folder "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $legacyBmp.Dispose()

    # Adaptive foreground
    $fgBmp = Create-AdaptiveForeground $masterBmp $d.fgSize
    $fgBmp.Save((Join-Path $folder "ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $fgBmp.Dispose()

    Write-Host "Generated resources for $($d.name)"
}

$masterGraphics.Dispose()
$masterBmp.Dispose()
$srcBmp.Dispose()

Write-Host "All launcher icon resources successfully generated!"
