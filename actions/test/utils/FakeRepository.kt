package com.github.hubvd.odootools.actions.utils

class FakeRepository(log: String) : Repository {
    private val commits = log
        .lineSequence()
        .map { Commit(it.substringBefore(' '), it.substringAfter(' ')) }
        .toList()

    override fun commitTitle(hash: String) = commits.first { it.hash == hash }.title

    override fun switch(hash: String) {
        // NOOP
    }

    override fun commitsBetween(oldHash: String, newHash: String): List<Commit> = if (oldHash == newHash) {
        listOf(commits.first { it.hash == newHash })
    } else {
        commits.asSequence()
            .dropWhile { it.hash != newHash }
            .takeWhile { it.hash != oldHash }
            .toList()
    }
}
