package com.github.hubvd.odootools.actions.git

import com.github.hubvd.odootools.workspace.Workspace
import java.lang.foreign.Arena
import java.lang.foreign.GroupLayout
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.*
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists

class Repository(
    val path: Path,
    internal val arena: Arena,
    internal val proxy: LibGit,
    internal val address: MemorySegment,
) : AutoCloseable {

    fun head() = GitReference(
        arena.allocate(ADDRESS).apply {
            proxy.repository_head(this, address)
        }.get(ADDRESS, 0).reinterpret(arena, proxy::reference_free),
        this,
    )

    // TODO: should accept git_object
    fun shortId(ref: GitReference): String {
        val obj = arena.allocate(ADDRESS)
        proxy.reference_peel(obj, ref.address, -2)

        val buff = arena.allocate(GitBuff.layout)
        proxy.object_short_id(buff, obj.get(ADDRESS, 0))

        return (GitBuff.layout.varHandle(groupElement("ptr")).get(buff) as MemorySegment).reinterpret(Long.MAX_VALUE)
            .getUtf8String(0)!!
    }

    // TODO: pass options ?
    fun status(): GitStatusList {
        val options = arena.allocate(GitStatusOptions.layout)
        proxy.status_options_init(options)
        GitStatusOptions.layout.varHandle(groupElement("show"))
            .set(options, Const.GIT_STATUS_SHOW_INDEX_AND_WORKDIR)

        MemorySegment.copy(
            GitStrArray.from(arena, arrayOf("*", "!*.po", "!*.pot")),
            0L,
            options,
            16,
            GitStrArray.layout.byteSize()
        )

        val out = arena.allocate(ADDRESS)
        proxy.status_list_new(out, address, options)
        return GitStatusList(out.get(ADDRESS, 0).reinterpret(arena, proxy::status_list_free), this)
    }

    fun findBranch(name: String, type: GitBranchType = GitBranchType.LOCAL): GitReference? {
        val ref = arena.allocate(ADDRESS)
        try {
            proxy.branch_lookup(
                ref,
                address,
                arena.allocateUtf8String(name),
                type.ordinal + 1,
            )
        } catch (e: LibGitError) {
            if (e.code == -3) {
                return null
            } else {
                throw e
            }
        }
        return GitReference(ref.get(ADDRESS, 0), this)
    }

    fun createBranch(name: String, commit: GitCommit): GitReference {
        val ref = arena.allocate(ADDRESS)
        proxy.branch_create(
            ref,
            address,
            arena.allocateUtf8String(name),
            commit.address,
            false,
        )
        return GitReference(ref.get(ADDRESS, 0), this)
    }

    fun checkoutBranch(ref: GitReference) {
        val options = arena.allocate(GitCheckoutOptions.layout)
        GitCheckoutOptions.layout.varHandle(groupElement("version")).set(options, 1)
        GitCheckoutOptions.layout.varHandle(groupElement("checkout_strategy")).set(options, 1)
        val res = proxy.checkout_tree(address, ref.target()!!.commit().address, options)
        if (res != 0) TODO()
        proxy.repository_set_head(
            address,
            arena.allocateUtf8String(ref.name()),
        )
    }

    fun remoteList(): List<GitRemote> {
        val strArray = arena.allocate(GitStrArray.layout).reinterpret(arena, proxy::strarray_dispose)
        proxy.remote_list(strArray, address)
        val remoteNames = GitStrArray.read(strArray)
        return remoteNames.map {
            val remote = arena.allocate(ADDRESS)
            proxy.remote_lookup(remote, address, arena.allocateUtf8String(it))
            GitRemote(remote.get(ADDRESS, 0).reinterpret(arena, proxy::remote_free), this)
        }
    }

    override fun close() {
        arena.close()
    }

    companion object {
        fun open(path: Path): Repository {
            val proxy = LibGitLoader.proxy
            val arena = Arena.ofConfined()
            val address = arena.allocate(ADDRESS).apply {
                proxy.repository_open(this, arena.allocateUtf8String(path.toString()))
            }.get(ADDRESS, 0)

            return Repository(
                path,
                arena,
                proxy,
                address.reinterpret(arena, proxy::repository_free),
            )
        }
    }
}

