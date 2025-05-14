package com.github.hubvd.odootools.actions.git

import com.github.hubvd.odootools.workspace.Workspace
import java.lang.foreign.Arena
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

    // TODO: extract peel should accept git_object
    fun shortId(ref: GitReference): String {
        val obj = arena.allocate(ADDRESS)
        proxy.reference_peel(obj, ref.address, git_object_t.GIT_OBJECT_ANY.value)

        val buff = arena.allocate(git_buff.layout)
        proxy.object_short_id(buff, obj.get(ADDRESS, 0))

        return (
            git_buff.layout.varHandle(groupElement("ptr"))
                .get(buff, 0) as MemorySegment
            ).reinterpret(Long.MAX_VALUE)
            .getString(0)!!
    }

    // TODO: merge with previous function
    fun shortId(ref: GitObject): String {
        val buff = arena.allocate(git_buff.layout)
        proxy.object_short_id(buff, ref.address)

        return (
            git_buff.layout.varHandle(groupElement("ptr"))
                .get(buff, 0) as MemorySegment
            ).reinterpret(Long.MAX_VALUE)
            .getString(0)!!
    }

    // TODO: pass options ?
    fun status(): GitStatusList {
        val options = arena.allocate(git_status_options.layout)
        proxy.status_options_init(options)
        git_status_options.layout.varHandle(groupElement("show"))
            .set(options, 0, git_status_show_t.GIT_STATUS_SHOW_INDEX_AND_WORKDIR.value)

        MemorySegment.copy(
            git_strarray.from(arena, emptyArray()),
            0L,
            options,
            16,
            git_strarray.layout.byteSize(),
        )

        val out = arena.allocate(ADDRESS)
        proxy.status_list_new(out, address, options)
        return GitStatusList(out.get(ADDRESS, 0).reinterpret(arena, proxy::status_list_free), this)
    }

    fun findBranch(name: String, type: git_branch_t = git_branch_t.GIT_BRANCH_LOCAL): GitReference? {
        val ref = arena.allocate(ADDRESS)
        try {
            proxy.branch_lookup(
                ref,
                address,
                arena.allocateFrom(name),
                type.value,
            )
        } catch (e: LibGitError) {
            if (e.code == git_error_code.GIT_ENOTFOUND) {
                return null
            } else {
                throw e
            }
        }
        return GitReference(ref.get(ADDRESS, 0), this)
    }

    fun findReference(name: String): GitReference? {
        val ref = arena.allocate(ADDRESS)
        try {
            proxy.reference_dwim(
                ref,
                address,
                arena.allocateFrom(name),
            )
        } catch (e: LibGitError) {
            if (e.code == git_error_code.GIT_ENOTFOUND) {
                return null
            } else {
                throw e
            }
        }
        return GitReference(ref.get(ADDRESS, 0), this)
    }

    fun revParse(name: String): GitObject? {
        val rev = arena.allocate(ADDRESS)
        try {
            proxy.revparse_single(
                rev,
                address,
                arena.allocateFrom(name),
            )
        } catch (e: LibGitError) {
            if (e.code == git_error_code.GIT_ENOTFOUND) {
                return null
            } else {
                throw e
            }
        }
        return GitObject(rev.get(ADDRESS, 0), this)
    }

    fun createBranch(name: String, commit: GitCommit): GitReference {
        val ref = arena.allocate(ADDRESS)
        proxy.branch_create(
            ref,
            address,
            arena.allocateFrom(name),
            commit.address,
            false,
        )
        return GitReference(ref.get(ADDRESS, 0), this)
    }

    fun checkoutBranch(ref: GitReference) {
        checkoutTree(ref)
        proxy.repository_set_head(
            address,
            arena.allocateFrom(ref.name()),
        )
    }

    fun checkoutTree(ref: GitReference) {
        val options = arena.allocate(git_checkout_options.layout)
        git_checkout_options.layout.varHandle(groupElement("version")).set(options, 0, 1)
        git_checkout_options.layout.varHandle(groupElement("checkout_strategy")).set(options, 0, 1)
        proxy.checkout_tree(address, ref.target()!!.commit().address, options)
    }

    fun remoteList(): List<GitRemote> {
        val strArray = arena.allocate(git_strarray.layout).reinterpret(arena, proxy::strarray_dispose)
        proxy.remote_list(strArray, address)
        val remoteNames = git_strarray.read(strArray)
        return remoteNames.map {
            val remote = arena.allocate(ADDRESS)
            proxy.remote_lookup(remote, address, arena.allocateFrom(it))
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
                proxy.repository_open(this, arena.allocateFrom(path.toString()))
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

fun Workspace.currentRepositoryPath(): Path? = path.relativize(Path(System.getProperty("user.dir")))
    .subpath(0, 1)
    .takeIf { it.toString().isNotEmpty() }
    ?.let { path / it }
    ?.takeIf { (it / ".git").exists() }

fun Workspace.currentRepository(): Repository? = currentRepositoryPath()?.let { Repository.open(it) }

class GitStatusList(private val address: MemorySegment, private val repo: Repository) {
    fun count(): Long = repo.proxy.status_list_entrycount(address)
}

class GitObject(val address: MemorySegment, private val repo: Repository) {
    fun oid(): GitOid = GitOid(repo.proxy.object_id(address), repo)
}

class GitReference(val address: MemorySegment, private val repo: Repository) {
    fun branchName(): String? {
        val res = repo.arena.allocate(ADDRESS).apply {
            repo.proxy.branch_name(this, address)
        }.get(ADDRESS, 0)
        if (res == MemorySegment.NULL) return null
        return res.reinterpret(Long.MAX_VALUE).getString(0)!!
    }

    fun name() = repo.proxy.reference_name(address).reinterpret(Long.MAX_VALUE).getString(0)!!

    fun shorthand() = repo.proxy.reference_shorthand(address).reinterpret(Long.MAX_VALUE).getString(0)!!

    fun upstream(): GitReference? {
        if (!isBranch()) return null

        val out = repo.arena.allocate(ADDRESS)
        try {
            repo.proxy.branch_upstream(out, address)
        } catch (e: LibGitError) {
            if (e.code == git_error_code.GIT_ENOTFOUND) {
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

    fun setTarget(target: GitOid) {
        val out = repo.arena.allocate(ADDRESS)
        repo.proxy.reference_set_target(out, address, target.address, repo.arena.allocateFrom(""))

        // TODO: return new GitReference and destroy this one
    }

    fun isBranch(): Boolean = repo.proxy.reference_is_branch(address)
}

class GitOid(val address: MemorySegment, private val repo: Repository) {
    fun commit(): GitCommit {
        val out = repo.arena.allocate(ADDRESS)
        repo.proxy.commit_lookup(out, repo.address, address)
        return GitCommit(
            out.get(ADDRESS, 0).reinterpret(repo.arena, repo.proxy::commit_free),
            repo,
        )
    }

    fun hash(): String? = repo.proxy.oid_tostr_s(address)
        .takeIf { it != MemorySegment.NULL }!!
        .reinterpret(Long.MAX_VALUE)
        ?.getString(0)

    fun isEqual(other: GitOid) = repo.proxy.oid_equal(this.address, other.address)

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

    fun id(): GitOid = GitOid(repo.proxy.commit_id(address), repo)
}

class GitRemote(private val address: MemorySegment, private val repo: Repository) {
    fun name(): String = repo.proxy.remote_name(address).reinterpret(Long.MAX_VALUE).getString(0)

    fun fetchUrl(): String = repo.proxy.remote_url(address).reinterpret(Long.MAX_VALUE).getString(0)

    fun pushUrl(): String? = repo.proxy.remote_pushurl(address)
        .takeIf { it != MemorySegment.NULL }
        ?.reinterpret(Long.MAX_VALUE)
        ?.getString(0)
}
