package com.example.timetracking.web;

import com.example.timetracking.service.WeeklyHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class AdminReportsController {

    private final WeeklyHoursService weeklyHoursService;
    private final Clock clock;

    @GetMapping("/admin/reports/weekly")
    public String weeklyReport(Model model,
                               @RequestParam(value = "weekStart", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                               LocalDate weekStart) {
        ZoneId zone = ZoneId.of("UTC");

        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (weekStart == null) {
            weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        }

        Instant start = weekStart.atStartOfDay(zone).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(zone).toInstant();

        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekStart.plusDays(6));
        model.addAttribute("rows", weeklyHoursService.computeWeeklyHours(start, end));
        return "admin/weekly-report";
    }

    @GetMapping("/admin/reports/range")
    public String rangeReport(Model model,
                              @RequestParam(value = "fromDate", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                              LocalDate fromDate,
                              @RequestParam(value = "toDate", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                              LocalDate toDate) {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(clock.withZone(zone));

        if (toDate == null) {
            toDate = today;
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(6);
        }
        if (toDate.isBefore(fromDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "admin/range-report";
    }

    @GetMapping("/admin/reports/weekly/data")
    @ResponseBody
    public WeeklyHoursService.WeeklyReport weeklyReportData(
        @RequestParam(value = "weekStart", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate weekStart
    ) {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (weekStart == null) {
            weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        }

        Instant start = weekStart.atStartOfDay(zone).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(zone).toInstant();
        return weeklyHoursService.computeWeeklyReport(start, end, Instant.now(clock));
    }

    @GetMapping("/admin/reports/range/data")
    @ResponseBody
    public WeeklyHoursService.WeeklyReport rangeReportData(
        @RequestParam(value = "fromDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,
        @RequestParam(value = "toDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
    ) {
        ZoneId zone = ZoneId.of("UTC");
        if (toDate.isBefore(fromDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        Instant start = fromDate.atStartOfDay(zone).toInstant();
        Instant endExclusive = toDate.plusDays(1).atStartOfDay(zone).toInstant();
        return weeklyHoursService.computeWeeklyReport(start, endExclusive, Instant.now(clock));
    }

    @GetMapping("/admin/reports/weekly/pdf")
    public ResponseEntity<byte[]> weeklyReportPdf(
        @RequestParam(value = "weekStart", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate weekStart
    ) throws DocumentException {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (weekStart == null) {
            weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        }

        Instant start = weekStart.atStartOfDay(zone).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(zone).toInstant();

        WeeklyHoursService.WeeklyReport report = weeklyHoursService.computeWeeklyReport(start, end, Instant.now(clock));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        doc.add(new Paragraph("Weekly Hours Report", titleFont));
        doc.add(new Paragraph("Week: " + weekStart + " to " + weekStart.plusDays(6), bodyFont));
        doc.add(new Paragraph(" "));

        doc.add(new Paragraph("Totals by Employee", hFont));
        PdfPTable totalsTable = new PdfPTable(3);
        totalsTable.setWidthPercentage(100);
        totalsTable.setSpacingBefore(6);
        totalsTable.setSpacingAfter(10);
        totalsTable.setWidths(new float[]{3.5f, 2.2f, 2.2f});

        totalsTable.addCell(headerCell("Employee"));
        totalsTable.addCell(headerCell("Completed (H:MM)"));
        totalsTable.addCell(headerCell("Live incl. open (H:MM)"));

        if (report.totals() != null) {
            for (var r : report.totals()) {
                totalsTable.addCell(bodyCell(r.employeeName(), bodyFont));
                totalsTable.addCell(bodyCell(r.completedTotalFormatted(), bodyFont));
                totalsTable.addCell(bodyCell(r.liveTotalFormatted(), bodyFont));
            }
        }
        doc.add(totalsTable);

        doc.add(new Paragraph("Shift Details", hFont));
        PdfPTable shiftTable = new PdfPTable(6);
        shiftTable.setWidthPercentage(100);
        shiftTable.setSpacingBefore(6);
        shiftTable.setWidths(new float[]{3.2f, 1.4f, 2.2f, 2.2f, 1.5f, 1.5f});

        shiftTable.addCell(headerCell("Employee"));
        shiftTable.addCell(headerCell("Day"));
        shiftTable.addCell(headerCell("Clock in"));
        shiftTable.addCell(headerCell("Clock out"));
        shiftTable.addCell(headerCell("Hours"));
        shiftTable.addCell(headerCell("Status"));

        if (report.shifts() != null) {
            for (var s : report.shifts()) {
                shiftTable.addCell(bodyCell(s.employeeName(), bodyFont));
                shiftTable.addCell(bodyCell(s.day(), bodyFont));
                shiftTable.addCell(bodyCell(formatEpochMillis(s.clockInEpochMillis()), bodyFont));
                shiftTable.addCell(bodyCell(formatEpochMillis(s.clockOutEpochMillis()), bodyFont));
                shiftTable.addCell(bodyCell(formatWorkedSecondsAsHoursMinutes(s.workedSeconds()), bodyFont));
                shiftTable.addCell(bodyCell(s.completed() ? "COMPLETED" : "OPEN", bodyFont));
            }
        }
        doc.add(shiftTable);

        doc.close();

        String filename = "weekly-report-" + weekStart + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
    }

    @GetMapping("/admin/reports/range/pdf")
    public ResponseEntity<byte[]> rangeReportPdf(
        @RequestParam(value = "fromDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,
        @RequestParam(value = "toDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
    ) throws DocumentException {
        ZoneId zone = ZoneId.of("UTC");
        if (toDate.isBefore(fromDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        Instant start = fromDate.atStartOfDay(zone).toInstant();
        Instant endExclusive = toDate.plusDays(1).atStartOfDay(zone).toInstant();
        WeeklyHoursService.WeeklyReport report = weeklyHoursService.computeWeeklyReport(start, endExclusive, Instant.now(clock));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        doc.add(new Paragraph("Date Range Report", titleFont));
        doc.add(new Paragraph("Range: " + fromDate + " to " + toDate + " (UTC)", bodyFont));
        doc.add(new Paragraph(" "));

        doc.add(new Paragraph("Totals by Employee", hFont));
        PdfPTable totalsTable = new PdfPTable(3);
        totalsTable.setWidthPercentage(100);
        totalsTable.setSpacingBefore(6);
        totalsTable.setSpacingAfter(10);
        totalsTable.setWidths(new float[]{3.5f, 2.2f, 2.2f});

        totalsTable.addCell(headerCell("Employee"));
        totalsTable.addCell(headerCell("Completed (H:MM)"));
        totalsTable.addCell(headerCell("Live incl. open (H:MM)"));

        if (report.totals() != null) {
            for (var r : report.totals()) {
                totalsTable.addCell(bodyCell(r.employeeName(), bodyFont));
                totalsTable.addCell(bodyCell(r.completedTotalFormatted(), bodyFont));
                totalsTable.addCell(bodyCell(r.liveTotalFormatted(), bodyFont));
            }
        }
        doc.add(totalsTable);

        doc.add(new Paragraph("Shift Details", hFont));
        PdfPTable shiftTable = new PdfPTable(6);
        shiftTable.setWidthPercentage(100);
        shiftTable.setSpacingBefore(6);
        shiftTable.setWidths(new float[]{3.2f, 1.4f, 2.2f, 2.2f, 1.5f, 1.5f});

        shiftTable.addCell(headerCell("Employee"));
        shiftTable.addCell(headerCell("Day"));
        shiftTable.addCell(headerCell("Clock in"));
        shiftTable.addCell(headerCell("Clock out"));
        shiftTable.addCell(headerCell("Hours"));
        shiftTable.addCell(headerCell("Status"));

        if (report.shifts() != null) {
            for (var s : report.shifts()) {
                shiftTable.addCell(bodyCell(s.employeeName(), bodyFont));
                shiftTable.addCell(bodyCell(s.day(), bodyFont));
                shiftTable.addCell(bodyCell(formatEpochMillis(s.clockInEpochMillis()), bodyFont));
                shiftTable.addCell(bodyCell(formatEpochMillis(s.clockOutEpochMillis()), bodyFont));
                shiftTable.addCell(bodyCell(formatWorkedSecondsAsHoursMinutes(s.workedSeconds()), bodyFont));
                shiftTable.addCell(bodyCell(s.completed() ? "COMPLETED" : "OPEN", bodyFont));
            }
        }
        doc.add(shiftTable);

        doc.close();

        String filename = "range-report-" + fromDate + "-to-" + toDate + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
    }

    @GetMapping("/admin/reports/weekly/csv")
    public ResponseEntity<byte[]> weeklyReportCsv(
        @RequestParam(value = "weekStart", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate weekStart
    ) {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (weekStart == null) {
            weekStart = today.with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        }

        Instant start = weekStart.atStartOfDay(zone).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(zone).toInstant();
        WeeklyHoursService.WeeklyReport report = weeklyHoursService.computeWeeklyReport(start, end, Instant.now(clock));

        StringBuilder sb = new StringBuilder();
        sb.append("Weekly Hours Report\n");
        sb.append("Week,").append(csv(weekStart.toString())).append(",to,").append(csv(weekStart.plusDays(6).toString())).append("\n\n");

        sb.append("Totals by Employee\n");
        sb.append("Employee,Completed (H:MM),Live incl. open (H:MM)\n");
        if (report.totals() != null) {
            for (var r : report.totals()) {
                sb.append(csv(r.employeeName())).append(',')
                    .append(csv(r.completedTotalFormatted())).append(',')
                    .append(csv(r.liveTotalFormatted())).append("\n");
            }
        }

        sb.append("\nShift Details\n");
        sb.append("Employee,Day,Clock in (UTC),Clock out (UTC),Hours (H:MM),Status\n");
        if (report.shifts() != null) {
            for (var s : report.shifts()) {
                sb.append(csv(s.employeeName())).append(',')
                    .append(csv(s.day())).append(',')
                    .append(csv(formatEpochMillis(s.clockInEpochMillis()))).append(',')
                    .append(csv(formatEpochMillis(s.clockOutEpochMillis()))).append(',')
                    .append(csv(formatWorkedSecondsAsHoursMinutes(s.workedSeconds()))).append(',')
                    .append(csv(s.completed() ? "COMPLETED" : "OPEN"))
                    .append("\n");
            }
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "weekly-report-" + weekStart + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/admin/reports/range/csv")
    public ResponseEntity<byte[]> rangeReportCsv(
        @RequestParam(value = "fromDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,
        @RequestParam(value = "toDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
    ) {
        ZoneId zone = ZoneId.of("UTC");
        if (toDate.isBefore(fromDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        Instant start = fromDate.atStartOfDay(zone).toInstant();
        Instant endExclusive = toDate.plusDays(1).atStartOfDay(zone).toInstant();
        WeeklyHoursService.WeeklyReport report = weeklyHoursService.computeWeeklyReport(start, endExclusive, Instant.now(clock));

        StringBuilder sb = new StringBuilder();
        sb.append("Date Range Report\n");
        sb.append("From,").append(csv(fromDate.toString())).append(",To,").append(csv(toDate.toString())).append("\n\n");

        sb.append("Totals by Employee\n");
        sb.append("Employee,Completed (H:MM),Live incl. open (H:MM)\n");
        if (report.totals() != null) {
            for (var r : report.totals()) {
                sb.append(csv(r.employeeName())).append(',')
                    .append(csv(r.completedTotalFormatted())).append(',')
                    .append(csv(r.liveTotalFormatted())).append("\n");
            }
        }

        sb.append("\nShift Details\n");
        sb.append("Employee,Day,Clock in (UTC),Clock out (UTC),Hours (H:MM),Status\n");
        if (report.shifts() != null) {
            for (var s : report.shifts()) {
                sb.append(csv(s.employeeName())).append(',')
                    .append(csv(s.day())).append(',')
                    .append(csv(formatEpochMillis(s.clockInEpochMillis()))).append(',')
                    .append(csv(formatEpochMillis(s.clockOutEpochMillis()))).append(',')
                    .append(csv(formatWorkedSecondsAsHoursMinutes(s.workedSeconds()))).append(',')
                    .append(csv(s.completed() ? "COMPLETED" : "OPEN"))
                    .append("\n");
            }
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "range-report-" + fromDate + "-to-" + toDate + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private static PdfPCell headerCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(6f);
        return cell;
    }

    private static PdfPCell bodyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(6f);
        return cell;
    }

    private static String formatEpochMillis(Long epochMillis) {
        if (epochMillis == null) {
            return "-";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));
        return fmt.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String formatWorkedSecondsAsHoursMinutes(long seconds) {
        long s = Math.max(0, seconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        return String.format("%d:%02d", h, m);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        boolean needsQuotes = v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"");
        v = v.replace("\"", "\"\"");
        return needsQuotes ? ("\"" + v + "\"") : v;
    }
}
