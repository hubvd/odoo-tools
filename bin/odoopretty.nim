import nre, terminal, strformat, strutils

proc ctrlc() {.noconv.} =
  quit(0)

setControlCHook(ctrlc)

var
  line: string
  first = true
  loaded = false

let
  pattern = re"^(?P<date>\d{4}-\d{2}-\d{2}) (?P<time>\d{2}:\d{2}:\d{2},\d{3}) \d{2,6} (?P<level>[A-Z]+) (?P<version>\S+) (?P<logger>.*?): (?P<remaining>.*)"
  yellow = fgYellow.ansiForegroundColorCode(true)
  boldRed = fgRed.ansiForegroundColorCode(true)
  red = fgRed.ansiForegroundColorCode()
  blue = fgBlue.ansiForegroundColorCode(true)
  green = fgGreen.ansiForegroundColorCode(true)
  reset = ansiResetCode

while true:
  try:
    line = readLine(stdin)
    let matches = line.find(pattern)
    if matches.isSome:
      let
        m = matches.get
        date = m.captures["date"]
        time = m.captures["time"]
        level = m.captures["level"]
        version = m.captures["version"]
        logger = m.captures["logger"]
        remaining = m.captures["remaining"]

      if remaining.contains("longpolling"):
        continue

      if not loaded and level == "INFO":
        stdout.eraseLine()
      elif not first:
        echo()

      let color = case level:
        of "INFO":
          green
        of "WARN", "WARNING":
          red
        of "ERROR", "CRITICAL":
          boldRed
        else:
          blue

      first = false
      stdout.write(fmt"{time} {color}{level}{reset} {yellow}{logger}{reset}: {remaining}")
      stdout.flushFile()
      if not loaded:
        loaded = level == "INFO" and logger == "odoo.modules.loading" and remaining == "Modules loaded. "
        if loaded:
          echo()
    else:
      echo line
      first = false
  except EOFError:
    break
