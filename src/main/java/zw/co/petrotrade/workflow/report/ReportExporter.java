package zw.co.petrotrade.workflow.report;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static zw.co.petrotrade.workflow.report.TableExport.BRAND;
import static zw.co.petrotrade.workflow.report.TableExport.DAY;
import static zw.co.petrotrade.workflow.report.TableExport.STAMP;
import static zw.co.petrotrade.workflow.report.TableExport.cell;
import static zw.co.petrotrade.workflow.report.TableExport.format;

// Turns a TripReport into the Excel and PDF files people download
@Component
public class ReportExporter {

    private static final List<TableExport.Col<TripReport.Row>> COLUMNS = List.of(
            new TableExport.Col<>("Request #", 10, TripReport.Row::requestId),
            new TableExport.Col<>("From", 12, TripReport.Row::from),
            new TableExport.Col<>("To", 12, TripReport.Row::to),
            new TableExport.Col<>("Driver", 22, TripReport.Row::driver),
            new TableExport.Col<>("Department", 16, TripReport.Row::department),
            new TableExport.Col<>("Vehicle", 13, TripReport.Row::vehicle),
            new TableExport.Col<>("Make / model", 18, TripReport.Row::vehicleModel),
            new TableExport.Col<>("Route", 28, TripReport.Row::route),
            new TableExport.Col<>("Planned distance (km)", 13, TripReport.Row::distanceKm),
            new TableExport.Col<>("Distance travelled (km)", 13, TripReport.Row::distanceTravelledKm),
            new TableExport.Col<>("Fuel type", 12, TripReport.Row::fuelType),
            new TableExport.Col<>("Fuel issued (L)", 12, TripReport.Row::litresIssued),
            new TableExport.Col<>("Fuel used (L)", 12, TripReport.Row::fuelUsedLitres),
            new TableExport.Col<>("HOD approval", 20, TripReport.Row::hodApprover),
            new TableExport.Col<>("HR & Admin approval", 20, TripReport.Row::hrApprover),
            new TableExport.Col<>("Fuel approval", 20, TripReport.Row::fuelApprover),
            new TableExport.Col<>("Allocated by", 20, TripReport.Row::allocatedBy),
            new TableExport.Col<>("Returned", 17, TripReport.Row::returnedAt),
            new TableExport.Col<>("Status", 20, TripReport.Row::status));

    public String fileName(TripReport report, String extension) {
        return "trip-report_" + report.from() + "_to_" + report.to() + "." + extension;
    }

    public byte[] excel(TripReport report, String generatedBy) {
        TripReport.Totals t = report.totals();
        return TableExport.excel("Trips", "Vehicle trips report", meta(report, generatedBy), COLUMNS, report.rows(),
                t.trips() + " trips",
                Map.of("Planned distance (km)", t.distanceKm(),
                        "Distance travelled (km)", t.distanceTravelledKm(),
                        "Fuel issued (L)", t.litresIssued(),
                        "Fuel used (L)", t.fuelUsedLitres()));
    }

    private static List<String> meta(TripReport report, String generatedBy) {
        return List.of(
                "Period: " + DAY.format(report.from()) + " – " + DAY.format(report.to()),
                "Filters: " + report.filterSummary(),
                "Generated " + STAMP.format(LocalDateTime.now()) + " by " + generatedBy);
    }

    public byte[] pdf(TripReport report, String generatedBy) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 28, 32);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new TableExport.PageNumbers());
        doc.open();

        TripReport.Totals t = report.totals();
        TableExport.header(doc, "Vehicle trips report", meta(report, generatedBy),
                t.trips() + " trips   ·   " + format(t.distanceKm()) + " km   ·   "
                        + format(t.litresIssued()) + " L issued   ·   " + format(t.fuelUsedLitres()) + " L used   ·   "
                        + t.vehicles() + " vehicles   ·   " + t.drivers() + " drivers");

        // fewer, combined columns than Excel so a landscape page stays readable
        String[] heads = {"#", "Dates", "Driver", "Vehicle", "Route", "km", "Fuel", "Issued L", "Used L", "Approved by", "Allocated by", "Returned", "Status"};
        float[] widths = {3, 8, 10, 9, 12, 5, 6, 5, 5, 14, 9, 8, 8};
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
        Font muted = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, Color.GRAY);
        for (String h : heads) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(BRAND);
            cell.setPadding(4);
            cell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        boolean shade = false;
        for (TripReport.Row row : report.rows()) {
            Color bg = shade ? new Color(0xf3, 0xf7, 0xf4) : Color.WHITE;
            shade = !shade;
            String dates = DAY.format(row.from()) + (row.to().equals(row.from()) ? "" : "\nto " + DAY.format(row.to()));
            table.addCell(cell(String.valueOf(row.requestId()), body, null, bg, false));
            table.addCell(cell(dates, body, null, bg, false));
            table.addCell(cell(row.driver(), body, row.department() == null ? null : new Phrase(row.department(), muted), bg, false));
            table.addCell(cell(row.vehicle(), body, row.vehicleModel() == null ? null : new Phrase(row.vehicleModel(), muted), bg, false));
            // the built-in PDF fonts have no arrow glyph; an en dash prints everywhere
            table.addCell(cell(row.route().replace(" → ", " – "), body, null, bg, false));
            // planned, with the odometer distance underneath when it was recorded
            table.addCell(cell(format(row.distanceKm()), body,
                    row.distanceTravelledKm() == null ? null : new Phrase("actual " + format(row.distanceTravelledKm()), muted), bg, true));
            table.addCell(cell(row.fuelType(), body, null, bg, false));
            table.addCell(cell(format(row.litresIssued()), body, null, bg, true));
            table.addCell(cell(format(row.fuelUsedLitres()), body, null, bg, true));
            table.addCell(cell(approvers(row), body, null, bg, false));
            table.addCell(cell(row.allocatedBy(), body, null, bg, false));
            table.addCell(cell(row.returnedAt() == null ? "—" : STAMP.format(row.returnedAt()), body, null, bg, false));
            table.addCell(cell(row.status(), body, null, bg, false));
        }

        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f);
        PdfPCell label = cell("Total: " + t.trips() + " trips", bold, null, Color.WHITE, false);
        label.setColspan(5);
        table.addCell(label);
        table.addCell(cell(format(t.distanceKm()), bold, null, Color.WHITE, true));
        table.addCell(cell("", bold, null, Color.WHITE, false));
        table.addCell(cell(format(t.litresIssued()), bold, null, Color.WHITE, true));
        table.addCell(cell(format(t.fuelUsedLitres()), bold, null, Color.WHITE, true));
        PdfPCell rest = cell("", bold, null, Color.WHITE, false);
        rest.setColspan(4);
        table.addCell(rest);

        if (report.rows().isEmpty()) {
            doc.add(new Paragraph("No trips match this period and these filters.", body));
        } else {
            doc.add(table);
        }
        doc.close();
        return out.toByteArray();
    }

    private static String approvers(TripReport.Row row) {
        StringBuilder text = new StringBuilder();
        if (row.hodApprover() != null) text.append("HOD: ").append(row.hodApprover());
        if (row.hrApprover() != null) text.append(text.isEmpty() ? "" : "\n").append("HR: ").append(row.hrApprover());
        if (row.fuelApprover() != null) text.append(text.isEmpty() ? "" : "\n").append("Fuel: ").append(row.fuelApprover());
        return text.isEmpty() ? null : text.toString();
    }
}
