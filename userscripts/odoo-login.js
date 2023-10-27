// ==UserScript==
// @name        Login odoo admin/demo
// @match       https://*.runbot*.odoo.com/web/login
// @match       https://*.runbot*.odoo.com/*/web/login
// @match       http://localhost:*/web/login
// @match       http://localhost:*/*/web/login
// @match       http://127.*.*.*:*/web/login
// @match       http://127.*.*.*:*/*/web/login
// @grant       none
// @version     1.0
// @author      hubvd
// ==/UserScript==

window.addEventListener('load', () => {
  const login = document.querySelector('.oe_login_form button[type="submit"]')
  for (let text of ['admin', 'demo', 'portal']) {
    const el = document.createElement('button')
    el.innerText = text
    el.classList.add('btn', 'btn-info', 'btn-block')
    el.addEventListener('click', (e) => {
      document.getElementById('login').value = text
      document.getElementById('password').value = text
      e.preventDefault()
      document.querySelector('form.oe_login_form').submit()
    })
    login.parentNode.insertBefore(el, login)
  }
})

