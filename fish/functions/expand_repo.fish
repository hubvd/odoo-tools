# abbr -a --regex '^ra?[sfur]+' --function expand_repo -- expand_repo
function expand_repo
    if test $argv[1] = ruff
        return 1
    end

    set out repo
    set parts (string split '' $argv[1])[2..]
    for part in $parts
        switch $part
            case a
                set -a out -a
            case s
                set -a out status
            case f
                set -a out fetch
            case u
                set -a out update
            case r
                set -a out rebase
        end
    end
    echo -- $out
end
