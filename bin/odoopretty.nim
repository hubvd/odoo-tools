import nre, terminal, strformat

proc ctrlc() {.noconv.} =
  quit(0)

setControlCHook(ctrlc)

type
  Context = tuple[level: string, logger: string]

var previousContext: Context
var first = true
let pattern = re"^(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3} \d{2,5}) (?P<level>[A-Z]+) (?P<version>\S+) (?P<logger>.*?): (?P<remaining>.*)"

let
  yellow = fgYellow.ansiForegroundColorCode(true)
  red = fgRed.ansiForegroundColorCode(true)
  blue = fgBlue.ansiForegroundColorCode(true)
  green = fgGreen.ansiForegroundColorCode(true)
  reset = ansiResetCode

var line: string
while true:
  try:
    line = readLine(stdin)
    let matches = line.find(pattern)
    if matches.isSome:
      let
        m = matches.get
        timestamp = m.captures["timestamp"]
        level = m.captures["level"]
        version = m.captures["version"]
        logger = m.captures["logger"]
        remaining = m.captures["remaining"]
        currentContext: Context = (level, logger)

      if currentContext == previousContext and currentContext.level == "INFO" and currentContext.logger == "odoo.modules.loading":
        stdout.eraseLine()
      elif not first:
        echo()

      first = false
      stdout.write(fmt"{blue}{timestamp}{reset} {red}{level}{reset} {green}{version}{reset} {yellow}{logger}{reset}: {remaining}")
      stdout.flushFile()
      previousContext = currentContext
    else:
      echo line
      first = false
      previousContext = ("", "")
  except EOFError:
    break
