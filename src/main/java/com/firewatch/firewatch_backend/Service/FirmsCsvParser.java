package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FirmsCsvParser {

    public List<Hotspot> parse(String csv) {

        List<Hotspot> hotspots = new ArrayList<>();

        try {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .get();

            Iterable<CSVRecord> records =
                    format.parse(new java.io.StringReader(csv));

            for (CSVRecord record : records) {

                Hotspot hotspot = new Hotspot();

                hotspot.setLatitude(parseDouble(record.get("latitude")));
                hotspot.setLongitude(parseDouble(record.get("longitude")));

                hotspot.setBrightTi4(parseDouble(record.get("bright_ti4")));
                hotspot.setBrightTi5(parseDouble(record.get("bright_ti5")));

                hotspot.setScan(parseDouble(record.get("scan")));
                hotspot.setTrack(parseDouble(record.get("track")));

                hotspot.setAcqDate(
                        LocalDate.parse(record.get("acq_date"))
                );

                hotspot.setAcqTime(
                        parseTime(record.get("acq_time"))
                );

                hotspot.setSatellite(record.get("satellite"));
                hotspot.setInstrument(record.get("instrument"));
                hotspot.setConfidence(record.get("confidence"));
                hotspot.setVersion(record.get("version"));

                hotspot.setFrp(parseDouble(record.get("frp")));
                hotspot.setDaynight(record.get("daynight"));
                hotspot.setSource("VIIRS_SNPP_NRT");
                hotspot.setClassification(null);
                hotspot.setMlConfidence(null);
                hotspot.setBrightness(
                        parseDouble(record.get("bright_ti4"))
                );

                hotspots.add(hotspot);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse NASA FIRMS CSV", e);
        }

        return hotspots;
    }

    private Double parseDouble(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value);
    }

    private LocalTime parseTime(String value) {
    try {
        String time = value.trim();

        if (time.length() < 1 || time.length() > 4) {
            throw new IllegalArgumentException(
                    "Invalid FIRMS acquisition time: " + value
            );
        }

        // Pad to HHMM
        time = String.format("%4s", time).replace(' ', '0');

        int hour = Integer.parseInt(time.substring(0, 2));
        int minute = Integer.parseInt(time.substring(2, 4));

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException(
                    "Invalid FIRMS acquisition time: " + value
            );
        }

        return LocalTime.of(hour, minute);

    } catch (Exception e) {
        throw new IllegalArgumentException(
                "Invalid FIRMS acquisition time: " + value,
                e
        );
    }
}
}