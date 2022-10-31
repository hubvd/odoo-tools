function __odoo_completions
    commandline -cop | odoocomplete "$(commandline -ot)" "$(workspace -p)"
end

complete -f -c odoo -a "(__odoo_completions)"
