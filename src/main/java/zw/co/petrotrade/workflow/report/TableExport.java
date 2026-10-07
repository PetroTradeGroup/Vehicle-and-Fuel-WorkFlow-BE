package zw.co.petrotrade.workflow.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// Shared Excel / PDF building for the reports: one header row, typed cells, a totals row
public final class TableExport {

    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");
    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    static final Color BRAND = new Color(0x16, 0xa3, 0x4a); // company green, as in the app

    // width is in Excel characters
    public record Col<T>(String title, int width, Function<T, Object> value) {
    }

    private TableExport() {
    }

    // totals: column title -> value for the totals row; the first cell holds totalLabel
    public static <T> byte[] excel(String sheetName, String title, List<String> meta, List<Col<T>> cols,
                                   List<T> rows, String totalLabel, Map<String, Double> totals) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet(sheetName);

            XSSFFont titleFont = book.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = book.createCellStyle();
            titleStyle.setFont(titleFont);

            XSSFFont headFont = book.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            XSSFCellStyle head = book.createCellStyle();
            head.setFont(headFont);
            head.setFillForegroundColor(new XSSFColor(BRAND, null));
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setWrapText(true);

            CellStyle date = book.createCellStyle();
            date.setDataFormat(book.createDataFormat().getFormat("dd mmm yyyy"));
            CellStyle stamp = book.createCellStyle();
            stamp.setDataFormat(book.createDataFormat().getFormat("dd mmm yyyy hh:mm"));
            CellStyle number = book.createCellStyle();
            number.setDataFormat(book.createDataFormat().getFormat("#,##0.0"));

            XSSFFont boldFont = book.createFont();
            boldFont.setBold(true);
            CellStyle total = book.createCellStyle();
            total.setFont(boldFont);
            total.setBorderTop(BorderStyle.THIN);
            CellStyle totalNumber = book.createCellStyle();
            totalNumber.cloneStyleFrom(total);
            totalNumber.setDataFormat(number.getDataFormat());

            int r = 0;
            text(sheet.createRow(r++), 0, title, titleStyle);
            for (String line : meta) {
                text(sheet.createRow(r++), 0, line, null);
            }
            r++;

            int headerRow = r;
            Row header = sheet.createRow(r++);
            header.setHeightInPoints(30);
            for (int c = 0; c < cols.size(); c++) {
                text(header, c, cols.get(c).title(), head);
                sheet.setColumnWidth(c, cols.get(c).width() * 256);
            }

            for (T row : rows) {
                Row line = sheet.createRow(r++);
                for (int c = 0; c < cols.size(); c++) {
                    Object value = cols.get(c).value().apply(row);
                    Cell cell = line.createCell(c);
                    if (value == null) {
                        cell.setBlank();
                    } else if (value instanceof LocalDate d) {
                        cell.setCellValue(d);
                        cell.setCellStyle(date);
                    } else if (value instanceof LocalDateTime t) {
                        cell.setCellValue(t);
                        cell.setCellStyle(stamp);
                    } else if (value instanceof Long id) {
                        cell.setCellValue(id);
                    } else if (value instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                        cell.setCellStyle(number);
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            Row totalsRow = sheet.createRow(r);
            text(totalsRow, 0, totalLabel, total);
            for (int c = 1; c < cols.size(); c++) {
                Double value = totals.get(cols.get(c).title());
                Cell cell = totalsRow.createCell(c);
                if (value != null) {
                    cell.setCellValue(value);
                    cell.setCellStyle(totalNumber);
                } else {
                    cell.setCellStyle(total);
                }
            }

            sheet.createFreezePane(0, headerRow + 1);
            if (!rows.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(headerRow, headerRow + rows.size(), 0, cols.size() - 1));
            }
            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't build the Excel file. Please try again.", e);
        }
    }

    // A plain landscape table in the company style; widths are relative
    public static <T> byte[] pdf(String title, List<String> meta, String summary, List<Col<T>> cols, float[] widths,
                                 List<T> rows, String totalLabel, Map<String, Double> totals) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 28, 32);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new PageNumbers());
        doc.open();
        header(doc, title, meta, summary);

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f);
        for (Col<T> col : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(col.title(), headFont));
            cell.setBackgroundColor(BRAND);
            cell.setPadding(4);
            cell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }
        boolean shade = false;
        for (T row : rows) {
            Color bg = shade ? new Color(0xf3, 0xf7, 0xf4) : Color.WHITE;
            shade = !shade;
            for (Col<T> col : cols) {
                Object value = col.value().apply(row);
                table.addCell(cell(display(value), body, null, bg, value instanceof Number && !(value instanceof Long)));
            }
        }
        for (int c = 0; c < cols.size(); c++) {
            Double value = totals.get(cols.get(c).title());
            table.addCell(cell(c == 0 ? totalLabel : value == null ? "" : format(value), bold, null, Color.WHITE, value != null));
        }

        if (rows.isEmpty()) {
            doc.add(new Paragraph("Nothing matches this period.", body));
        } else {
            doc.add(table);
        }
        doc.close();
        return out.toByteArray();
    }

    static void header(Document doc, String title, List<String> meta, String summary) {
        doc.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15)));
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.DARK_GRAY);
        for (String line : meta) {
            doc.add(new Paragraph(line, metaFont));
        }
        Paragraph summaryLine = new Paragraph(summary, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, BRAND));
        summaryLine.setSpacingBefore(6);
        summaryLine.setSpacingAfter(8);
        doc.add(summaryLine);
    }

    static PdfPCell cell(String text, Font font, Phrase secondLine, Color bg, boolean right) {
        Phrase phrase = new Phrase(text == null ? "—" : text, font);
        if (secondLine != null) {
            phrase.add("\n");
            phrase.add(secondLine);
        }
        PdfPCell cell = new PdfPCell(phrase);
        cell.setPadding(3.5f);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(new Color(0xdd, 0xe5, 0xdf));
        if (right) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        return cell;
    }

    static String format(Double value) {
        return value == null ? "—" : String.format("%,.1f", value);
    }

    private static String display(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate d) return DAY.format(d);
        if (value instanceof LocalDateTime t) return STAMP.format(t);
        if (value instanceof Double n) return format(n);
        // the built-in PDF fonts have no arrow glyph; an en dash prints everywhere
        return value.toString().replace(" → ", " – ");
    }

    private static void text(Row row, int column, String text, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(text);
        if (style != null) cell.setCellStyle(style);
    }

    // "Page 2" in the bottom-right corner of every page
    static final class PageNumbers extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY)),
                    document.right(), document.bottom() - 16, 0);
        }
    }
}
