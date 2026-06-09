$ErrorActionPreference = "Stop"

$repo = "android-auto-clicker"
$tag = "v1.0.5"
$apk = Join-Path $PSScriptRoot "lian-dian-qi-v1.0.5-release.apk"

if (!(Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI is not installed."
}

gh auth status | Out-Null

if (!(Test-Path $apk)) {
    throw "APK not found: $apk"
}

$existingRemote = git remote get-url origin 2>$null
if (!$existingRemote) {
    gh repo create $repo --public --source . --remote origin --push
} else {
    git push -u origin master
}

gh release create $tag $apk --title $tag --notes-file RELEASE_NOTES.md
