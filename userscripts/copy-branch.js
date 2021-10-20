// ==UserScript==
// @name        Copy branch without remote
// @include     https://github.com/odoo/odoo/pull/*
// @version     1.0
// @author      hubvd
// ==/UserScript==

window.addEventListener('load', document.querySelector('.js-copy-branch').setAttribute('value', document.querySelector('.commit-ref.head-ref span + span').innerText))

