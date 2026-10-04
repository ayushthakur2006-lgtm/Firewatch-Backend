package com.firewatch.firewatch_backend;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Service.FirmsCsvParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FirmsCsvParserTest {

    private final FirmsCsvParser parser = new FirmsCsvParser();

    @Test
    void testParseValidFirmsCsv() {
        String csvData = """
                latitude,longitude,bright_ti4,scan,track,acq_date,acq_time,satellite,instrument,confidence,version,bright_ti5,frp,daynight
                21.1702,72.8311,345.6,0.39,0.36,2026-08-27,803,N,VIIRS,nominal,2.0NRT,298.2,42.5,D
                """;

        List<Hotspot> hotspots = parser.parse(csvData);

        assertEquals(1, hotspots.size());
        Hotspot h = hotspots.get(0);
        assertEquals(21.1702, h.getLatitude());
        assertEquals(72.8311, h.getLongitude());
        assertEquals(345.6, h.getBrightTi4());
        assertEquals(298.2, h.getBrightTi5());
        assertEquals(42.5, h.getFrp());
        assertEquals(LocalDate.of(2026, 8, 27), h.getAcqDate());
        assertEquals(LocalTime.of(8, 3), h.getAcqTime());
        assertEquals("nominal", h.getConfidence());
        assertEquals("D", h.getDaynight());
    }
}
