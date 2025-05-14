# abbr -a expand_odoo --position command --regex 'o[sScCdDmpa]*' --function expand_odoo
function expand_odoo
    set out odoo
    set addons
    set parts (string split '' $argv[1])[2..]
    for part in $parts
        switch $part
            case d
                set -a out --drop
            case s
                set -a addons web_studio
            case S
                set -a addons stock
            case c
                set -a addons contacts
            case C
                set -a addons crm
            case D
                set -a addons documents
            case m
                set -a addons mrp
            case p
                set -a addons project
            case a
                if test (worktree version || worktree default version) -ge 17.4
                    set -a addons accountant
                else
                    set -a addons account_accountant
                end
        end
    end
    if set -q addons[1]
        set -a out -i (string join , $addons)
    end
    echo -- $out
end
