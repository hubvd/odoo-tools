import json, strutils, terminal, httpclient, os

let
    githubUsername = "hubvd"
    odooUsername = "huvw"

type
    PullRequest = tuple
        headRefName: string
        title: string
        url: string
        isDraft: bool
        commits: JsonNode
    Check = tuple
        state: string
        targetUrl: string
        context: string

const graphQueryTemplate = readFile("./query.graphql")
let graphQuery = graphQueryTemplate.replace("<INVOLVES>", githubUsername)
let client = newHttpClient()

var token: string
for line in (getHomeDir() / ".config/gh/hosts.yml").lines():
    let l = line.strip()
    if l.startsWith("oauth_token"):
        let parts = l.split(':')
        if len(parts) != 2:
            quit(1)
        token = parts[1].strip()
        break


stdout.styledWriteLine(styleBright, fgBlue, "Fetching data..")

client.headers = newHttpHeaders({"Authorization": "Bearer " & token})
let body = %*{"query": graphQuery}
let response = client.request("https://api.github.com/graphql", httpMethod = HttpPost, body = $body)

cursorUp 1
eraseLine()

if response.code != Http200:
    echo response.status
    echo response.body
    quit(1)

for pr in parseJson(response.body)["data"]["results"]["prs"].to(seq[PullRequest]):
    if not (odooUsername in pr.headRefName):
        continue

    let isDraft = pr.isDraft or pr.title.startsWith("[WIP]") or pr.title.startsWith("[DRAFT]")
    var status = "success"
    var pendings: seq[(string, string)]
    var failures: seq[(string, string)]

    for (state, url, name) in pr.commits["nodes"][0]["commit"]["status"]["contexts"].to(seq[Check]):
        if name == "ci/codeowner":
            if isDraft:
                continue

        if state == "PENDING" and status != "failure":
            pendings.add((name, url))
            status = "pending"

        if state == "FAILURE":
            failures.add((name, url))
            status = "failure"

    var color: ForegroundColor
    if status == "success":
        color = fgGreen
    elif status == "failure":
        color = fgRed
    else:
        color = fgBlue

    let draftStr = if isDraft: "[DRAFT]" else: ""
    stdout.styledWriteLine(draftStr, styleBright, color, pr.title)

    for (name, url) in pendings:
        stdout.styledWriteLine(styleBright, fgYellow, "[", name, "]", resetStyle, fgYellow, " ", url)
    for (name, url) in failures:
        stdout.styledWriteLine(styleBright, fgRed, "[", name, "]", resetStyle, fgRed, " ", url)

    echo pr.headRefName
    echo pr.url
    echo()
