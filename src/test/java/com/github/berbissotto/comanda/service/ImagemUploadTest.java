package com.github.berbissotto.comanda.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagemUploadTest {

    private static byte[] comPadding(byte[] inicio) {
        byte[] b = new byte[32];
        System.arraycopy(inicio, 0, b, 0, inicio.length);
        return b;
    }

    @Test
    void reconheceFormatosPermitidos() {
        assertEquals("image/png", ImagemUpload.detectarTipo(comPadding(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).orElseThrow());
        assertEquals("image/jpeg", ImagemUpload.detectarTipo(comPadding(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0})).orElseThrow());
        assertEquals("image/gif", ImagemUpload.detectarTipo(comPadding("GIF89a".getBytes(StandardCharsets.US_ASCII))).orElseThrow());
        byte[] webp = comPadding("RIFF".getBytes(StandardCharsets.US_ASCII));
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, webp, 8, 4);
        assertEquals("image/webp", ImagemUpload.detectarTipo(webp).orElseThrow());
    }

    @Test
    void recusaSvgHtmlEVazio() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>".getBytes(StandardCharsets.UTF_8);
        assertTrue(ImagemUpload.detectarTipo(svg).isEmpty());
        assertTrue(ImagemUpload.detectarTipo("<html><script>1</script></html>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(ImagemUpload.detectarTipo(new byte[0]).isEmpty());
        assertTrue(ImagemUpload.detectarTipo(null).isEmpty());
    }
}
