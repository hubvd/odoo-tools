function __odoo_completions
    commandline -cop | odoocomplete (commandline -t) "$(workspace -p)"
end

complete -f -c odoo -a "(__odoo_completions)"
