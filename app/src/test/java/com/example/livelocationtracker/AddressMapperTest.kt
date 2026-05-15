package com.example.livelocationtracker

import com.example.livelocationtracker.data.mapper.toDomain
import com.example.livelocationtracker.data.remote.dto.AddressDto
import com.example.livelocationtracker.data.remote.dto.NominatimResponseDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressMapperTest {
    @Test fun `prefers city over town village suburb`() {
        val dto = NominatimResponseDto(
            displayName = "1 Main St, Springfield, USA",
            address = AddressDto(
                road = "Main St",
                neighbourhood = "North End",
                suburb = "Westside",
                city = "Springfield",
                town = "Shelbyville",
                village = null,
                state = "IL",
                country = "USA",
                postcode = "62701",
            ),
        )
        val out = dto.toDomain()
        assertEquals("Springfield", out.city)
        assertEquals("Main St", out.road)
        assertEquals("USA", out.country)
    }

    @Test fun `falls back through locality candidates`() {
        val dto = NominatimResponseDto(
            displayName = null,
            address = AddressDto(
                road = null, neighbourhood = "N", suburb = "S", city = null,
                town = null, village = null, state = null, country = null, postcode = null,
            ),
        )
        assertEquals("S", dto.toDomain().city)
    }
}