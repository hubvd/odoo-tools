// ==UserScript== //
// @name odoo.com patches
// @match https://www.odoo.com/web
// @match https://www.odoo.com/web?*
// @match https://www.odoo.com/odoo
// @match https://www.odoo.com/odoo/*
// @match https://www.odoo.com/odoo?*
// ==/UserScript==

function resurrectProgressBar({ patch, KanbanArchParser, RelationalModel }) {
    patch(KanbanArchParser.prototype, {
        parse(xmlDoc, models, modelName) {
            if (modelName === "project.task") {
                const container = document.createElement("div");
                container.innerHTML = `<progressbar field="state" colors='{"1_done": "success-done", "1_canceled": "danger", "03_approved": "success", "02_changes_requested": "warning", "04_waiting_normal": "info", "01_in_progress": "200" }'/>`;
                xmlDoc.insertBefore(container.firstChild, xmlDoc.querySelector("templates"));
            }
            return super.parse(...arguments);
        },
    });
}

function increaseOpenGroupLimit({ RelationalModel, patch }) {
    patch(RelationalModel, {
        MAX_NUMBER_OPENED_GROUPS: 25,
        DEFAULT_OPEN_GROUP_LIMIT: 25,
    });
}

async function getInstances(name, dependencies) {
    const { promise, resolve } = Promise.withResolvers();
    odoo.define(
        name,
        dependencies.map((e) => e[0]),
        (require) => {
            const resolvedDependencies = {};
            dependencies.forEach(([name, ...required]) => {
                const module = require(name);
                required.forEach((name) => (resolvedDependencies[name] = module[name]));
            });
            resolve(resolvedDependencies);
        }
    );
    return promise;
}

const patches = [resurrectProgressBar, increaseOpenGroupLimit];

getInstances("odoosoup.com", [
    ["@web/core/utils/patch", "patch"],
    ["@web/views/kanban/kanban_arch_parser", "KanbanArchParser"],
    ["@web/model/relational_model/relational_model", "RelationalModel"],
]).then((dependencies) => {
    patches.forEach((patch) => patch.call(this, dependencies));
});
