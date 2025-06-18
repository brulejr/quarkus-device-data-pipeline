/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.common.service.crud

import io.jrb.labs.common.model.Entity
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import java.util.UUID

class CrudService<E : Entity<E>, R : Any>(
    private val entityType: String,
    private val repository: PanacheMongoRepository<E>,
    private val guidGenerator: () -> String = { UUID.randomUUID().toString() },
    private val ownerGuidExtractor: suspend () -> String? = { null },
    private val toResource: suspend (E) -> R
) {

    suspend fun create(entity: E): CrudOutcome<R> {
        return runCatching {
            val entityToCreate: E = entity.withCreateInfo(guidGenerator(), ownerGuidExtractor())
            repository.persist(entityToCreate)
            findByGuid(entityToCreate.guid!!)
        }.getOrElse {
            CrudOutcome.Error("Failed to create $entityType: ${it.message}", it)
        }
    }

    suspend fun findByGuid(
        guid: String,
        filter: (E) -> Boolean = { true }
    ): CrudOutcome<R> {
        val ownerGuid = ownerGuidExtractor()
        val findFn = ownerGuid
            ?.let { repository.find("guid = ?1 and ownerGuid = ?2", guid, ownerGuid) }
            ?: repository.find("guid = ?1", guid)
        return runCatching {
            findFn.list().first { filter(it) }.let {
                CrudOutcome.Success(toResource(it))
            }
        }.getOrElse {
            if (it is NoSuchElementException) {
                CrudOutcome.NotFound(guid)
            } else {
                CrudOutcome.Error("Failed to get $entityType with ownerGuid $ownerGuid and $guid: ${it.message}", it)
            }
        }
    }

    suspend fun getAll(filter: (E) -> Boolean = { true }): CrudOutcome<List<R>> {
        val ownerGuid = ownerGuidExtractor()
        val findFn = ownerGuid
            ?.let { repository.find("ownerGuid = ?1", ownerGuid) }
            ?: repository.findAll()
        return runCatching {
            findFn.list().filter { filter(it) }.map { toResource(it) }.let {
                CrudOutcome.Success(it)
            }
        }.getOrElse {
            CrudOutcome.Error("Failed to get $entityType with ownerGuid $ownerGuid: ${it.message}", it)
        }
    }

    suspend fun updateByGuid(
        guid: String,
        filter: (E) -> Boolean = { true },
        updatefn: suspend (E) -> E
    ): CrudOutcome<R> {
        val ownerGuid = ownerGuidExtractor()
        val findFn = ownerGuid
            ?.let { repository.find("guid = ?1 and ownerGuid = ?2", guid, ownerGuid) }
            ?: repository.find("guid = ?1", guid)
        return runCatching {
            findFn.list().first { filter(it) }.let { existing ->
                repository.update(updatefn(existing.withUpdateInfo(ownerGuidExtractor())))
                findByGuid(guid)
            }
        }.getOrElse {
            if (it is NoSuchElementException) {
                CrudOutcome.NotFound(guid)
            } else {
                CrudOutcome.Error("Failed to update $entityType with ownerGuid $ownerGuid and guid $guid: ${it.message}", it)
            }
        }
    }

    suspend fun deleteByGuid(
        guid: String,
        filter: (E) -> Boolean = { true }
    ): CrudOutcome<Unit> {
        val ownerGuid = ownerGuidExtractor()
        val findFn = ownerGuid
            ?.let { repository.find("guid = ?1 and ownerGuid = ?2", guid, ownerGuid) }
            ?: repository.find("guid = ?1", guid)
        return runCatching {
            findFn.list().first { filter(it) }.let { existing ->
                repository.delete(existing)
                CrudOutcome.Success(Unit)
            }
        }.getOrElse {
            if (it is NoSuchElementException) {
                CrudOutcome.NotFound(guid)
            } else {
                CrudOutcome.Error("Failed to delete $entityType with ownerGuid $ownerGuid and guid $guid: ${it.message}", it)
            }
        }
    }

}