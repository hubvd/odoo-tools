@file:Suppress("FunctionName", "LocalVariableName")

package com.github.hubvd.odootools.ffi.libffi.test

import com.github.hubvd.odootools.ffi.libffi.annotations.Prefix
import com.github.hubvd.odootools.ffi.libffi.annotations.Self

@Self(0)
@Prefix("ts_node_")
interface TSNode {
    fun language()

    fun grammar_type(): String

    fun grammar_symbol()

    fun start_byte(): Int

    fun start_point(): TSPoint

    fun end_byte(): Int

    fun end_point(): TSPoint

    fun string(): String

    fun is_null(): Boolean

    fun is_named(): Boolean

    fun is_missing(): Boolean

    fun is_extra(): Boolean

    fun has_changes(): Boolean

    fun has_error(): Boolean

    fun is_error(): Boolean

    fun parse_state(): TSStateId

    fun next_parse_state(): TSStateId

    fun parent(): TSNode

    fun child(child_index: Int): TSNode

    fun field_name_for_child(child_index: Int): String

    fun child_count(): Int

    fun named_child(child_index: Int): TSNode

    fun child_by_field_name(name: String, length: Int): TSNode

    fun child_by_field_id(field_id: TSFieldId): TSNode

    fun next_sibling(): TSNode

    fun prev_sibling(): TSNode

    fun next_named_sibling(): TSNode

    fun prev_named_sibling(): TSNode

    fun first_child_for_byte(byte: Int): TSNode

    fun firs_named_child_for_byte(byte: Int): TSNode

    fun descendant_count(): Int

    fun descendant_for_byte_range(start: Int, end: Int): TSNode

    fun descendant_for_point_range(start: TSPoint, end: TSPoint): TSNode

    fun named_descendant_for_byte_range(start: Int, end: Int): TSNode

    fun named_descendant_for_point_range(start: TSPoint, end: TSPoint): TSNode

    fun edit(edit: TSInputEdit)

    fun eq(other: TSNode): Boolean
}

interface TSInputEdit

interface TSFieldId

interface TSPoint

interface TSStateId
