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
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import io.quarkus.mongodb.panache.kotlin.PanacheQuery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class CrudServiceTest {

    private lateinit var repository: PanacheMongoRepository<TestEntity>
    private lateinit var panacheQuery: PanacheQuery<TestEntity>
    private lateinit var guidGenerator: () -> String
    private lateinit var ownerGuidExtractor: suspend () -> String?
    private lateinit var toResource: suspend (TestEntity) -> TestResource
    private lateinit var crudService: CrudService<TestEntity, TestResource>

    @BeforeEach
    fun setUp() {
        repository = mockk()
        panacheQuery = mockk()
        guidGenerator = mockk()
        ownerGuidExtractor = mockk()
        toResource = mockk()

        crudService = CrudService(
            entityType = "TestEntity",
            repository = repository,
            guidGenerator = guidGenerator,
            ownerGuidExtractor = ownerGuidExtractor,
            toResource = toResource
        )

        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `create should successfully create entity and return resource`() {
        runBlocking {
            val inputEntity = TestEntity("", "test-name")
            val generatedGuid = "generated-guid"
            val ownerGuid = "owner-guid"
            val createdEntity = TestEntity(generatedGuid, "test-name", ownerGuid = ownerGuid)
            val expectedResource = TestResource(generatedGuid, "test-name")

            every { guidGenerator() } returns generatedGuid
            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.persist(any<TestEntity>()) } just Runs
            every { repository.find("guid = ?1 and ownerGuid = ?2", generatedGuid, ownerGuid) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(createdEntity)
            coEvery { toResource(createdEntity) } returns expectedResource

            val result = crudService.create(inputEntity)

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).isEqualTo(expectedResource)

            verify { guidGenerator() }
            coVerify { ownerGuidExtractor() }
            verify { repository.persist(any<TestEntity>()) }
        }
    }

    @Test
    fun `create should handle repository exception`() {
        runBlocking {
            val inputEntity = TestEntity("", "test-name")
            val exception = RuntimeException("Database error")

            every { guidGenerator() } returns "guid"
            coEvery { ownerGuidExtractor() } returns null
            every { repository.persist(any<TestEntity>()) } throws exception

            val result = crudService.create(inputEntity)

            assertThat(result).isInstanceOf(CrudOutcome.Error::class.java)
            val error = result as CrudOutcome.Error
            assertThat(error.message).isEqualTo("Failed to create TestEntity: Database error")
            assertThat(error.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `findByField should return resource when entity exists with owner`() {
        runBlocking {
            val field = "name"
            val value = "test-name"
            val ownerGuid = "owner-guid"
            val entity = TestEntity("guid", "test-name", ownerGuid = ownerGuid)
            val expectedResource = TestResource("guid", "test-name")

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("$field = ?1 and ownerGuid = ?2", value, ownerGuid) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(entity)
            coEvery { toResource(entity) } returns expectedResource

            val result = crudService.findByField(field, value)

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).isEqualTo(expectedResource)
        }
    }

    @Test
    fun `findByField should return resource when entity exists without owner`() {
        runBlocking {
            val field = "name"
            val value = "test-name"
            val entity = TestEntity("guid", "test-name")
            val expectedResource = TestResource("guid", "test-name")

            coEvery { ownerGuidExtractor() } returns null
            every { repository.find("$field = ?1", value) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(entity)
            coEvery { toResource(entity) } returns expectedResource

            val result = crudService.findByField(field, value)

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).isEqualTo(expectedResource)
        }
    }

    @Test
    fun `findByField should apply filter correctly`() {
        runBlocking {
            val field = "name"
            val value = "test-name"
            val entity1 = TestEntity("guid1", "test-name", active = true)
            val entity2 = TestEntity("guid2", "test-name", active = false)
            val expectedResource = TestResource("guid1", "test-name")

            coEvery { ownerGuidExtractor() } returns null
            every { repository.find("$field = ?1", value) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(entity1, entity2)
            coEvery { toResource(entity1) } returns expectedResource

            val result = crudService.findByField(field, value) { it.active }

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).isEqualTo(expectedResource)
        }
    }

    @Test
    fun `findByField should return NotFound when no entity matches`() {
        runBlocking {
            val field = "name"
            val value = "non-existent"

            coEvery { ownerGuidExtractor() } returns null
            every { repository.find("$field = ?1", value) } returns panacheQuery
            every { panacheQuery.list() } returns emptyList()

            val result = crudService.findByField(field, value)

            assertThat(result).isInstanceOf(CrudOutcome.NotFound::class.java)
            val notFound = result as CrudOutcome.NotFound
            assertThat(notFound.id).isEqualTo(value)
        }
    }

    @Test
    fun `findByField should return NotFound when no entity matches filter`() {
        runBlocking {
            val field = "name"
            val value = "test-name"
            val entity = TestEntity("guid", "test-name", active = false)

            coEvery { ownerGuidExtractor() } returns null
            every { repository.find("$field = ?1", value) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(entity)

            val result = crudService.findByField(field, value) { it.active }

            assertThat(result).isInstanceOf(CrudOutcome.NotFound::class.java)
            val notFound = result as CrudOutcome.NotFound
            assertThat(notFound.id).isEqualTo(value)
        }
    }

    @Test
    fun `findByField should handle repository exception`() {
        runBlocking {
            val field = "name"
            val value = "test-name"
            val ownerGuid = "owner-guid"
            val exception = RuntimeException("Database error")

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("$field = ?1 and ownerGuid = ?2", value, ownerGuid) } throws exception

            val result = crudService.findByField(field, value)

            assertThat(result).isInstanceOf(CrudOutcome.Error::class.java)
            val error = result as CrudOutcome.Error
            assertThat(error.message).isEqualTo("Failed to get TestEntity with ownerGuid $ownerGuid and $value: Database error")
            assertThat(error.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `getAll should return all entities with owner`() {
        runBlocking {
            val ownerGuid = "owner-guid"
            val entities = listOf(
                TestEntity("guid1", "name1", ownerGuid = ownerGuid),
                TestEntity("guid2", "name2", ownerGuid = ownerGuid)
            )
            val expectedResources = listOf(
                TestResource("guid1", "name1"),
                TestResource("guid2", "name2")
            )

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("ownerGuid = ?1", ownerGuid) } returns panacheQuery
            every { panacheQuery.list() } returns entities
            coEvery { toResource(entities[0]) } returns expectedResources[0]
            coEvery { toResource(entities[1]) } returns expectedResources[1]

            val result = crudService.getAll()

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).containsExactlyElementsOf(expectedResources)
        }
    }

    @Test
    fun `getAll should return all entities without owner`() {
        runBlocking {
            val entities = listOf(
                TestEntity("guid1", "name1"),
                TestEntity("guid2", "name2")
            )
            val expectedResources = listOf(
                TestResource("guid1", "name1"),
                TestResource("guid2", "name2")
            )

            coEvery { ownerGuidExtractor() } returns null
            every { repository.findAll() } returns panacheQuery
            every { panacheQuery.list() } returns entities
            coEvery { toResource(entities[0]) } returns expectedResources[0]
            coEvery { toResource(entities[1]) } returns expectedResources[1]

            val result = crudService.getAll()

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).containsExactlyElementsOf(expectedResources)
        }
    }

    @Test
    fun `getAll should apply filter correctly`() {
        runBlocking {
            val entities = listOf(
                TestEntity("guid1", "name1", active = true),
                TestEntity("guid2", "name2", active = false),
                TestEntity("guid3", "name3", active = true)
            )
            val expectedResources = listOf(
                TestResource("guid1", "name1"),
                TestResource("guid3", "name3")
            )

            coEvery { ownerGuidExtractor() } returns null
            every { repository.findAll() } returns panacheQuery
            every { panacheQuery.list() } returns entities
            coEvery { toResource(entities[0]) } returns expectedResources[0]
            coEvery { toResource(entities[2]) } returns expectedResources[1]

            val result = crudService.getAll { it.active }

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).containsExactlyElementsOf(expectedResources)
        }
    }

    @Test
    fun `getAll should handle repository exception`() {
        runBlocking {
            val ownerGuid = "owner-guid"
            val exception = RuntimeException("Database error")

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("ownerGuid = ?1", ownerGuid) } throws exception

            val result = crudService.getAll()

            assertThat(result).isInstanceOf(CrudOutcome.Error::class.java)
            val error = result as CrudOutcome.Error
            assertThat(error.message).isEqualTo("Failed to get TestEntity with ownerGuid $ownerGuid: Database error")
            assertThat(error.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `updateByField should successfully update entity`() {
        runBlocking {
            val field = "guid"
            val value = "test-guid"
            val ownerGuid = "owner-guid"
            val existingEntity = TestEntity("test-guid", "old-name", ownerGuid = ownerGuid)
            val updatedEntity = TestEntity("test-guid", "new-name", ownerGuid = ownerGuid)
            val expectedResource = TestResource("test-guid", "new-name")
            val updateFn: suspend (TestEntity) -> TestEntity = { it.copy(name = "new-name") }

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("$field = ?1 and ownerGuid = ?2", value, ownerGuid) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(existingEntity)
            every { repository.update(any<TestEntity>()) } just Runs
            every { repository.find("guid = ?1 and ownerGuid = ?2", value, ownerGuid) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(updatedEntity)
            coEvery { toResource(updatedEntity) } returns expectedResource

            val result = crudService.updateByField(field, value, updatefn = updateFn)

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).isEqualTo(expectedResource)

            verify { repository.update(any<TestEntity>()) }
        }
    }

    @Test
    fun `updateByField should return NotFound when entity does not exist`() {
        runBlocking {
            val field = "guid"
            val value = "non-existent-guid"
            val updateFn: suspend (TestEntity) -> TestEntity = { it }

            coEvery { ownerGuidExtractor() } returns null
            every { repository.find("$field = ?1", value) } returns panacheQuery
            every { panacheQuery.list() } returns emptyList()

            val result = crudService.updateByField(field, value, updatefn = updateFn)

            assertThat(result).isInstanceOf(CrudOutcome.NotFound::class.java)
            val notFound = result as CrudOutcome.NotFound
            assertThat(notFound.id).isEqualTo(value)
        }
    }

    @Test
    fun `updateByField should handle repository exception`() {
        runBlocking {
            val field = "guid"
            val value = "test-guid"
            val ownerGuid = "owner-guid"
            val exception = RuntimeException("Database error")
            val updateFn: suspend (TestEntity) -> TestEntity = { it }

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("$field = ?1 and ownerGuid = ?2", value, ownerGuid) } throws exception

            val result = crudService.updateByField(field, value, updatefn = updateFn)

            assertThat(result).isInstanceOf(CrudOutcome.Error::class.java)
            val error = result as CrudOutcome.Error
            assertThat(error.message).isEqualTo("Failed to update TestEntity with ownerGuid $ownerGuid and $field $value: Database error")
            assertThat(error.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `deleteByGuid should successfully delete entity`() {
        runBlocking {
            val field = "guid"
            val value = "test-guid"
            val ownerGuid = "owner-guid"
            val existingEntity = TestEntity("test-guid", "test-name", ownerGuid = ownerGuid)

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("$field = ?1 and ownerGuid = ?2", value, ownerGuid) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(existingEntity)
            every { repository.delete(existingEntity) }

            val result = crudService.deleteByGuid(field, value)

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).isEqualTo(Unit)

            verify { repository.delete(existingEntity) }
        }
    }

    @Test
    fun `deleteByGuid should return NotFound when entity does not exist`() {
        runBlocking {
            val field = "guid"
            val value = "non-existent-guid"

            coEvery { ownerGuidExtractor() } returns null
            every { repository.find("$field = ?1", value) } returns panacheQuery
            every { panacheQuery.list() } returns emptyList()

            val result = crudService.deleteByGuid(field, value)

            assertThat(result).isInstanceOf(CrudOutcome.NotFound::class.java)
            val notFound = result as CrudOutcome.NotFound
            assertThat(notFound.id).isEqualTo(value)
        }
    }

    @Test
    fun `deleteByGuid should handle repository exception`() {
        runBlocking {
            val field = "guid"
            val value = "test-guid"
            val ownerGuid = "owner-guid"
            val exception = RuntimeException("Database error")

            coEvery { ownerGuidExtractor() } returns ownerGuid
            every { repository.find("$field = ?1 and ownerGuid = ?2", value, ownerGuid) } throws exception

            val result = crudService.deleteByGuid(field, value)

            assertThat(result).isInstanceOf(CrudOutcome.Error::class.java)
            val error = result as CrudOutcome.Error
            assertThat(error.message).isEqualTo("Failed to delete TestEntity with ownerGuid $ownerGuid and $field $value: Database error")
            assertThat(error.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `should handle different field names for operations`() {
        runBlocking {
            val customField = "customField"
            val value = "custom-value"
            val entity = TestEntity("guid", "name", customField = value)
            val expectedResource = TestResource("guid", "name")

            coEvery { ownerGuidExtractor() } returns null
            every { repository.find("$customField = ?1", value) } returns panacheQuery
            every { panacheQuery.list() } returns listOf(entity)
            coEvery { toResource(entity) } returns expectedResource

            val result = crudService.findByField(customField, value)

            assertThat(result).isInstanceOf(CrudOutcome.Success::class.java)
            val success = result as CrudOutcome.Success
            assertThat(success.data).isEqualTo(expectedResource)
        }
    }

    @Test
    fun `should verify FIELD_GUID constant value`() {
        assertThat(CrudService.FIELD_GUID).isEqualTo("guid")
    }

    // Helper classes for testing
    data class TestEntity(
        override val guid: String = "",
        val name: String = "",
        override val ownerGuid: String? = null,
        val active: Boolean = true,
        val customField: String = "",
        override val id: ObjectId? = mockk(),
        override val createdOn: Instant? = Instant.now(),
        override val createdBy: String? = "ABC",
        override val modifiedOn: Instant? = Instant.now(),
        override val modifiedBy: String? = "DEF",
        override val version: Long? = 0
    ) : Entity<TestEntity> {
        override fun withCreateInfo(guid: String?, userGuid: String?): TestEntity {
            return copy(guid = guid!!, ownerGuid = userGuid)
        }

        override fun withUpdateInfo(userGuid: String?): TestEntity {
            return copy(ownerGuid = userGuid)
        }
    }

    data class TestResource(
        val guid: String,
        val name: String
    )

}