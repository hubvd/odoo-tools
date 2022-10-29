import sequtils, strutils, os, sugar, osproc, options, json

type
  TestTag = tuple
    prefix: string
    module: Option[string]
    class: Option[string]
    def: Option[string]
  Completion = tuple
    names: seq[string]
    hasValue: bool
    provider: string -> seq[string]
  Addon = tuple
    name: string
    path: string

proc newCompletion(names: seq[string], provider: string -> seq[
    string]): Completion =
  (names, true, provider)

proc newCompletion(names: seq[string], hasValue: bool = false): Completion =
  (names, hasValue, (proc (_: string): seq[string] {.closure.} = discard))

if paramCount() < 2 or paramStr(2) == "":
  quit(1)

let
  input = stdin.lines.toSeq
  currentToken = paramStr(1)
  workspace = paramStr(2)
  last = input[^1]

var
  token = currentToken
  equal = token.find('=')
  flagName = last

if token.startsWith("--") and equal > 0:
  flagName = token.substr(0, equal - 1)
  token = token.substr(equal + 1, token.len - 1)

iterator findAddons(): Addon =
  # FIXME: merge iterators
  for kind, path in walkDir(workspace / "odoo/addons"):
    if kind == pcDir and fileExists(path / "__manifest__.py"):
      yield (path.extractFileName, path)
  for kind, path in walkDir(workspace / "odoo/odoo/addons"):
    if kind == pcDir and fileExists(path / "__manifest__.py"):
      yield (path.extractFileName, path)
  if not input.contains("--community"):
    for kind, path in walkDir(workspace / "enterprise"):
      if kind == pcDir and fileExists(path / "__manifest__.py"):
        yield (path.extractFileName, path)

proc parseTestTag(tag: string): TestTag =
  var parts = [
    some(""),     # prefix
    none(string), # module
    none(string), # class
    none(string), # def
  ]
  var idx: int
  var current: string
  proc append() =
    parts[idx + 1] = some(current)
    current = ""

  const seps = ['/', ':', '.']
  for (token, isSep) in tag.tokenize({'/', ':', '.'}):
    if not isSep: current = token
    else: idx = seps.find(token[0])
    append()
  result = (parts[0].get(""), parts[1], parts[2], parts[3])

proc completeTestTags(token: string): seq[string] =
  let tags = token.split(',')
  let lastTag = tags[^1]
  var previousTags: string
  if tags.len > 1:
    previousTags = tags[0..tags.len - 2].join(",") & ','

  # FIXME: clean this mess
  let (prefix, module, class, def) = parseTestTag(lastTag)

  var addonPath: string
  if module.isSome:
    for (name, path) in findAddons():
      if name == module.get:
        addonPath = path
        break

  if class.isNone and def.isNone:
    for i in findAddons():
      result.add(previousTags & prefix & '/' & i.name)
  elif module.isSome and def.isNone:
    if addonPath == "": return
    for line in execProcess("ctags", args = ["--kinds-Python=c", "--recurse",
        "--output-format=json", addonPath / "tests/"], options = {
            poUsePath}).strip(
        leading = false).split("\n"):
      let json = parseJson(line)
      result.add(previousTags & prefix & '/' & module.get & ':' & json["name"].getStr)
  elif module.isSome:
    if addonPath == "": return
    if class.isNone:
      for line in execProcess("ctags", args = ["--kinds-Python=f", "--recurse",
          "--output-format=json", addonPath / "tests/"], options = {
              poUsePath}).strip(
          leading = false).split("\n"):
        let json = parseJson(line)
        let name = json["name"].getStr
        if name.startsWith("test_"):
          result.add(previousTags & prefix & '/' & module.get & '.' & name)
    else:
      for line in execProcess("ctags", args = ["--kinds-Python=cmf",
          "--recurse", "--output-format=json", addonPath / "tests/"],
          options = {poUsePath}).strip(
          leading = false).split("\n"):
        let json = parseJson(line)
        let name = json["name"].getStr
        if name.startsWith("test_") and json.contains("scopeKind") and json[
            "scopeKind"].getStr == "class" and json.contains("scope") and json[
            "scope"].getStr == class.get:
          result.add(previousTags & prefix & '/' & module.get & ':' &
              class.get & '.' & name)


proc completeQUnitTests(): seq[string] =
  execProcess(
    "rg", args = [
      "-IPt",
      "js",
      "-or",
      "$name",
      """(?<=QUnit\.module\(|QUnit\.test\(|QUnit\.debug\(|QUnit\.only\()(?P<quote>["'`])(?P<name>.+)(?=(?P=quote))""",
      workspace / "odoo/addons/web/static/tests"
    ],
    options = {poUsePath}
  ).split('\n')

proc completeAddons(token: string): seq[string] =
  var choices: seq[string]
  for i in findAddons():
    choices.add(i.name)
  var currents = token.split(',')
  if currents[^1] notin choices:
    currents.delete(currents.len - 1)
  var c = ""
  if currents.len > 0:
    c = currents.join(",") & ","
  for choice in choices:
    if choice notin currents:
      result.add c & choice

let completions = [
  newCompletion(@["-i", "--init"], (token: string) => completeAddons(token)),
  newCompletion(@["-u", "--update"], (token: string) => completeAddons(token)),
  newCompletion(@["--test-qunit"], (token: string) => completeQUnitTests()),
  newCompletion(@["--test-tags"], (token: string) => completeTestTags(token)),
  newCompletion(@["--drop"]),
  newCompletion(@["-p", "--http-port"], true),
  newCompletion(@["--log-sql"]),
  newCompletion(@["--stop-after-init"]),
  newCompletion(@["-h", "--help"]),
  newCompletion(@["--community"]),
  newCompletion(@["--failslow"]),
]

var completed = false
block outer:
  for (names, hasValue, provider) in completions:
    for name in names:
      if flagName == name and hasValue:
        for i in provider token:
          if equal > 0:
            echo flagName & '=' & i
          else:
            echo i
        completed = true
        break outer

if not completed:
  for (names, _, _) in completions:
    if all(names, (name: string) => name notin input):
      echo names[0]
      if names.len > 1 and currentToken.startsWith("--") and names[
          1].startsWith(currentToken):
        echo names[1]

