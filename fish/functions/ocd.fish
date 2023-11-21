function ocd
  if set -l current (worktree current path)
    set -l paths (path filter -d $current $current/odoo $current/enterprise $current/design-themes)
    set -l len (count $paths)
    set -l current (contains -i $PWD $paths || echo $len)
    set -l next (math "($current) % $len + 1")
    cd $paths[$next]
  else
    set -l paths (worktree list path | string replace $HOME '~')
    set -l choice "$(string join0 $paths | fzf --read0)" && cd (string replace '~' $HOME $choice)
  end
end

