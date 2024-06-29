// ==UserScript==
// @name        Login odoo admin/demo

// @match       https://*.runbot*.odoo.com/web/login
// @match       https://*.runbot*.odoo.com/*/web/login

// @match       http://localhost:*/web/login
// @match       http://localhost:*/*/web/login
//
// @match       http://*.localhost:*/web/login
// @match       http://*.localhost:*/*/web/login

// @match       http://127.*.*.*:*/web/login
// @match       http://127.*.*.*:*/*/web/login

// @grant       none
// @version     1.0
// @author      hubvd
// ==/UserScript==

login?.addEventListener("change", ({ target }) => {
  const user = { a: "admin", d: "demo", p: "portal" }[target.value];
  if (user) target.value = target.form.password.value = user;
});

