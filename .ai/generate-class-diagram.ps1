Add-Type -AssemblyName System.Drawing

$outPath = Join-Path (Get-Location) "class.jpg"
$width = 1600
$height = 1200

$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$g.Clear([System.Drawing.Color]::White)

$fontLabel = New-Object System.Drawing.Font "Segoe UI", 10
$fontSmall = New-Object System.Drawing.Font "Segoe UI", 8
$fontBubble = New-Object System.Drawing.Font "Segoe UI", 9, ([System.Drawing.FontStyle]::Bold)
$fontBubbleSmall = New-Object System.Drawing.Font "Segoe UI", 7
$brushText = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(42, 50, 60))
$brushWhite = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$penGroup = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(214, 214, 214)), 1.35

$colors = @{
    ".java" = [System.Drawing.Color]::FromArgb(187, 116, 21)
    ".fxml" = [System.Drawing.Color]::FromArgb(0, 105, 180)
    ".css" = [System.Drawing.Color]::FromArgb(95, 63, 141)
    ".xml" = [System.Drawing.Color]::FromArgb(0, 110, 180)
    ".cmd" = [System.Drawing.Color]::FromArgb(187, 235, 23)
    ".md" = [System.Drawing.Color]::FromArgb(6, 104, 177)
    ".gitignore" = [System.Drawing.Color]::FromArgb(0, 0, 0)
    ".property" = [System.Drawing.Color]::FromArgb(204, 213, 222)
}

function Draw-Group([double]$x, [double]$y, [double]$r, [string]$label, [double]$angle = -88) {
    $g.DrawEllipse($penGroup, ($x - $r), ($y - $r), ($r * 2), ($r * 2))
    if ($label) {
        $rad = $angle * [Math]::PI / 180
        $g.DrawString($label, $fontLabel, $brushText, ($x + ($r + 8) * [Math]::Cos($rad)), ($y + ($r + 8) * [Math]::Sin($rad)))
    }
}

function Short-Name($name, $max) {
    if ($name.Length -le $max) { return $name }
    return $name.Substring(0, [Math]::Max(1, $max - 3)) + "..."
}

function Draw-CenteredText($text, $font, $brush, $x, $y, $maxWidth) {
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = New-Object System.Drawing.RectangleF ($x - $maxWidth / 2), ($y - 18), $maxWidth, 36
    $g.DrawString($text, $font, $brush, $rect, $sf)
}

function Draw-Bubble($x, $y, $r, $label, $ext, $textColor = $null) {
    $color = $colors[$ext]
    if ($null -eq $color) { $color = [System.Drawing.Color]::FromArgb(145, 160, 176) }
    $brush = New-Object System.Drawing.SolidBrush $color
    $g.FillEllipse($brush, ($x - $r), ($y - $r), ($r * 2), ($r * 2))
    $font = if ($r -lt 22) { $fontBubbleSmall } else { $fontBubble }
    $fg = if ($textColor) { New-Object System.Drawing.SolidBrush $textColor } else { $brushWhite }
    Draw-CenteredText (Short-Name $label ([Math]::Max(4, [int]($r / 3.0) + 4))) $font $fg $x $y ($r * 1.75)
    $brush.Dispose()
    if ($textColor) { $fg.Dispose() }
}

function Draw-Legend($x, $y) {
    $legend = @(".cmd", ".css", ".fxml", ".gitignore", ".java", ".md", ".property", ".xml")
    for ($i = 0; $i -lt $legend.Count; $i++) {
        $ext = $legend[$i]
        $brush = New-Object System.Drawing.SolidBrush $colors[$ext]
        $g.FillEllipse($brush, $x, ($y + $i * 23), 13, 13)
        $g.DrawString($ext, $fontSmall, $brushText, ($x + 20), ($y + $i * 23 - 2))
        $brush.Dispose()
    }
}

$borderPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(226, 226, 226)), 2
$g.DrawRectangle($borderPen, 8, 8, $width - 16, $height - 16)

