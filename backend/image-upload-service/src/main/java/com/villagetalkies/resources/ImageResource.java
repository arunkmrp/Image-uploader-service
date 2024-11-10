package com.villagetalkies.resources;

import java.awt.image.BufferedImage;
import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Iterator;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.villagetalkies.MongoDBUtil;

@Path("/images")
@Produces(MediaType.APPLICATION_JSON)
public class ImageResource {

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImage(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {

        String uploadDir = "uploads/";
        String uploadedFileLocation = uploadDir + fileDetail.getFileName();

        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.copy(uploadedInputStream, Paths.get(uploadedFileLocation), StandardCopyOption.REPLACE_EXISTING);
            return Response.ok("File uploaded successfully! Location: " + uploadedFileLocation).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("File upload failed.").build();
        }
    }

    @GET
    @Path("/convert")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)

    public Response convertImage(
            @QueryParam("filename") String filename,
            @QueryParam("format") String format) {

        String uploadDir = "uploads/";
        String filePath = uploadDir + filename;
        String outputFilePath = uploadDir + "converted_" + removeExtension(filename) + "."
                + format.toLowerCase().trim();

        // // Trim the format parameter to handle trailing newline characters or spaces
        // format = format.trim(); // Add this line

        // Check if file exists
        if (!new File(filePath).exists()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("File not found: " + filename).build();
        }

        try {
            switch (format.toLowerCase(Locale.ROOT)) {
                case "pdf":
                    convertToPDF(filePath, outputFilePath);
                    break;
                case "jpg":
                    convertToJPG(filePath, outputFilePath);
                    break;
                case "png":
                    convertToPNG(filePath, outputFilePath);
                    break;
                case "bmp":
                    convertToBMP(filePath, outputFilePath);
                    break;
                case "gif":
                    convertToGIF(filePath, outputFilePath);
                    break;
                case "svg":
                    convertToSVG(filePath, outputFilePath);
                    break;
                case "tiff":
                    convertToTIFF(filePath, outputFilePath);
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST).entity("Unsupported format").build();
            }
            // Save conversion data to MongoDB
            saveConversionData(filename, format);
            java.nio.file.Path path = Paths.get(outputFilePath);
            return Response.ok(path.toFile())
                    .header("Content-Disposition", "attachment; filename=" + path.getFileName())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Conversion failed").build();
        }
    }

    private void convertToPDF(String inputPath, String outputPath) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IOException("File not found at " + inputPath);
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            BufferedImage image = ImageIO.read(inputFile);
            if (image == null) {
                throw new IOException("Failed to read image file for PDF conversion.");
            }

            // Convert BufferedImage to PDImageXObject
            PDImageXObject pdImage = PDImageXObject.createFromFile(inputPath, document);

            // Calculate scale to fit image within page boundaries while preserving aspect
            // ratio
            float imageWidth = pdImage.getWidth();
            float imageHeight = pdImage.getHeight();
            float pageWidth = page.getMediaBox().getWidth() - 40; // margin of 20 on each side
            float pageHeight = page.getMediaBox().getHeight() - 40; // margin of 20 on each side
            float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);

            // Calculate image position (centered with margins)
            float x = (pageWidth - (imageWidth * scale)) / 2 + 20; // offset by left margin
            float y = (pageHeight - (imageHeight * scale)) / 2 + 20; // offset by bottom margin

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, x, y, imageWidth * scale, imageHeight * scale);
            }

            document.save(outputPath);
        }
    }

    private void convertToJPG(String inputPath, String outputPath) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IOException("File not found at " + inputPath);
        }

        BufferedImage image = ImageIO.read(inputFile);
        if (image == null) {
            throw new IOException("Failed to read image file for JPG conversion.");
        }

        // Check if the image is in a different colorspace (not RGB)
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgbImage = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            rgbImage.getGraphics().drawImage(image, 0, 0, null);
            image = rgbImage;
        }

        // Setting JPG output quality to 0.8 (adjustable)
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
            throw new IOException("No suitable JPEG writer found");

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(new File(outputPath))) {
            writer.setOutput(ios);

            // Configure the JPEG compression quality
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.8f); // Quality between 0 (lowest) and 1 (highest)
            }

            // Write the image to the output file with parameters
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    // Convert to PNG
    private void convertToPNG(String inputPath, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(inputPath));
        if (image == null) {
            throw new IOException("Failed to read image for PNG conversion.");
        }
        ImageIO.write(image, "PNG", new File(outputPath));
    }

    // Convert to BMP
    private void convertToBMP(String inputPath, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(inputPath));
        if (image == null) {
            throw new IOException("Failed to read image for BMP conversion.");
        }
        ImageIO.write(image, "BMP", new File(outputPath));
    }

    // Convert to GIF
    private void convertToGIF(String inputPath, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(inputPath));
        if (image == null) {
            throw new IOException("Failed to read image for GIF conversion.");
        }
        ImageIO.write(image, "GIF", new File(outputPath));
    }

    // Convert to TIFF
    private void convertToTIFF(String inputPath, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(inputPath));
        if (image == null) {
            throw new IOException("Failed to read image for TIFF conversion.");
        }
        ImageIO.write(image, "TIFF", new File(outputPath));
    }

    // Convert to SVG using ImageMagick
    private void convertToSVG(String inputPath, String outputPath) throws IOException {
        // Construct the command to call ImageMagick's magick tool
        String command = "magick convert " + inputPath + " " + outputPath;

        // Execute the command
        Process process = new ProcessBuilder(command.split(" ")).start();
        try {
            // Wait for the command to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("ImageMagick conversion to SVG failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ImageMagick conversion interrupted", e);
        }
    }

    // public void saveImageMetadata(String imageName, String format) {
    // MongoDatabase database = MongoDBUtil.getDatabase();
    // Document doc = new Document("imageName", imageName)
    // .append("format", format);
    // database.getCollection("imageMetadata").insertOne(doc);
    // }

    public void saveConversionData(String fileName, String format) {
        MongoDatabase db = MongoDBUtil.getDatabase(); // Ensure MongoDBUtil is correctly configured
        MongoCollection<Document> collection = db.getCollection("conversions");

        Document doc = new Document("fileName", fileName)
                .append("format", format)
                .append("timestamp", System.currentTimeMillis());

        collection.insertOne(doc);
    }

    // Method to close the connection to the database
    public void closeDBConnection() {
        MongoDBUtil.closeConnection();
    }

    private String removeExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return (index > 0) ? filename.substring(0, index) : filename;
    }

}
