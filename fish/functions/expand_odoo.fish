# abbr -a expand_odoo --position command --regex 'o[sScCdDmpa]*' --function expand_odoo
function expand_odoo
    set out odoo
    set addons
    set parts (string split '' $argv[1])[2..]
    for part in $parts
        switch $part
            case i
                set init
            case T
                set tags
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
    if set -q addons[1] || set -q init
        set -a out -i (string join , $addons)
    end
    if set -q tags
        set -a out --test-tags
        set clipboard_types (wl-paste -l)

        for type in 'text/plain;charset=utf-8' text/plain UTF8_STRING TEXT STRING
            contains -- $type $clipboard_types
            and set clipboard "$(wl-paste -n -t $type 2>/dev/null)"
            and break
        end

        and string match -q 'test*' "$clipboard"
        and set -a out ".$clipboard"
    end
    echo -- $out
end
