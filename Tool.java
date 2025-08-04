import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.codec.binary.Base85;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.aztec.AztecWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class Tool {
    // Error correction level (0-100 for Aztec/QR, 0-8 for PDF417)
    private static final int DEFAULT_ERROR_CORRECTION = 50;

    /**
     * Step 1: Create TAR archive from path
     * @param path File/directory path
     * @return TAR as byte array
     */
    public byte[] createTar(String path) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(byteOut)) {
            
            Path sourcePath = Paths.get(path);
            if (Files.isDirectory(sourcePath)) {
                Files.walk(sourcePath)
                     .filter(p -> !Files.isDirectory(p))
                     .forEach(p -> addToTar(p, sourcePath, tarOut));
            } else {
                addToTar(sourcePath, sourcePath.getParent(), tarOut);
            }
            
            tarOut.finish();
            return byteOut.toByteArray();
        }
    }

    private void addToTar(Path file, Path basePath, TarArchiveOutputStream tarOut) {
        try {
            String entryName = basePath.relativize(file).toString();
            TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), entryName);
            tarOut.putArchiveEntry(entry);
            
            try (InputStream input = Files.newInputStream(file)) {
                IOUtils.copy(input, tarOut);
            }
            
            tarOut.closeArchiveEntry();
        } catch (IOException e) {
            throw new RuntimeException("TAR creation failed: " + file, e);
        }
    }

    /**
     * Step 2: Compress with LZMA
     * @param input Byte array to compress
     * @return LZMA compressed data
     */
    public byte[] compressWithLzma(byte[] input) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             LZMACompressorOutputStream lzmaOut = new LZMACompressorOutputStream(byteOut)) {
            lzmaOut.write(input);
            return byteOut.toByteArray();
        }
    }

    /**
     * Step 3: Encode to Base85
     * @param input Byte array to encode
     * @return Base85-encoded string
     */
    public String encodeBase85(byte[] input) {
        return new String(Base85.encodeBase85(input));
    }

    /**
     * Step 4: Generate and display barcodes
     * @param data Base85-encoded data
     * @param barcodeType Aztec (default), PDF417, QR
     * @param chunkSize Max characters per barcode
     * @param errorCorrection 0-100 for Aztec/QR, 0-8 for PDF417
     */
    public void generateAndDisplayBarcodes(String data, String barcodeType, int chunkSize, int errorCorrection) {
        if (errorCorrection < 0) errorCorrection = DEFAULT_ERROR_CORRECTION;
        
        // Split data into chunks with metadata headers
        List<String> chunks = createChunks(data, chunkSize);
        
        // Generate and display barcodes
        List<JFrame> frames = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            BufferedImage barcode = generateBarcode(
                chunks.get(i), 
                barcodeType, 
                errorCorrection
            );
            frames.add(displayBarcode(barcode, i+1, chunks.size()));
        }
    }

    private List<String> createChunks(String data, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int total = (int) Math.ceil((double) data.length() / chunkSize);
        
        for (int i = 0; i < total; i++) {
            int start = i * chunkSize;
            int end = Math.min((i + 1) * chunkSize, data.length());
            String chunk = i + ":" + total + ":" + data.substring(start, end);
            chunks.add(chunk);
        }
        return chunks;
    }

    private BufferedImage generateBarcode(String data, String type, int errorCorrection) {
        BarcodeFormat format = BarcodeFormat.AZTEC;
        com.google.zxing.Writer writer = new AztecWriter();
        
        if ("PDF417".equalsIgnoreCase(type)) {
            format = BarcodeFormat.PDF_417;
            writer = new PDF417Writer();
        } else if ("QR".equalsIgnoreCase(type)) {
            format = BarcodeFormat.QR_CODE;
            writer = new QRCodeWriter();
        }
        
        try {
            // Error correction configuration
            Map<com.google.zxing.EncodeHintType, Object> hints = new EnumMap<>(com.google.zxing.EncodeHintType.class);
            hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, errorCorrection);
            
            BitMatrix matrix = writer.encode(data, format, 0, 0, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception e) {
            throw new RuntimeException("Barcode generation failed", e);
        }
    }

    private JFrame displayBarcode(BufferedImage image, int index, int total) {
        // Scale for better scanning
        Image scaled = image.getScaledInstance(600, 600, Image.SCALE_SMOOTH);
        BufferedImage bufferedScaled = new BufferedImage(
            600, 600, BufferedImage.TYPE_INT_RGB
        );
        bufferedScaled.getGraphics().drawImage(scaled, 0, 0, null);
        
        JFrame frame = new JFrame("Barcode " + index + "/" + total);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(new JLabel(new ImageIcon(bufferedScaled)));
        frame.pack();
        frame.setVisible(true);
        
        // Secure wipe on close
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                wipeImage(bufferedScaled);
                wipeImage(image);
                System.gc();
            }
        });
        
        return frame;
    }

    private void wipeImage(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        DataExfiltrationTool tool = new DataExfiltrationTool();
        
        // Step 1: Create TAR
        byte[] tarData = tool.createTar("/path/to/critical_code");
        
        // Step 2: LZMA compression
        byte[] compressed = tool.compressWithLzma(tarData);
        
        // Step 3: Base85 encoding
        String base85 = tool.encodeBase85(compressed);
        
        // Step 4: Generate barcodes (Aztec, 1000 chars/chunk, 50% error correction)
        tool.generateAndDisplayBarcodes(base85, "AZTEC", 1000, 50);
    }
}