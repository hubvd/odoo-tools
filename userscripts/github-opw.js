// ==UserScript==
// @name           GitHub OPW Links
// @namespace      odoo.com
// @description    Change OPW tag to links to the ticket
// @include        https://github.com/odoo/*
// @include        https://github.com/odoo-dev/*
// @author         hubvd
// @run-at         document-end
// ==/UserScript==

(function () {
    "use strict"

    function replace_opw() {
        document
            .querySelectorAll(
                ".commit-desc pre, div.timeline-comment td.comment-body p"
            )
            .forEach(function (desc) {
                desc.innerHTML = desc.innerHTML.replace(
                    /\b(opw|task)\s*(?:-?id)?\s*(?:[:-](?:&gt;)?)?\s*#?(\d+)\b/gi,
                    function (match, qualifier, id) {
                        id = parseInt(id)
                        const offset = 1000000
                        if (qualifier === "opw" && id < offset) {
                            id += offset
                        }
                        let style
                        if (qualifier === "opw") {
                            style = {
                                "background-color": "#875A7B",
                            }
                        } else {
                            style = {
                                "background-color": "#369ff6",
                            }
                        }
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
                )
            })
    }

    window.addEventListener("load", function () {
        replace_opw()
    })
    // Now that GitHub use websockets, this may not works anymore...
    document.addEventListener("pjax:complete", function () {
        replace_opw()
    })
})()
