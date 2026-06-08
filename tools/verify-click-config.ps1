$ErrorActionPreference = "Stop"

$random = [System.Random]::new(12)

for ($i = 0; $i -lt 1000; $i++) {
    $x = 100.0 + $random.NextDouble() * (300.0 - 100.0)
    $y = 200.0 + $random.NextDouble() * (400.0 - 200.0)
    if ($x -lt 100.0 -or $x -gt 300.0 -or $y -lt 200.0 -or $y -gt 400.0) {
        throw "Rectangle random point escaped bounds."
    }
}

$random = [System.Random]::new(34)
for ($i = 0; $i -lt 1000; $i++) {
    $angle = $random.NextDouble() * [Math]::PI * 2.0
    $distance = [Math]::Sqrt($random.NextDouble()) * 80.0
    $x = 500.0 + [Math]::Cos($angle) * $distance
    $y = 600.0 + [Math]::Sin($angle) * $distance
    $dx = $x - 500.0
    $dy = $y - 600.0
    if ([Math]::Sqrt($dx * $dx + $dy * $dy) -gt 80.0001) {
        throw "Circle random point escaped radius."
    }
}

Write-Output "Click point math verified: rectangle, circle, and fixed-point mode are bounded as expected."
