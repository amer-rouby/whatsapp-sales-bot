package com.salesbot.catalog.application;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Component;

/**
 * Turns an uploaded photo into what we store and send: a JPEG (WhatsApp image messages accept JPEG/PNG, not WebP)
 * plus a small thumbnail. The upload is never trusted: it must decode as a real JPEG/PNG of sane size, and it is
 * re-encoded, which also drops any metadata (location, camera) the browser left in.
 */
@Component
class ImageProcessor {

    static final int MAX_UPLOAD_BYTES = 3 * 1024 * 1024;
    static final int MAX_SOURCE_SIDE = 4000;
    static final int FULL_SIDE = 1600;
    static final int THUMB_SIDE = 400;
    private static final Set<String> FORMATS = Set.of("jpeg", "png");

    Processed process(byte[] upload) {
        if (upload == null || upload.length == 0) {
            throw new IllegalArgumentException("The photo is empty");
        }
        if (upload.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("The photo is larger than 3 MB");
        }
        BufferedImage source = decode(upload);
        BufferedImage full = scaleToFit(source, FULL_SIDE);
        BufferedImage thumb = scaleToFit(full, THUMB_SIDE);
        byte[] fullJpeg = jpeg(full, 0.88f);
        return new Processed(fullJpeg, jpeg(thumb, 0.82f), full.getWidth(), full.getHeight());
    }

    /** Reads the size first, so a tiny file claiming a huge image (a "decompression bomb") is refused cheaply. */
    private static BufferedImage decode(byte[] upload) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw new IllegalArgumentException("Only JPEG or PNG photos are accepted");
            }
            ImageReader reader = readers.next();
            try {
                if (!FORMATS.contains(reader.getFormatName().toLowerCase())) {
                    throw new IllegalArgumentException("Only JPEG or PNG photos are accepted");
                }
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_SOURCE_SIDE || height > MAX_SOURCE_SIDE) {
                    throw new IllegalArgumentException("The photo must be at most " + MAX_SOURCE_SIDE + " pixels wide and high");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw new IllegalArgumentException("The photo couldn't be read; try another file", e);
        }
    }

    /** Smooth downscale (halving steps, then one bicubic pass) onto white, so transparent PNGs look right. */
    static BufferedImage scaleToFit(BufferedImage source, int maxSide) {
        double scale = Math.min(1.0, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
        int targetW = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int targetH = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage current = source;
        int w = source.getWidth();
        int h = source.getHeight();
        while (w / 2 >= targetW && h / 2 >= targetH) {
            w /= 2;
            h /= 2;
            current = draw(current, w, h, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        return draw(current, targetW, targetH, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static BufferedImage draw(BufferedImage source, int width, int height, Object interpolation) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] jpeg(BufferedImage image, float quality) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode the photo", e);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    record Processed(byte[] full, byte[] thumb, int width, int height) {
    }
}