Draw-Group 590 505 470 "src/main"
Draw-Group 300 505 250 "resources/AUCTIONCODE/UI/view/auth" (-145)
Draw-Group 300 505 145 "FXML"
Draw-Group 155 575 80 "CSS" (-145)
Draw-Group 725 470 315 "java" (-86)
Draw-Group 725 470 280 "AUCTIONCODE" (-92)
Draw-Group 565 430 145 "UI/Controller/auth" (-120)
Draw-Group 830 450 150 "Model" (-55)
Draw-Group 675 640 95 "Database" 130
Draw-Group 905 645 85 "AuthModule" 30
Draw-Group 1220 370 92 ".idea"
Draw-Group 1040 805 72 ".mvn/wrapper" 140

$fxml = @(
    @(285,515,54,"Main.fxml"), @(370,500,43,"Login.fxml"), @(375,575,40,"Register.fxml"),
    @(275,620,36,"Profile.fxml"), @(220,575,34,"Admin.fxml"), @(230,475,30,"AddItem.fxml"),
    @(315,440,34,"CreateBid.fxml"), @(415,445,30,"WareHouse.fxml"), @(330,555,26,"ItemDetail.fxml"),
    @(205,520,26,"Vitien.fxml")
)
foreach ($b in $fxml) { Draw-Bubble $b[0] $b[1] $b[2] $b[3] ".fxml" }
Draw-Bubble 155 575 31 "auction-theme.css" ".css"

$java = @(
    @(545,425,50,"MainController.java"), @(620,405,43,"LoginController.java"), @(505,500,39,"RegisterController.java"),
    @(610,505,38,"AdminController.java"), @(555,565,35,"BidRoomController.java"), @(465,430,28,"AddItemController.java"),
    @(815,420,50,"AuctionRoom.java"), @(895,425,38,"User.java"), @(770,520,39,"Item.java"),
    @(865,525,34,"Player.java"), @(935,500,31,"BidTransaction.java"), @(730,430,29,"AuctionScheduler.java"),
    @(800,570,28,"Admin.java"), @(920,565,28,"UserInformation.java"), @(670,645,44,"AuctionDAO.java"),
    @(610,690,38,"ItemDAO.java"), @(720,715,36,"USERDAO.java"), @(900,645,42,"AuthService.java"),
    @(975,630,36,"InMemorySessionManager.java"), @(935,705,26,"Session.java"), @(775,265,36,"AuctionManager.java"),
    @(840,260,34,"UserManager.java"), @(945,315,35,"AuctionServer.java"), @(1005,315,29,"ClientHandler.java"),
    @(925,370,25,"AuctionClient.java"), @(990,375,23,"RequestParser.java"), @(1045,500,19,"Main.java"),
    @(1075,560,18,"MainLauncher.java")
)
foreach ($b in $java) { Draw-Bubble $b[0] $b[1] $b[2] $b[3] ".java" }

$idea = @(
    @(1195,380,43,"uiDesigner.xml"), @(1260,385,35,"artifacts.xml"), @(1250,325,23,"inspectionProfiles"),
    @(1300,340,18,"misc.xml"), @(1215,315,15,"vcs.xml"), @(1265,430,15,"workspace.xml")
)
foreach ($b in $idea) { Draw-Bubble $b[0] $b[1] $b[2] $b[3] ".xml" }

Draw-Bubble 1040 805 42 "maven-wrapper.properties" ".property"
Draw-Bubble 1225 735 47 "mvnw" ".property"
Draw-Bubble 1320 735 42 "mvnw.cmd" ".cmd" ([System.Drawing.Color]::FromArgb(38, 52, 28))
Draw-Bubble 1270 800 31 "pom.xml" ".xml"
Draw-Bubble 1170 675 22 ".gitignore" ".gitignore"
Draw-Bubble 1215 660 28 "README.md" ".md"
Draw-Bubble 1325 660 20 "data" ".property"
Draw-Bubble 1365 690 18 "lib" ".property"
Draw-Bubble 1375 805 17 "target" ".property"
Draw-Legend 1245 895

$encoder = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
$params = New-Object System.Drawing.Imaging.EncoderParameters 1
$params.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), 94L
$bmp.Save($outPath, $encoder, $params)

$g.Dispose()
$bmp.Dispose()
$params.Dispose()