fun Workspace.currentRepository(): Repository? {
    val repoPath = path.relativize(Path(System.getProperty("user.dir")))
        .subpath(0, 1)
        .takeIf { it.toString().isNotEmpty() }
        ?.let { path / it }
        ?.takeIf { (it / ".git").exists() }
        ?: return null
    return Repository.open(repoPath)
}

enum class GitBranchType {
    LOCAL,
    REMOTE,
    ALL,
}

object GitStatusOptions {
    val layout = MemoryLayout.structLayout(
        JAVA_INT.withName("version"),
        JAVA_INT.withName("show"),
        JAVA_INT.withName("flags"),
        MemoryLayout.paddingLayout(4),
        GitStrArray.layout.withName("pathspec"),
        ADDRESS.withName("baseline"),
        JAVA_SHORT.withName("rename_threshold"),
        MemoryLayout.paddingLayout(6),
    )!!
}

enum class GitStatusFlags {
    GIT_STATUS_OPT_INCLUDE_UNTRACKED,
    GIT_STATUS_OPT_INCLUDE_IGNORED,
    GIT_STATUS_OPT_INCLUDE_UNMODIFIED,
    GIT_STATUS_OPT_EXCLUDE_SUBMODULES,
    GIT_STATUS_OPT_RECURSE_UNTRACKED_DIRS,
    GIT_STATUS_OPT_DISABLE_PATHSPEC_MATCH,
    GIT_STATUS_OPT_RECURSE_IGNORED_DIRS,
    GIT_STATUS_OPT_RENAMES_HEAD_TO_INDEX,
    GIT_STATUS_OPT_RENAMES_INDEX_TO_WORKDIR,
    GIT_STATUS_OPT_SORT_CASE_SENSITIVELY,
    GIT_STATUS_OPT_SORT_CASE_INSENSITIVELY,
    GIT_STATUS_OPT_RENAMES_FROM_REWRITES,
    GIT_STATUS_OPT_NO_REFRESH,
    GIT_STATUS_OPT_UPDATE_INDEX,
    GIT_STATUS_OPT_INCLUDE_UNREADABLE,
    GIT_STATUS_OPT_INCLUDE_UNREADABLE_AS_UNTRACKED,
}

object GitCheckoutOptions {
    val layout: GroupLayout = MemoryLayout.structLayout(
        JAVA_INT.withName("version"),
        JAVA_INT.withName("checkout_strategy"),
        JAVA_INT.withName("disable_filters"),
        JAVA_INT.withName("dir_mode"),
        JAVA_INT.withName("file_mode"),
        JAVA_INT.withName("file_open_flags"),
        JAVA_INT.withName("notify_flags"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("notify_cb"),
        ADDRESS.withName("notify_payload"),
        ADDRESS.withName("progress_cb"),
        ADDRESS.withName("progress_payload"),
        GitStrArray.layout.withName("paths"),
        ADDRESS.withName("baseline"),
        ADDRESS.withName("baseline_index"),
        ADDRESS.withName("target_directory"),
        ADDRESS.withName("ancestor_label"),
        ADDRESS.withName("our_label"),
        ADDRESS.withName("their_label"),
        ADDRESS.withName("perfdata_cb"),
        ADDRESS.withName("perfdata_payload"),
    ).withName("git_checkout_options")
}

object Const {
    const val GIT_STATUS_SHOW_INDEX_AND_WORKDIR = 0
    const val GIT_STATUS_SHOW_INDEX_ONLY = 1
    const val GIT_STATUS_SHOW_WORKDIR_ONLY = 2
}

class GitStatusList(private val address: MemorySegment, private val repo: Repository) {
    fun count(): Long {
        return repo.proxy.status_list_entrycount(address)
    }
}

object GitStrArray {
    val layout = MemoryLayout.structLayout(
        ADDRESS.withName("strings"),
        JAVA_LONG.withName("count"),
    ).withName("git_strarray")!!
    val countHandle = layout.varHandle(groupElement("count"))!!
    val stringsHandle = layout.varHandle(groupElement("strings"))!!

