package by.bsuir.productservice.service.impl;

import by.bsuir.productservice.DTO.DispatchDTO;
import by.bsuir.productservice.DTO.EmployeeDto;
import by.bsuir.productservice.DTO.ProductDTO;
import by.bsuir.productservice.DTO.WarehouseDTO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {
    private static final String FONT_PATH = "/fonts/arial.ttf";
    private static final BaseFont BASE_FONT;
    private static final Font TITLE_FONT;
    private static final Font HEADER_FONT;
    private static final Font NORMAL_FONT;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 100;

    static {
        try {
            BASE_FONT = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            TITLE_FONT = new Font(BASE_FONT, 16, Font.BOLD);
            HEADER_FONT = new Font(BASE_FONT, 12, Font.BOLD);
            NORMAL_FONT = new Font(BASE_FONT, 12, Font.NORMAL);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load font for PDF", e);
        }
    }

    public byte[] generateReceiptOrderPDF(
            List<ProductDTO> products,
            List<Integer> productIds,
            Map<Integer, String> idToEan,
            String fullName) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Paragraph title = new Paragraph("Приходной ордер", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph info = new Paragraph(
                String.format("Дата: %s   Принял: %s",
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        fullName), NORMAL_FONT);
        info.setSpacingBefore(12);
        info.setSpacingAfter(12);
        document.add(info);

        PdfPTable table = new PdfPTable(new float[]{4, 2, 4});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        table.addCell(createCell("Наименование", HEADER_FONT, Element.ALIGN_LEFT));
        table.addCell(createCell("Количество", HEADER_FONT, Element.ALIGN_CENTER));
        table.addCell(createCell("Штрихкод (EAN)", HEADER_FONT, Element.ALIGN_CENTER));

        for (int i = 0; i < products.size(); i++) {
            ProductDTO dto = products.get(i);
            Integer id = productIds.get(i);
            String ean = idToEan.get(id);

            table.addCell(createCell(dto.getName(), NORMAL_FONT, Element.ALIGN_LEFT));
            table.addCell(createCell(String.valueOf(dto.getAmount()), NORMAL_FONT, Element.ALIGN_CENTER));
            table.addCell(createCell(ean, NORMAL_FONT, Element.ALIGN_CENTER));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.BOX);
        return cell;
    }

    public byte[] generateBarcodePDF(String ean) throws Exception {

        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(ean, BarcodeFormat.CODE_128, WIDTH, HEIGHT);

        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        int textHeight = 20;
        BufferedImage combinedImage = new BufferedImage(
                barcodeImage.getWidth(),
                barcodeImage.getHeight() + textHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = combinedImage.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, combinedImage.getWidth(), combinedImage.getHeight());

        g2d.drawImage(barcodeImage, 0, 0, null);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(ean);
        int x = (barcodeImage.getWidth() - textWidth) / 2;
        int y = barcodeImage.getHeight() + (textHeight + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(ean, x, y);

        g2d.dispose();

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
        ImageIO.write(combinedImage, "png", imgBaos);
        Image img = Image.getInstance(imgBaos.toByteArray());
        img.setAlignment(Image.ALIGN_CENTER);
        document.add(img);

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateDispatchOrderPDF(
            List<ProductDTO> products,
            List<Integer> productIds,
            String workerFullName) throws Exception {

        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Paragraph title = new Paragraph("Отпускной ордер", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph info = new Paragraph(
                String.format("Дата: %s    Отпустил: %s",
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        workerFullName),
                NORMAL_FONT);
        info.setSpacingBefore(12);
        info.setSpacingAfter(12);
        doc.add(info);

        PdfPTable table = new PdfPTable(new float[]{4, 2, 4});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setSpacingAfter(8);

        table.addCell(createCell("Наименование", HEADER_FONT, Element.ALIGN_LEFT));
        table.addCell(createCell("Кол-во", HEADER_FONT, Element.ALIGN_CENTER));
        table.addCell(createCell("Ед. изм.", HEADER_FONT, Element.ALIGN_CENTER));

        for (ProductDTO dto : products) {
            table.addCell(createCell(dto.getName(), NORMAL_FONT, Element.ALIGN_LEFT));
            table.addCell(createCell(String.valueOf(dto.getAmount()), NORMAL_FONT, Element.ALIGN_CENTER));
            table.addCell(createCell(dto.getUnit(), NORMAL_FONT, Element.ALIGN_CENTER));
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    public byte[] generateTTN(
            DispatchDTO dto,
            List<ProductDTO> products,
            EmployeeDto driver,
            WarehouseDTO warehouse) throws Exception {

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        PdfPTable header = new PdfPTable(new float[]{1, 2, 1, 2});
        header.setWidthPercentage(100);
        header.addCell(createCell("Форма № ТТН-1", HEADER_FONT, Element.ALIGN_LEFT));
        header.addCell(createCell("Товарно-транспортная накладная", TITLE_FONT, Element.ALIGN_CENTER));
        header.addCell(createCell("Код формы по ОКУД", HEADER_FONT, Element.ALIGN_LEFT));
        header.addCell(createCell("0310001", NORMAL_FONT, Element.ALIGN_CENTER));
        doc.add(header);

        PdfPTable parties = new PdfPTable(new float[]{1,1});
        parties.setSpacingBefore(12);
        parties.setWidthPercentage(100);
        //TODO
        parties.addCell(createCell(
                "Грузоотправитель:\n" +
                        warehouse.getName() + "\nУНП: " + "12314" +
                        "\nАдрес: " + warehouse.getAddress(),
                NORMAL_FONT, Element.ALIGN_LEFT));
        parties.addCell(createCell(
                "Грузополучатель:\n" +
                        dto.getCustomerName() + "\nУНП: " + dto.getCustomerUnp() +
                        "\nАдрес: " + dto.getCustomerAddress(),
                NORMAL_FONT, Element.ALIGN_LEFT));
        doc.add(parties);

        PdfPTable table = new PdfPTable(new float[]{1,3,1,1,1,1,1});
        table.setSpacingBefore(12);
        table.setWidthPercentage(100);
        String[] cols = {
                "№", "Наименование товара", "Ед. изм.", "Кол-во",
                "Цена (без НДС)", "Сумма (без НДС)", "Ставка НДС, %"
        };
        for (String c : cols) {
            table.addCell(createCell(c, HEADER_FONT, Element.ALIGN_CENTER));
        }
        for (int i = 0; i < products.size(); i++) {
            ProductDTO p = products.get(i);
            double sum = p.getPrice() * p.getAmount();
            table.addCell(createCell(String.valueOf(i+1), NORMAL_FONT, Element.ALIGN_CENTER));
            table.addCell(createCell(p.getName(), NORMAL_FONT, Element.ALIGN_LEFT));
            table.addCell(createCell(p.getUnit(), NORMAL_FONT, Element.ALIGN_CENTER));
            table.addCell(createCell(String.valueOf(p.getAmount()), NORMAL_FONT, Element.ALIGN_CENTER));
            table.addCell(createCell(String.format("%.2f", p.getPrice()), NORMAL_FONT, Element.ALIGN_RIGHT));
            table.addCell(createCell(String.format("%.2f", sum), NORMAL_FONT, Element.ALIGN_RIGHT));
            table.addCell(createCell("20", NORMAL_FONT, Element.ALIGN_CENTER));
        }
        doc.add(table);

        double total = products.stream()
                .mapToDouble(p -> p.getPrice() * p.getAmount())
                .sum();
        Paragraph totalPara = new Paragraph(
                "Всего без НДС: " + String.format("%.2f руб.", total),
                NORMAL_FONT);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        totalPara.setSpacingBefore(8);
        doc.add(totalPara);

        PdfPTable sig = new PdfPTable(new float[]{1,1,1});
        sig.setWidthPercentage(100);
        sig.setSpacingBefore(24);
        sig.addCell(createCell("Грузоотправитель __________", NORMAL_FONT, Element.ALIGN_LEFT));
        sig.addCell(createCell("Экспедитор __________", NORMAL_FONT, Element.ALIGN_LEFT));
        sig.addCell(createCell("Грузополучатель __________", NORMAL_FONT, Element.ALIGN_LEFT));
        doc.add(sig);

        doc.close();
        return out.toByteArray();
    }
    public byte[] generateTN(
            DispatchDTO dto,
            List<ProductDTO> products,
            EmployeeDto driver,
            WarehouseDTO warehouse) throws Exception {

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Paragraph header = new Paragraph("Товарная накладная (ТН-2)", TITLE_FONT);
        header.setAlignment(Element.ALIGN_CENTER);
        doc.add(header);

        PdfPTable parts = new PdfPTable(new float[]{1, 1});
        parts.setWidthPercentage(100);
        parts.setSpacingBefore(10);
        parts.addCell(createCell(
                "Продавец:\n" + warehouse.getName() +
                        "\nУНП: " + "23123312" +
                        "\nАдрес: " + warehouse.getAddress(),
                NORMAL_FONT, Element.ALIGN_LEFT));
        parts.addCell(createCell(
                "Покупатель:\n" + dto.getCustomerName() +
                        "\nУНП: " + dto.getCustomerUnp() +
                        "\nАдрес: " + dto.getCustomerAddress(),
                NORMAL_FONT, Element.ALIGN_LEFT));
        doc.add(parts);

        PdfPTable tbl = new PdfPTable(new float[]{1, 3, 1, 1, 1, 1});
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(12);

        tbl.addCell(createCell("№", HEADER_FONT, Element.ALIGN_CENTER));
        tbl.addCell(createCell("Наименование", HEADER_FONT, Element.ALIGN_CENTER));
        tbl.addCell(createCell("Ед. изм.", HEADER_FONT, Element.ALIGN_CENTER));
        tbl.addCell(createCell("Кол-во", HEADER_FONT, Element.ALIGN_CENTER));
        tbl.addCell(createCell("Цена", HEADER_FONT, Element.ALIGN_CENTER));
        tbl.addCell(createCell("Сумма", HEADER_FONT, Element.ALIGN_CENTER));

        double grandTotal = 0;
        for (int i = 0; i < products.size(); i++) {
            ProductDTO p = products.get(i);
            double price = p.getPrice() != 0 ? p.getPrice() : 0;
            int qty    = p.getAmount();
            double sum = price * qty;
            grandTotal += sum;

            tbl.addCell(createCell(String.valueOf(i + 1), NORMAL_FONT, Element.ALIGN_CENTER));
            tbl.addCell(createCell(p.getName(), NORMAL_FONT, Element.ALIGN_LEFT));
            tbl.addCell(createCell(p.getUnit(), NORMAL_FONT, Element.ALIGN_CENTER));
            tbl.addCell(createCell(String.valueOf(qty), NORMAL_FONT, Element.ALIGN_CENTER));
            tbl.addCell(createCell(String.format("%.2f", price), NORMAL_FONT, Element.ALIGN_RIGHT));
            tbl.addCell(createCell(String.format("%.2f", sum), NORMAL_FONT, Element.ALIGN_RIGHT));
        }
        doc.add(tbl);

        Paragraph totalPara = new Paragraph(
                "Итого по накладной: " + String.format("%.2f", grandTotal) + " руб.",
                NORMAL_FONT);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        totalPara.setSpacingBefore(16);
        doc.add(totalPara);

        Paragraph sign = new Paragraph(
                "Подпись: ______________________    ФИО: " +
                        driver.getFirstName() + " " + driver.getSecondName(),
                NORMAL_FONT);
        sign.setSpacingBefore(24);
        doc.add(sign);

        doc.close();
        return out.toByteArray();
    }

    public byte[] generateInventoryReport(String username,
                                          WarehouseDTO warehouse,
                                          List<ProductDTO> products,
                                          List<Integer> expected,
                                          List<Integer> actual) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        BaseFont bf = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font headerFont = new Font(bf, 12, Font.BOLD);
        Font regularFont = new Font(bf, 10);

        document.add(new Paragraph("ИНВЕНТАРИЗАЦИОННАЯ ОПИСЬ", headerFont));
                document.add(new Paragraph("Работник: " + username, regularFont));
                        document.add(new Paragraph("Склад: " + warehouse.getName(), regularFont));
                                document.add(new Paragraph("Дата: " + LocalDate.now(), regularFont));
                                        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.addCell(new PdfPCell(new Phrase("ID", headerFont)));
                table.addCell(new PdfPCell(new Phrase("Наименование", headerFont)));
                        table.addCell(new PdfPCell(new Phrase("Ожидалось", headerFont)));
                                table.addCell(new PdfPCell(new Phrase("Фактически", headerFont)));
                                        table.addCell(new PdfPCell(new Phrase("Разница", headerFont)));

        for (int i = 0; i < products.size(); i++) {
            ProductDTO p = products.get(i);
            int exp = expected.get(i);
            int act = actual.get(i);
            int diff = act - exp;
            table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getId()), regularFont)));
            table.addCell(new PdfPCell(new Phrase(p.getName(), regularFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(exp), regularFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(act), regularFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(diff), regularFont)));
        }

        document.add(table);
        document.close();
        return baos.toByteArray();
    }

    public byte[] generateRevaluationReport(List<ProductDTO> products,
                                            List<Double> oldPrices,
                                            List<Double> newPrices,
                                            List<Integer> quantities) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        BaseFont bf = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font headerFont = new Font(bf, 14, Font.BOLD);
        Font regularFont = new Font(bf, 10);

        document.add(new Paragraph("АКТ ПЕРЕОЦЕНКИ", headerFont));
        document.add(new Paragraph("Дата: " + LocalDate.now(), regularFont));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        String[] headers = {"Наименование товара", "Ед.", "Кол-во", "Старая цена", "Новая цена", "Разница"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        double totalDiff = 0;
        for (int i = 0; i < products.size(); i++) {
            ProductDTO p = products.get(i);
            int qty = quantities.get(i);
            double oldP = oldPrices.get(i);
            double newP = newPrices.get(i);
            double diff = (newP - oldP) * qty;
            totalDiff += diff;

            table.addCell(new PdfPCell(new Phrase(p.getName(), regularFont)));
            table.addCell(new PdfPCell(new Phrase(p.getUnit(), regularFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(qty), regularFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", oldP), regularFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", newP), regularFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", diff), regularFont)));
        }
        document.add(table);
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Итоговая разница: " + String.format("%.2f", totalDiff), regularFont));
        document.close();
        return baos.toByteArray();
    }

    public byte[] generateWriteOffAct(LocalDate orderDate,
                                      String chairman,
                                      List<ProductDTO> products,
                                      String reason) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        BaseFont bf = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font headerFont = new Font(bf, 14, Font.BOLD);
        Font regularFont = new Font(bf, 10);

        Paragraph title = new Paragraph(
                "АКТ НА СПИСАНИЕ СТРОИТЕЛЬНОГО ИНСТРУМЕНТА, ХОЗЯЙСТВЕННОГО ИНВЕНТАРЯ",
                headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("Приказ от: " + orderDate, regularFont));
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Председатель комиссии: " + chairman, regularFont));
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Причина списания: " + reason, regularFont));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        String[] headers = {
                "Инструмент, инвентарь",
                "Единица измерения",
                "Количество",
                "Цена, руб.",
                "Сумма, руб.",
                "Годен до",
                "Примечание"
        };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        int totalQty = 0;
        double totalSum = 0;
        double totalAmount = 0;
        for (ProductDTO p : products) {
            int qty = p.getAmount();
            double price = p.getPrice();
            double itemTotal = qty * price;

            totalQty += qty;
            totalAmount += itemTotal;
            totalSum += price;

            table.addCell(new PdfPCell(new Paragraph(p.getName(), regularFont)));
            table.addCell(new PdfPCell(new Paragraph(p.getUnit(), regularFont)));
            table.addCell(new PdfPCell(new Paragraph(String.valueOf(qty), regularFont)));
            table.addCell(new PdfPCell(new Paragraph(String.format("%.2f", price), regularFont)));
            table.addCell(new PdfPCell(new Paragraph(String.format("%.2f", itemTotal), regularFont)));
            table.addCell(new PdfPCell(new Paragraph(
                    p.getBestBeforeDate() != null ? p.getBestBeforeDate().toString() : "-",
                    regularFont)));

            table.addCell(new PdfPCell(new Paragraph(reason, regularFont)));
        }

        PdfPCell sumCell = new PdfPCell(new Paragraph("ВСЕГО ПОДЛЕЖИТ СПИСАНИЮ", regularFont));
        sumCell.setColspan(2);
        table.addCell(sumCell);
        table.addCell(new PdfPCell(new Paragraph(String.valueOf(totalQty), regularFont)));
        table.addCell(new PdfPCell(new Paragraph(String.format("%.2f", totalSum), regularFont)));
        table.addCell(new PdfPCell(new Paragraph(String.format("%.2f", totalAmount), regularFont)));
        table.addCell(new PdfPCell(new Paragraph("-", regularFont)));
        table.addCell(new PdfPCell(new Paragraph("-", regularFont)));

        document.add(table);
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Акт составлен: " + LocalDate.now(), regularFont));
        document.close();

        return outputStream.toByteArray();
    }

}