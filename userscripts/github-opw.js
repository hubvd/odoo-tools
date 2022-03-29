// ==UserScript==
// @name           GitHub OPW Links
// @namespace      odoo.com
// @description    Change OPW tag to links to the ticket
// @include        https://github.com/odoo/*
// @include        https://github.com/odoo-dev/*
// @author         hubvd
// @run-at         document-end
// ==/UserScript==

"use strict"

function createLink(match, qualifier, id) {
    id = parseInt(id)
    const offset = 1000000
    if (qualifier === "opw" && id < offset) id += offset
    let style = qualifier === "opw"
        ? {"background-color": "#875A7B",}
        : {"background-color": "#369ff6"}

    style = Object.assign(
        {
            color: "white",
            padding: "0 0.2em",
            "text-decoration": "none",
            "border-radius": "2px",
        },
        style
    )
    const styleStr = Object.entries(style)
        .map((e) => e.join(":"))
        .join(";")
    return `<a href="https://www.odoo.com/web#view_type=form&model=project.task&id=${id}" style="${styleStr}" target="_blank">${match}</a>`
}

function replaceOpw() {
    Array.from(document.querySelectorAll(".commit-desc pre, div.timeline-comment div.comment-body"))
        .flatMap(e => Array.from(e.childNodes))
        .filter(e => !(e instanceof Text))
        .filter(el => !el.hasAttribute('opw'))
        .forEach(desc => {
            desc.innerHTML = desc.innerHTML.replace(/\b(opw|task)\s*(?:-?id)?\s*(?:[:-](?:&gt;)?)?\s*#?(\d+)\b/gi, createLink)
            desc.setAttribute('opw', true)
        })
}

window.addEventListener("load", replaceOpw)
window.addEventListener("message", replaceOpw)
window.addEventListener('pjax:end', replaceOpw)
window.addEventListener('urlchange', () => {
    setTimeout(replaceOpw, 200)
});