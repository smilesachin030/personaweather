package com.example.personaweather.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoGeocodingClientTest {
    @Test
    fun searchRestrictsRequestToIndiaAndMapsLiveOnlyPlaceMetadata() = runBlocking {
        var requestedUrl = ""
        val client = OpenMeteoGeocodingClient { url ->
            requestedUrl = url
            """{"results":[{"id":12345,"name":"New Delhi","latitude":28.61,"longitude":77.21,"admin1":"Delhi","country_code":"IN","timezone":"Asia/Kolkata"}]}"""
        }

        val result = client.search(" New Delhi ").single()

        assertTrue(requestedUrl.startsWith("https://geocoding-api.open-meteo.com/v1/search?"))
        assertTrue(requestedUrl.contains("name=New+Delhi"))
        assertTrue(requestedUrl.contains("countryCode=IN"))
        assertTrue(requestedUrl.contains("language=en"))
        assertEquals("open-meteo:IN:12345", result.id)
        assertEquals("Delhi", result.adminArea)
        assertEquals("Asia/Kolkata", result.timeZone)
        assertFalse(result.isCoastal)
    }

    @Test
    fun searchRequestsNamesInSelectedLanguage() = runBlocking {
        var requestedUrl = ""
        val client = OpenMeteoGeocodingClient { url ->
            requestedUrl = url
            """{"results":[{"id":12345,"name":"दिल्ली","latitude":28.61,"longitude":77.21,"admin1":"दिल्ली","country_code":"IN","timezone":"Asia/Kolkata"}]}"""
        }

        val result = client.search("Delhi", "hi-IN").single()

        assertTrue(requestedUrl.contains("language=hi"))
        assertEquals("दिल्ली", result.name)
        assertEquals("दिल्ली", result.adminArea)
    }

    @Test
    fun searchFallsBackToEnglishForUnsupportedLanguage() = runBlocking {
        var requestedUrl = ""
        val client = OpenMeteoGeocodingClient { url ->
            requestedUrl = url
            "{}"
        }

        client.search("Delhi", "fr")

        assertTrue(requestedUrl.contains("language=en"))
    }

    @Test
    fun searchFiltersForeignAndOutOfBoundsResults() = runBlocking {
        val client = OpenMeteoGeocodingClient {
            """{"results":[{"id":1,"name":"Foreign","latitude":27.0,"longitude":85.0,"country_code":"NP"},{"id":2,"name":"Outside","latitude":40.0,"longitude":80.0,"country_code":"IN"},{"id":3,"name":"India","latitude":19.0,"longitude":72.0,"country_code":"IN"}]}"""
        }

        assertEquals(listOf("open-meteo:IN:3"), client.search("Mumbai").map { it.id })
    }

    @Test
    fun searchDoesNotRequestForShortQueries() = runBlocking {
        var requested = false
        val client = OpenMeteoGeocodingClient {
            requested = true
            "{}"
        }

        assertTrue(client.search(" ").isEmpty())
        assertFalse(requested)
    }

    @Test
    fun curatedIndianCityShortcutsContainFiftyUniqueCityNames() {
        assertEquals(50, IndianCityCatalog.cities.size)
        assertEquals(50, IndianCityCatalog.cities.map { it.name }.distinct().size)
        assertTrue(IndianCityCatalog.cities.any { it.name == "Srinagar" && it.region == "Jammu and Kashmir" })
    }
}