    fun read(strArray: MemorySegment): Array<String> {
        val count = GitStrArray.countHandle.get(strArray) as Long
        val strings = (GitStrArray.stringsHandle.get(strArray) as MemorySegment).reinterpret(
            ADDRESS.byteSize() * count,
        )
        return Array(count.toInt()) {
            strings.getAtIndex(ADDRESS, it.toLong()).reinterpret(Long.MAX_VALUE).getUtf8String(0)
        }
    }

    fun from(arena: Arena, values: Array<String>): MemorySegment {
        val strArray = arena.allocate(GitStrArray.layout)
        val strings = arena.allocate(ADDRESS.byteSize() * values.size)
        values.forEachIndexed { index, s ->
            strings.setAtIndex(ADDRESS, index.toLong(), arena.allocateUtf8String(s))
        }
        stringsHandle.set(strArray, strings)
        countHandle.set(strArray, values.size)
        return strArray
    }
}

object GitError {
    val layout = MemoryLayout.structLayout(
        ADDRESS.withName("message"),
        JAVA_INT.withName("klass"),
        MemoryLayout.paddingLayout(4),
    )!!
}

object GitBuff {
    val layout = MemoryLayout.structLayout(
        ADDRESS.withName("ptr"),
        JAVA_LONG.withName("reserved"),
        JAVA_LONG.withName("size"),
    )!!
}

class GitReference(val address: MemorySegment, private val repo: Repository) {
    fun branchName(): String? {
        val res = repo.arena.allocate(ADDRESS).apply {
            repo.proxy.branch_name(this, address)
        }.get(ADDRESS, 0)
        if (res == MemorySegment.NULL) return null
        return res.reinterpret(Long.MAX_VALUE).getUtf8String(0)!!
    }

    fun name() = repo.proxy.reference_name(address).reinterpret(Long.MAX_VALUE).getUtf8String(0)!!

    fun shorthand() = repo.proxy.reference_shorthand(address).reinterpret(Long.MAX_VALUE).getUtf8String(0)!!

    fun upstream(): GitReference? {
        if (!isBranch()) return null

        val out = repo.arena.allocate(ADDRESS)
        try {
            repo.proxy.branch_upstream(out, address)
        } catch (e: LibGitError) {
            if (e.code == -3) {
                return null
            } else {
                throw e
            }
        }
        return GitReference(
            out.get(ADDRESS, 0).reinterpret(repo.arena, repo.proxy::reference_free),
            repo,
        )
    }

    fun target(): GitOid? {
        val res = repo.proxy.reference_target(address)
        if (res == MemorySegment.NULL) return null
        return GitOid(res, repo)
    }

    fun isBranch(): Boolean = repo.proxy.reference_is_branch(address)
}

class GitOid(private val address: MemorySegment, private val repo: Repository) {
    fun commit(): GitCommit {
        val out = repo.arena.allocate(ADDRESS)
        repo.proxy.commit_lookup(out, repo.address, address)
        return GitCommit(
            out.get(ADDRESS, 0).reinterpret(repo.arena, repo.proxy::commit_free),
            repo,
        )
    }

    // TODO: move this ?
    fun aheadBehind(other: GitOid): Pair<Long, Long> {
        val ahead = repo.arena.allocate(JAVA_LONG) // size_t: Long ???
        val behind = repo.arena.allocate(JAVA_LONG)
        repo.proxy.graph_ahead_behind(
            ahead,
            behind,
            repo.address,
            this.address,
            other.address,
        )
        return Pair(
            ahead.get(JAVA_LONG, 0),
            behind.get(JAVA_LONG, 0),
        )
    }
}

class GitCommit(val address: MemorySegment, private val repo: Repository) {

    fun id(): GitOid {
        return GitOid(repo.proxy.commit_id(address), repo)
    }
}

class GitRemote(private val address: MemorySegment, private val repo: Repository) {
    fun name(): String {
        return repo.proxy.remote_name(address).reinterpret(Long.MAX_VALUE).getUtf8String(0)
    }

    fun fetchUrl(): String {
        return repo.proxy.remote_url(address).reinterpret(Long.MAX_VALUE).getUtf8String(0)
    }

    fun pushUrl(): String? {
        return repo.proxy.remote_pushurl(address)
            .takeIf { it != MemorySegment.NULL }
            ?.reinterpret(Long.MAX_VALUE)
            ?.getUtf8String(0)
    }
}
