function __odoo_completions
    commandline -cop | odoocomplete "$(commandline -ot)" (worktree path || worktree default path)
end

complete -f -c odoo -a "(__odoo_completions)"
