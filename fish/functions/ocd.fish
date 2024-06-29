function ocd
  if set -l current (worktree current path)
    set -l paths (path filter -d $current $current/odoo $current/enterprise $current/design-themes)
    set -l len (count $paths)
    set -l current (contains -i $PWD $paths || echo $len)
    set -l next (math "($current) % $len + 1")
    cd $paths[$next]
  else
    set -e workspace_names workspace_paths
    worktree list Fish | while read line
      eval $line
      set -a workspace_names $workspace_name
      set -a workspace_paths $workspace_path
    end
    set -l choice "$(string join0 $workspace_names | fzf --read0)"
    and set i (contains -i $choice $workspace_names)
    and cd $workspace_paths[$i]
    and echo
    and repo status
  end
end

